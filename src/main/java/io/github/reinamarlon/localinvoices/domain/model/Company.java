package io.github.reinamarlon.localinvoices.domain.model;

public record Company(
    long id,
    String commercialName,
    String legalName,
    String taxId,
    String city,
    String country,
    String phone,
    String logoPath
) {
    public String label() {
        return commercialName == null || commercialName.isBlank() ? legalName : commercialName;
    }
}
