package io.github.reinamarlon.localinvoices.domain.model;

public record InvoiceItem(String description, int quantity, int unitPrice) {
    public int lineTotal() {
        return Math.multiplyExact(quantity, unitPrice);
    }
}
