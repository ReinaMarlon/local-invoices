package io.github.reinamarlon.localinvoices.infrastructure;

import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() {
    }

    public static Path dataDirectory() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, "Local Invoices");
        }
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "Local Invoices");
        }
        return Path.of(home, ".local", "share", "local-invoices");
    }

    public static Path logosDirectory() {
        return dataDirectory().resolve("logos");
    }

    public static Path databasePath() {
        return dataDirectory().resolve("local-invoices.db");
    }
}
