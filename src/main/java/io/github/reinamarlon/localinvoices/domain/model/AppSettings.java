package io.github.reinamarlon.localinvoices.domain.model;

public record AppSettings(String theme, String accent, String fontSize, String density) {
    public static AppSettings defaults() {
        return new AppSettings("Claro", "Azul", "Normal", "Comoda");
    }
}
