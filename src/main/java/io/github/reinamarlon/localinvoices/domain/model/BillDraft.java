package io.github.reinamarlon.localinvoices.domain.model;

import java.util.List;

public record BillDraft(
    long companyId,
    String cityDate,
    String customerDocumentType,
    String receivedFrom,
    String customerId,
    String customerPhone,
    String customerEmail,
    String customerAddress,
    List<InvoiceItem> items,
    String paymentMethod,
    String notes
) {
    public int total() {
        return items.stream().mapToInt(InvoiceItem::lineTotal).sum();
    }
}
