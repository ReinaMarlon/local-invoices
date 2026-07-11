package io.github.reinamarlon.localinvoices.domain.model;

import java.util.List;

public record Bill(
    long id,
    long companyId,
    String companyName,
    String companyTaxId,
    String companyLogoPath,
    int consecutive,
    String cityDate,
    String customerDocumentType,
    String receivedFrom,
    String customerId,
    String customerPhone,
    String customerEmail,
    String customerAddress,
    int amount,
    String paymentMethod,
    String notes,
    List<InvoiceItem> items
) {
}
