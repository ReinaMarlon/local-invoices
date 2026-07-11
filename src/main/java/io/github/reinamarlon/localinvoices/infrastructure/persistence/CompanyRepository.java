package io.github.reinamarlon.localinvoices.infrastructure.persistence;

import io.github.reinamarlon.localinvoices.domain.model.Company;
import io.github.reinamarlon.localinvoices.domain.model.CompanyDraft;
import io.github.reinamarlon.localinvoices.infrastructure.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class CompanyRepository {
    private final Connection connection;

    public CompanyRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Company> findAll() {
        List<Company> companies = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM companies ORDER BY commercial_name")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    companies.add(map(result));
                }
            }
            return companies;
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron listar las empresas", ex);
        }
    }

    public Company create(CompanyDraft draft) {
        try {
            connection.setAutoCommit(false);
            String logoPath = copyLogo(draft.sourceLogoPath());
            long id;
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO companies (commercial_name, legal_name, tax_id, city, country, phone, logo_path)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, draft.commercialName().trim());
                statement.setString(2, draft.legalName().trim());
                statement.setString(3, draft.taxId().trim());
                statement.setString(4, draft.city().trim());
                statement.setString(5, draft.country().trim());
                statement.setString(6, draft.phone().trim());
                statement.setString(7, logoPath);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se genero ID para la empresa");
                    id = keys.getLong(1);
                }
            }
            try (PreparedStatement counter = connection.prepareStatement("INSERT INTO company_counters (company_id, current_value) VALUES (?, 0)")) {
                counter.setLong(1, id);
                counter.executeUpdate();
            }
            connection.commit();
            return new Company(id, draft.commercialName(), draft.legalName(), draft.taxId(), draft.city(), draft.country(), draft.phone(), logoPath);
        } catch (Exception ex) {
            rollbackQuietly();
            throw new IllegalStateException("No se pudo registrar la empresa", ex);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void delete(Company company) {
        String logoPath = company.logoPath();
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM companies WHERE id = ?")) {
            statement.setLong(1, company.id());
            if (statement.executeUpdate() != 1) throw new SQLException("La empresa ya no existe.");
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo eliminar la empresa", ex);
        }
        if (logoPath != null && !logoPath.isBlank()) {
            try {
                Files.deleteIfExists(Path.of(logoPath));
            } catch (IOException ignored) {
                // The database deletion remains valid even if an old logo cannot be removed.
            }
        }
    }

    private String copyLogo(String sourceLogoPath) throws IOException {
        if (sourceLogoPath == null || sourceLogoPath.isBlank()) {
            return "";
        }
        Path source = Path.of(sourceLogoPath);
        String fileName = System.currentTimeMillis() + "-" + source.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        Path target = AppPaths.logosDirectory().resolve(fileName);
        Files.copy(source, target);
        return target.toAbsolutePath().toString();
    }

    private Company map(ResultSet result) throws SQLException {
        return new Company(
            result.getLong("id"),
            result.getString("commercial_name"),
            result.getString("legal_name"),
            result.getString("tax_id"),
            result.getString("city"),
            result.getString("country"),
            result.getString("phone"),
            result.getString("logo_path")
        );
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
