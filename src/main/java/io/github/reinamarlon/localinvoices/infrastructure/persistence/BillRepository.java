package io.github.reinamarlon.localinvoices.infrastructure.persistence;

import io.github.reinamarlon.localinvoices.domain.model.Bill;
import io.github.reinamarlon.localinvoices.domain.model.BillDraft;
import io.github.reinamarlon.localinvoices.domain.model.Company;
import io.github.reinamarlon.localinvoices.domain.model.InvoiceItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class BillRepository {
    private final Connection connection;

    public BillRepository(Connection connection) {
        this.connection = connection;
    }

    public int currentConsecutive(Company company) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT current_value FROM company_counters WHERE company_id = ?")) {
            statement.setLong(1, company.id());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("current_value") : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el consecutivo", ex);
        }
    }

    public int save(BillDraft draft) {
        try {
            connection.setAutoCommit(false);
            int next = currentConsecutive(draft.companyId()) + 1;
            long billId;
            try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO bills (
                    company_id, consecutive, city_date, received_from, customer_id, customer_phone,
                    customer_document_type, customer_email, customer_address, amount, concept, payment_method, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                insert.setLong(1, draft.companyId());
                insert.setInt(2, next);
                insert.setString(3, draft.cityDate());
                insert.setString(4, draft.receivedFrom());
                insert.setString(5, draft.customerId());
                insert.setString(6, draft.customerPhone());
                insert.setString(7, draft.customerDocumentType());
                insert.setString(8, draft.customerEmail());
                insert.setString(9, draft.customerAddress());
                insert.setInt(10, draft.total());
                insert.setString(11, draft.items().get(0).description());
                insert.setString(12, draft.paymentMethod());
                insert.setString(13, draft.notes());
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se genero ID para el recibo");
                    billId = keys.getLong(1);
                }
            }
            saveItems(billId, draft.items());
            try (PreparedStatement update = connection.prepareStatement("UPDATE company_counters SET current_value = ? WHERE company_id = ?")) {
                update.setInt(1, next);
                update.setLong(2, draft.companyId());
                update.executeUpdate();
            }
            connection.commit();
            return next;
        } catch (SQLException ex) {
            rollbackQuietly();
            throw new IllegalStateException("No se pudo guardar el recibo", ex);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public List<Bill> list(Company company, String filterType, String filterValue) {
        String value = filterValue == null ? "" : filterValue.trim().toLowerCase();
        List<Bill> bills = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT b.*, c.commercial_name, c.tax_id, c.logo_path
            FROM bills b
            JOIN companies c ON c.id = b.company_id
            WHERE b.company_id = ?
            """);
        if (!value.isBlank()) {
            String column = switch (filterType == null ? "" : filterType) {
                case "nombre" -> "b.received_from";
                case "telefono" -> "b.customer_phone";
                default -> "b.customer_id";
            };
            sql.append(" AND LOWER(").append(column).append(") LIKE ?");
        }
        sql.append(" ORDER BY b.id DESC");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, company.id());
            if (!value.isBlank()) statement.setString(2, "%" + value + "%");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) bills.add(map(result));
            }
            return bills;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo listar el historial", ex);
        }
    }

    private void saveItems(long billId, List<InvoiceItem> items) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO bill_items (bill_id, position, description, quantity, unit_price)
            VALUES (?, ?, ?, ?, ?)
            """)) {
            int position = 1;
            for (InvoiceItem item : items) {
                statement.setLong(1, billId);
                statement.setInt(2, position++);
                statement.setString(3, item.description());
                statement.setInt(4, item.quantity());
                statement.setInt(5, item.unitPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<InvoiceItem> items(long billId, String fallbackDescription, int fallbackAmount) throws SQLException {
        List<InvoiceItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT description, quantity, unit_price FROM bill_items WHERE bill_id = ? ORDER BY position
            """)) {
            statement.setLong(1, billId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) items.add(new InvoiceItem(result.getString(1), result.getInt(2), result.getInt(3)));
            }
        }
        if (items.isEmpty()) items.add(new InvoiceItem(fallbackDescription == null ? "Item" : fallbackDescription, 1, fallbackAmount));
        return List.copyOf(items);
    }

    private Bill map(ResultSet result) throws SQLException {
        long billId = result.getLong("id");
        int amount = result.getInt("amount");
        return new Bill(
            billId,
            result.getLong("company_id"),
            result.getString("commercial_name"),
            result.getString("tax_id"),
            result.getString("logo_path"),
            result.getInt("consecutive"),
            result.getString("city_date"),
            result.getString("customer_document_type"),
            result.getString("received_from"),
            result.getString("customer_id"),
            result.getString("customer_phone"),
            result.getString("customer_email"),
            result.getString("customer_address"),
            amount,
            result.getString("payment_method"),
            result.getString("notes"),
            items(billId, result.getString("concept"), amount)
        );
    }

    private int currentConsecutive(long companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT current_value FROM company_counters WHERE company_id = ?")) {
            statement.setLong(1, companyId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("current_value") : 0;
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
