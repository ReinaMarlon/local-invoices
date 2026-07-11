package io.github.reinamarlon.localinvoices.infrastructure.persistence;

import io.github.reinamarlon.localinvoices.infrastructure.AppPaths;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager implements AutoCloseable {
    private final Connection connection;

    public DatabaseManager() {
        try {
            Files.createDirectories(AppPaths.dataDirectory());
            Files.createDirectories(AppPaths.logosDirectory());
            connection = DriverManager.getConnection("jdbc:sqlite:" + AppPaths.databasePath());
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initializeSchema();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar la base de datos", ex);
        }
    }

    public Connection connection() {
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS companies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    commercial_name TEXT NOT NULL,
                    legal_name TEXT NOT NULL,
                    tax_id TEXT NOT NULL,
                    city TEXT NOT NULL,
                    country TEXT NOT NULL,
                    phone TEXT,
                    logo_path TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS company_counters (
                    company_id INTEGER PRIMARY KEY,
                    current_value INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
                )
                """);
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    company_id INTEGER NOT NULL,
                    consecutive INTEGER NOT NULL,
                    city_date TEXT,
                    received_from TEXT,
                    customer_id TEXT,
                    customer_phone TEXT,
                    amount INTEGER,
                    concept TEXT,
                    payment_method TEXT,
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
                )
                """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bills_company ON bills(company_id)");
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_bills_company_consecutive ON bills(company_id, consecutive)");
            addColumnIfMissing(statement, "ALTER TABLE bills ADD COLUMN customer_document_type TEXT");
            addColumnIfMissing(statement, "ALTER TABLE bills ADD COLUMN customer_email TEXT");
            addColumnIfMissing(statement, "ALTER TABLE bills ADD COLUMN customer_address TEXT");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bill_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_id INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price INTEGER NOT NULL,
                    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
                )
                """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bill_items_bill ON bill_items(bill_id)");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL
                )
                """);
        }
    }

    private void addColumnIfMissing(Statement statement, String sql) throws SQLException {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            String message = ex.getMessage();
            if (message == null || !message.toLowerCase().contains("duplicate column")) throw ex;
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
