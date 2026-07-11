package io.github.reinamarlon.localinvoices.domain.service;

import io.github.reinamarlon.localinvoices.domain.model.Bill;
import io.github.reinamarlon.localinvoices.domain.model.BillDraft;
import io.github.reinamarlon.localinvoices.domain.model.AppSettings;
import io.github.reinamarlon.localinvoices.domain.model.Company;
import io.github.reinamarlon.localinvoices.domain.model.CompanyDraft;
import io.github.reinamarlon.localinvoices.domain.model.InvoiceItem;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.BillRepository;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.CompanyRepository;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.SettingsRepository;

import java.util.List;

public final class InvoiceService {
    private final CompanyRepository companyRepository;
    private final BillRepository billRepository;
    private final SettingsRepository settingsRepository;

    public InvoiceService(CompanyRepository companyRepository, BillRepository billRepository, SettingsRepository settingsRepository) {
        this.companyRepository = companyRepository;
        this.billRepository = billRepository;
        this.settingsRepository = settingsRepository;
    }

    public List<Company> companies() {
        return companyRepository.findAll();
    }

    public Company registerCompany(CompanyDraft draft) {
        validateCompany(draft);
        return companyRepository.create(draft);
    }

    public void deleteCompany(Company company) {
        companyRepository.delete(company);
    }

    public AppSettings settings() {
        return settingsRepository.load();
    }

    public void saveSettings(AppSettings settings) {
        settingsRepository.save(settings);
    }

    public int nextConsecutive(Company company) {
        return billRepository.currentConsecutive(company) + 1;
    }

    public int saveBill(BillDraft draft) {
        validateBill(draft);
        return billRepository.save(draft);
    }

    public List<Bill> bills(Company company, String filterType, String filterValue) {
        return billRepository.list(company, filterType, filterValue);
    }

    private void validateCompany(CompanyDraft draft) {
        if (draft.commercialName().trim().isEmpty()) throw new IllegalArgumentException("El nombre comercial es obligatorio.");
        if (draft.legalName().trim().isEmpty()) throw new IllegalArgumentException("La razon social es obligatoria.");
        if (draft.taxId().trim().isEmpty()) throw new IllegalArgumentException("El NIT o identificacion fiscal es obligatorio.");
        if (draft.city().trim().isEmpty()) throw new IllegalArgumentException("La ciudad es obligatoria.");
        if (draft.country().trim().isEmpty()) throw new IllegalArgumentException("El pais es obligatorio.");
    }

    private void validateBill(BillDraft draft) {
        if (draft.receivedFrom().trim().isEmpty()) throw new IllegalArgumentException("El nombre o razon social del cliente es obligatorio.");
        if (draft.customerId().trim().isEmpty()) throw new IllegalArgumentException("La cedula o NIT del cliente es obligatorio.");
        if (draft.customerEmail().trim().isEmpty()) throw new IllegalArgumentException("El correo electronico del cliente es obligatorio.");
        if (draft.items() == null || draft.items().isEmpty()) throw new IllegalArgumentException("Agrega al menos un item al recibo.");
        for (InvoiceItem item : draft.items()) {
            if (item.description().isBlank()) throw new IllegalArgumentException("Cada item necesita una descripcion.");
            if (item.quantity() <= 0) throw new IllegalArgumentException("La cantidad de cada item debe ser mayor a cero.");
            if (item.unitPrice() < 0) throw new IllegalArgumentException("El precio unitario no puede ser negativo.");
        }
        if (draft.total() <= 0) throw new IllegalArgumentException("El total del recibo debe ser mayor a cero.");
        if (draft.paymentMethod().trim().isEmpty()) throw new IllegalArgumentException("Selecciona o escribe un medio de pago.");
    }
}
