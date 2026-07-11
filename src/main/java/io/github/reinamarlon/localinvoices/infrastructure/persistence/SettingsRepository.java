package io.github.reinamarlon.localinvoices.infrastructure.persistence;

import io.github.reinamarlon.localinvoices.domain.model.AppSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SettingsRepository {
    private final Connection connection;

    public SettingsRepository(Connection connection) {
        this.connection = connection;
    }

    public AppSettings load() {
        return new AppSettings(value("theme", "Claro"), value("accent", "Azul"), value("fontSize", "Normal"), value("density", "Comoda"));
    }

    public void save(AppSettings settings) {
        saveValue("theme", settings.theme());
        saveValue("accent", settings.accent());
        saveValue("fontSize", settings.fontSize());
        saveValue("density", settings.density());
    }

    private String value(String key, String fallback) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key = ?")) {
            statement.setString(1, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : fallback;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron leer los ajustes", ex);
        }
    }

    private void saveValue(String key, String value) {
        try (PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)
            ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
            """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron guardar los ajustes", ex);
        }
    }
}
