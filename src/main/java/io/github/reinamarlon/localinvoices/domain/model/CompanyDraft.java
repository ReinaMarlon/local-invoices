package io.github.reinamarlon.localinvoices.domain.model;

public record CompanyDraft(
    String commercialName,
    String legalName,
    String taxId,
    String city,
    String country,
    String phone,
    String sourceLogoPath
) {
}
