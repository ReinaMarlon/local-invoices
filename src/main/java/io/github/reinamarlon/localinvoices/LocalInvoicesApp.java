package io.github.reinamarlon.localinvoices;

import io.github.reinamarlon.localinvoices.domain.model.Bill;
import io.github.reinamarlon.localinvoices.domain.model.BillDraft;
import io.github.reinamarlon.localinvoices.domain.model.AppSettings;
import io.github.reinamarlon.localinvoices.domain.model.Company;
import io.github.reinamarlon.localinvoices.domain.model.CompanyDraft;
import io.github.reinamarlon.localinvoices.domain.model.InvoiceItem;
import io.github.reinamarlon.localinvoices.domain.service.InvoiceService;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.BillRepository;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.CompanyRepository;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.DatabaseManager;
import io.github.reinamarlon.localinvoices.infrastructure.persistence.SettingsRepository;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public final class LocalInvoicesApp extends Application {
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final NumberFormat COP = NumberFormat.getCurrencyInstance(COLOMBIA);
    private static final String APP_VERSION = "1.0.0";

    private DatabaseManager database;
    private InvoiceService service;
    private Stage stage;
    private StackPane shell;
    private StackPane loaderOverlay;
    private BorderPane workspace;
    private Company selectedCompany;
    private AppSettings appSettings;
    private Label dateTimeLabel;
    private Label invoiceNumber;
    private Label invoiceValue;
    private TextField cityDate;
    private TextField receivedFrom;
    private TextField customerId;
    private TextField customerPhone;
    private TextField customerEmail;
    private TextField customerAddress;
    private ComboBox<String> customerDocumentType;
    private ComboBox<String> paymentMethod;
    private TextField customPaymentMethod;
    private TextArea notes;
    private VBox itemRows;
    private final List<InvoiceItemEditor> itemEditors = new ArrayList<>();
    private Node invoiceNode;
    private ListView<Bill> billList;
    private VBox previewBox;
    private TextField filterInput;
    private ComboBox<String> filterType;

    @Override
    public void start(Stage primaryStage) {
        database = new DatabaseManager();
        service = new InvoiceService(
            new CompanyRepository(database.connection()),
            new BillRepository(database.connection()),
            new SettingsRepository(database.connection())
        );
        appSettings = service.settings();

        stage = primaryStage;
        stage.setTitle("Local Invoices v" + applicationVersion());
        stage.getIcons().add(resourceImage("/io/github/reinamarlon/localinvoices/images/icon.png"));
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setMaximized(true);

        shell = new StackPane();
        loaderOverlay = createLoaderOverlay();
        Scene scene = new Scene(shell, 1200, 800);
        scene.getStylesheets().add(LocalInvoicesApp.class.getResource("/io/github/reinamarlon/localinvoices/styles/app.css").toExternalForm());
        stage.setScene(scene);
        applySettings(appSettings);
        showSelector();
        stage.show();
    }

    private void showSelector() {
        List<Company> companies = service.companies();
        Label eyebrow = new Label("LOCAL INVOICES");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Elige tu espacio de trabajo");
        title.getStyleClass().add("selector-title");
        Label subtitle = new Label("Administra tus recibos desde una empresa registrada.");
        subtitle.getStyleClass().add("selector-subtitle");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        Button create = new Button("Registrar empresa");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> showCompanyForm());
        actions.getChildren().add(create);

        FlowPane options = new FlowPane(18, 18);
        options.setAlignment(Pos.CENTER);
        options.getChildren().setAll(companies.stream().map(this::companyCard).toList());

        Label empty = new Label("Aun no hay empresas registradas. Crea la primera para comenzar.");
        empty.getStyleClass().addAll("muted", "empty-state");

        VBox content = new VBox(18, eyebrow, title, subtitle, companies.isEmpty() ? empty : options, actions);
        content.getStyleClass().add("selector-panel");
        content.setAlignment(Pos.CENTER);

        ScrollPane selector = centeredScroll(content);
        selector.getStyleClass().add("selector-page");
        setContent(selector);
    }

    private Node companyCard(Company company) {
        ImageView logo = companyLogo(company, 155);
        Label name = new Label(company.label());
        Label taxId = new Label(company.taxId());
        taxId.getStyleClass().add("muted");

        name.getStyleClass().add("company-name");
        Button open = new Button("Abrir");
        open.getStyleClass().addAll("primary-button", "card-action");
        open.setOnAction(event -> showWorkspace(company));
        Button delete = new Button("Eliminar");
        delete.getStyleClass().add("ghost-danger-button");
        delete.setOnAction(event -> confirmCompanyDeletion(company));
        HBox actions = new HBox(8, open, delete);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, logo, name, taxId, spacerVertical(), actions);
        card.getStyleClass().add("company-card");
        card.setAlignment(Pos.CENTER);
        animateCardHover(card);
        return card;
    }

    private void showCompanyForm() {
        TextField commercialName = new TextField();
        TextField legalName = new TextField();
        TextField taxId = new TextField();
        TextField city = new TextField("Cali");
        TextField country = new TextField("Colombia");
        TextField phone = new TextField();
        TextField logoPath = new TextField();
        logoPath.setEditable(false);

        Button chooseLogo = new Button("Elegir foto/logo");
        chooseLogo.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar foto o logo");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) logoPath.setText(file.getAbsolutePath());
        });

        GridPane form = grid();
        addRow(form, 0, "Nombre comercial", commercialName);
        addRow(form, 1, "Razon social", legalName);
        addRow(form, 2, "NIT / ID fiscal", taxId);
        addRow(form, 3, "Ciudad", city);
        addRow(form, 4, "Pais", country);
        addRow(form, 5, "Telefono", phone);
        form.add(new Label("Foto/logo"), 0, 6);
        HBox logoRow = new HBox(10, logoPath, chooseLogo);
        HBox.setHgrow(logoPath, Priority.ALWAYS);
        form.add(logoRow, 1, 6);

        Button save = new Button("Guardar empresa");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                Company created = service.registerCompany(new CompanyDraft(
                    commercialName.getText(),
                    legalName.getText(),
                    taxId.getText(),
                    city.getText(),
                    country.getText(),
                    phone.getText(),
                    logoPath.getText()
                ));
                showWorkspace(created);
            } catch (RuntimeException ex) {
                alert(Alert.AlertType.WARNING, "Revisa la empresa", rootMessage(ex));
            }
        });

        Button cancel = new Button("Cancelar");
        cancel.setOnAction(event -> showSelector());

        Label eyebrow = new Label("CONFIGURACION INICIAL");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Registra una empresa");
        title.getStyleClass().add("form-title");
        Label helper = new Label("Esta informacion aparecera en tus recibos y se guardara en este equipo.");
        helper.getStyleClass().add("muted");
        cancel.getStyleClass().add("secondary-button");
        VBox panel = new VBox(16, eyebrow, title, helper, form, new HBox(12, save, cancel));
        panel.getStyleClass().add("selector-panel");
        panel.setMaxWidth(720);

        ScrollPane page = centeredScroll(panel);
        page.getStyleClass().add("selector-page");
        setContent(page);
    }

    private void showWorkspace(Company company) {
        selectedCompany = company;
        workspace = new BorderPane();
        workspace.setLeft(sidebar());
        workspace.setTop(topbar());
        workspace.setCenter(invoiceView());
        setContent(workspace);
        refreshConsecutive();
        startClock();
    }

    private Node sidebar() {
        Button biller = menuButton("Generar recibo");
        biller.setOnAction(event -> {
            switchWorkspaceCenter(invoiceView(), true);
        });

        Button history = menuButton("Historial");
        history.setOnAction(event -> {
            switchWorkspaceCenter(historyView(), false);
            loadBills();
        });

        Button companies = menuButton("Empresas");
        companies.setOnAction(event -> showSelector());

        Button settings = menuButton("Ajustes");
        settings.setOnAction(event -> switchWorkspaceCenter(settingsView(), false));

        Label brand = new Label("Local\nInvoices");
        brand.getStyleClass().add("sidebar-brand");
        Label section = new Label("ESPACIO DE TRABAJO");
        section.getStyleClass().add("sidebar-section");
        VBox sidebar = new VBox(12, brand, section, biller, history, companies, settings);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        return sidebar;
    }

    private Node topbar() {
        Label context = new Label("EMPRESA ACTIVA");
        context.getStyleClass().add("topbar-context");
        Label welcome = new Label(selectedCompany.label());
        welcome.getStyleClass().add("welcome");
        VBox company = new VBox(2, context, welcome);
        dateTimeLabel = new Label();
        dateTimeLabel.getStyleClass().add("topbar-date");
        HBox topbar = new HBox(company, spacer(), dateTimeLabel);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        return topbar;
    }

    private Node invoiceView() {
        invoiceNumber = new Label("---");
        invoiceValue = new Label(COP.format(0));
        cityDate = new TextField(selectedCompany.city() + ", " + LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", COLOMBIA)));
        receivedFrom = new TextField();
        customerDocumentType = new ComboBox<>(FXCollections.observableArrayList("Cedula de ciudadania", "NIT", "Pasaporte", "Consumidor final"));
        customerDocumentType.setValue("Cedula de ciudadania");
        customerId = numericField();
        customerDocumentType.valueProperty().addListener((obs, old, value) -> {
            if ("Consumidor final".equals(value) && customerId.getText().isBlank()) customerId.setText("222222222222");
        });
        customerPhone = numericField();
        customerEmail = new TextField();
        customerAddress = new TextField();
        paymentMethod = new ComboBox<>(FXCollections.observableArrayList("Efectivo", "Transferencia bancaria", "Tarjeta debito", "Tarjeta credito", "Nequi", "Daviplata", "Otro"));
        paymentMethod.setValue("Efectivo");
        customPaymentMethod = new TextField();
        customPaymentMethod.setPromptText("Escribe el medio de pago");
        customPaymentMethod.setVisible(false);
        customPaymentMethod.setManaged(false);
        paymentMethod.valueProperty().addListener((obs, old, value) -> {
            boolean isOther = "Otro".equals(value);
            customPaymentMethod.setVisible(isOther);
            customPaymentMethod.setManaged(isOther);
        });
        notes = new TextArea();
        notes.setPromptText("Informacion adicional, referencias o condiciones del pago.");
        notes.setPrefRowCount(3);
        itemRows = new VBox(9);
        itemRows.getStyleClass().add("items-list");
        itemEditors.clear();
        addItemEditor();

        invoiceNode = invoiceCard();
        Button save = new Button("Guardar recibo e imprimir");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveBill());

        Label title = new Label("Nuevo recibo");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Comprobante local con detalle de productos o servicios.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox heading = new VBox(4, title, subtitle);
        heading.getStyleClass().add("page-heading");
        VBox page = new VBox(18, heading, invoiceNode, save);
        page.getStyleClass().add("content-page");
        page.setAlignment(Pos.TOP_CENTER);
        return centeredScroll(page);
    }

    private Node invoiceCard() {
        FlowPane header = new FlowPane(24, 16);
        header.getStyleClass().add("invoice-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox companyInfo = new VBox(4,
            new Label(selectedCompany.legalName()),
            new Label(selectedCompany.taxId()),
            new Label(selectedCompany.city() + " - " + selectedCompany.country()),
            new Label("Tel: " + selectedCompany.phone())
        );
        companyInfo.getStyleClass().add("company-info");

        VBox numberBox = new VBox(5,
            new Label("RECIBO DE CAJA"),
            new HBox(new Label("No. "), invoiceNumber),
            new Label("Fecha y Hora"),
            cityDate
        );
        numberBox.getStyleClass().add("number-box");
        numberBox.setAlignment(Pos.CENTER);

        header.getChildren().addAll(companyLogo(selectedCompany, 210), companyInfo, numberBox);

        GridPane client = grid();
        addRow(client, 0, "Nombre / razon social", receivedFrom);
        addRow(client, 1, "Tipo de documento", customerDocumentType);
        addRow(client, 2, "Numero de documento", customerId);
        addRow(client, 3, "Correo electronico", customerEmail);
        addRow(client, 4, "Telefono", customerPhone);
        addRow(client, 5, "Direccion", customerAddress);

        Label itemsHint = new Label("Agrega cada producto o servicio con su cantidad y precio unitario.");
        itemsHint.getStyleClass().add("muted");
        Button addItem = new Button("Agregar item");
        addItem.getStyleClass().add("secondary-button");
        addItem.setOnAction(event -> addItemEditor());
        HBox itemHeader = new HBox(new Label("Detalle de productos y servicios"), spacer(), addItem);
        itemHeader.getStyleClass().add("items-header");
        itemHeader.setAlignment(Pos.CENTER_LEFT);
        HBox totals = new HBox(new Label("TOTAL"), spacer(), invoiceValue);
        totals.getStyleClass().add("invoice-total");
        totals.setAlignment(Pos.CENTER_LEFT);
        VBox detail = new VBox(10, itemHeader, itemsHint, itemRows, totals);
        detail.getStyleClass().add("items-section");

        HBox paymentFields = new HBox(10, paymentMethod, customPaymentMethod);
        HBox.setHgrow(paymentMethod, Priority.ALWAYS);
        HBox.setHgrow(customPaymentMethod, Priority.ALWAYS);
        VBox payment = new VBox(8, new Label("MEDIO DE PAGO"), paymentFields);
        payment.getStyleClass().add("section-block");
        VBox observations = new VBox(8, new Label("OBSERVACIONES"), notes);
        observations.getStyleClass().add("section-block");
        Label localNotice = new Label("Comprobante local: no equivale a una factura electronica validada por DIAN.");
        localNotice.getStyleClass().addAll("muted", "local-notice");

        Label clientTitle = new Label("Datos del cliente");
        clientTitle.getStyleClass().add("section-title");
        VBox invoice = new VBox(20, header, clientTitle, client, detail, payment, observations, localNotice);
        invoice.getStyleClass().add("invoice-card");
        return invoice;
    }

    private Node historyView() {
        filterType = new ComboBox<>(FXCollections.observableArrayList("cedula", "nombre", "telefono"));
        filterType.getSelectionModel().selectFirst();
        filterInput = new TextField();
        filterInput.setPromptText("Ingrese busqueda...");
        Button search = new Button("Buscar");
        search.getStyleClass().add("secondary-button");
        search.setOnAction(event -> loadBills());
        filterInput.setOnAction(event -> loadBills());

        HBox filters = new HBox(10, new Label("Filtrar por:"), filterType, filterInput, search);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("filters");

        billList = new ListView<>();
        billList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Bill item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.companyName() + " - #" + pad(item.consecutive()) + "\n" + item.cityDate());
            }
        });
        billList.getSelectionModel().selectedItemProperty().addListener((obs, old, bill) -> showPreview(bill));

        Label title = new Label("Historial de recibos");
        title.getStyleClass().add("page-title");
        Label listTitle = new Label("Recibos guardados");
        listTitle.getStyleClass().add("section-title");
        VBox left = new VBox(14, title, filters, listTitle, billList);
        left.getStyleClass().add("history-left");
        VBox.setVgrow(billList, Priority.ALWAYS);

        previewBox = new VBox();
        previewBox.getStyleClass().add("preview-card");
        showPreview(null);

        SplitPane layout = new SplitPane(left, previewBox);
        layout.getStyleClass().addAll("content-page", "history-split");
        layout.setDividerPositions(0.55);
        return layout;
    }

    private Node settingsView() {
        ComboBox<String> theme = new ComboBox<>(FXCollections.observableArrayList("Claro", "Oscuro"));
        ComboBox<String> accent = new ComboBox<>(FXCollections.observableArrayList("Azul", "Verde", "Coral"));
        ComboBox<String> fontSize = new ComboBox<>(FXCollections.observableArrayList("Compacto", "Normal", "Grande"));
        ComboBox<String> density = new ComboBox<>(FXCollections.observableArrayList("Comoda", "Compacta"));
        theme.setValue(appSettings.theme());
        accent.setValue(appSettings.accent());
        fontSize.setValue(appSettings.fontSize());
        density.setValue(appSettings.density());

        GridPane preferences = grid();
        addRow(preferences, 0, "Apariencia", theme);
        addRow(preferences, 1, "Color de acento", accent);
        addRow(preferences, 2, "Tamano de texto", fontSize);
        addRow(preferences, 3, "Densidad de interfaz", density);

        Button save = new Button("Guardar ajustes");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            appSettings = new AppSettings(theme.getValue(), accent.getValue(), fontSize.getValue(), density.getValue());
            service.saveSettings(appSettings);
            applySettings(appSettings);
            alert(Alert.AlertType.INFORMATION, "Ajustes guardados", "Tus preferencias se aplicaron y quedaran guardadas en este equipo.");
        });

        VBox preferencesCard = new VBox(18, sectionHeading("Preferencias", "Personaliza como se ve Local Invoices."), preferences, save);
        preferencesCard.getStyleClass().add("settings-card");

        Label companyName = new Label(selectedCompany.label());
        companyName.getStyleClass().add("danger-company-name");
        Label warning = new Label("Esto eliminara esta empresa, sus recibos y su consecutivo local de forma permanente.");
        warning.getStyleClass().add("muted");
        Button remove = new Button("Eliminar empresa");
        remove.getStyleClass().add("danger-button");
        remove.setOnAction(event -> confirmCompanyDeletion(selectedCompany));
        VBox dangerCard = new VBox(12, sectionHeading("Zona delicada", "Acciones que no se pueden deshacer."), companyName, warning, remove);
        dangerCard.getStyleClass().addAll("settings-card", "danger-card");

        Label title = new Label("Ajustes");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Define una experiencia de trabajo que se sienta tuya.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox heading = new VBox(4, title, subtitle);
        heading.getStyleClass().add("page-heading");
        VBox page = new VBox(18, heading, preferencesCard, dangerCard);
        page.getStyleClass().add("content-page");
        page.setMaxWidth(760);
        return centeredScroll(page);
    }

    private VBox sectionHeading(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("muted");
        return new VBox(3, title, subtitle);
    }

    private void confirmCompanyDeletion(Company company) {
        TextField confirmation = new TextField();
        confirmation.setPromptText("Escribe: " + company.label());
        VBox content = new VBox(10,
            new Label("Para confirmar, escribe exactamente el nombre de la empresa:"),
            new Label(company.label()),
            confirmation
        );
        content.getStyleClass().add("delete-confirmation");

        Alert dialog = styledAlert(Alert.AlertType.CONFIRMATION, "Eliminar empresa", "Esta accion es permanente", null);
        dialog.getDialogPane().setContent(content);
        ButtonType delete = new ButtonType("Eliminar definitivamente", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(ButtonType.CANCEL, delete);
        dialog.getDialogPane().lookupButton(delete).getStyleClass().add("danger-button");
        dialog.showAndWait().filter(button -> button == delete).ifPresent(button -> {
            if (!company.label().equals(confirmation.getText().trim())) {
                alert(Alert.AlertType.WARNING, "Nombre incorrecto", "El nombre escrito no coincide. La empresa no fue eliminada.");
                return;
            }
            service.deleteCompany(company);
            if (selectedCompany != null && selectedCompany.id() == company.id()) selectedCompany = null;
            showSelector();
        });
    }

    private void applySettings(AppSettings settings) {
        shell.getStyleClass().removeIf(style -> style.startsWith("theme-") || style.startsWith("accent-") || style.startsWith("font-") || style.startsWith("density-"));
        shell.getStyleClass().add(settings.theme().equals("Oscuro") ? "theme-dark" : "theme-light");
        shell.getStyleClass().add("accent-" + settings.accent().toLowerCase(Locale.ROOT));
        shell.getStyleClass().add(switch (settings.fontSize()) {
            case "Compacto" -> "font-compact";
            case "Grande" -> "font-large";
            default -> "font-normal";
        });
        shell.getStyleClass().add(settings.density().equals("Compacta") ? "density-compact" : "density-comfortable");
    }

    private void saveBill() {
        try {
            Alert confirm = styledAlert(Alert.AlertType.CONFIRMATION, "Guardar recibo", "Deseas guardar el recibo?", "No podras modificarlo despues.");
            confirm.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
                int saved = service.saveBill(new BillDraft(
                    selectedCompany.id(),
                    cityDate.getText(),
                    customerDocumentType.getValue(),
                    receivedFrom.getText().trim(),
                    digits(customerId.getText()),
                    digits(customerPhone.getText()),
                    customerEmail.getText().trim(),
                    customerAddress.getText().trim(),
                    itemEditors.stream().map(InvoiceItemEditor::toItem).toList(),
                    selectedPaymentMethod(),
                    notes.getText().trim()
                ));
                invoiceNumber.setText(pad(saved));
                copyInvoiceToClipboard();
                alert(Alert.AlertType.INFORMATION, "Recibo guardado", "Consecutivo: " + saved + ". Se copio una vista del comprobante al portapapeles.");
                clearForm();
                refreshConsecutive();
            });
        } catch (RuntimeException ex) {
            alert(Alert.AlertType.WARNING, "Revisa el formulario", rootMessage(ex));
        }
    }

    private void addItemEditor() {
        InvoiceItemEditor editor = new InvoiceItemEditor();
        itemEditors.add(editor);
        itemRows.getChildren().add(editor.node());
        refreshInvoiceTotals();
    }

    private void removeItemEditor(InvoiceItemEditor editor) {
        if (itemEditors.size() == 1) return;
        itemEditors.remove(editor);
        itemRows.getChildren().remove(editor.node());
        refreshInvoiceTotals();
    }

    private void refreshInvoiceTotals() {
        if (invoiceValue != null) invoiceValue.setText(COP.format(itemEditors.stream().mapToInt(editor -> editor.toItem().lineTotal()).sum()));
    }

    private String selectedPaymentMethod() {
        return "Otro".equals(paymentMethod.getValue()) ? customPaymentMethod.getText().trim() : paymentMethod.getValue();
    }

    private void loadBills() {
        billList.setItems(FXCollections.observableArrayList(
            service.bills(selectedCompany, filterType.getValue(), filterInput.getText())
        ));
    }

    private void showPreview(Bill bill) {
        previewBox.getChildren().clear();
        if (bill == null) {
            Label empty = new Label("Selecciona un recibo para ver su detalle");
            empty.getStyleClass().addAll("muted", "preview-empty");
            previewBox.getChildren().add(empty);
            return;
        }
        Label receipt = new Label("RECIBO DE CAJA");
        receipt.getStyleClass().add("preview-kicker");
        Label number = new Label("No. " + pad(bill.consecutive()));
        number.getStyleClass().add("preview-number");
        Label total = new Label(COP.format(bill.amount()));
        total.getStyleClass().add("preview-total");
        previewBox.getChildren().addAll(
            receipt, number, total,
            imageView(bill.companyLogoPath(), 190),
            new Label(bill.companyName()),
            new Label(bill.companyTaxId()),
            new Label(bill.cityDate()),
            new Label("Cliente: " + bill.receivedFrom()),
            new Label((bill.customerDocumentType() == null ? "Documento" : bill.customerDocumentType()) + ": " + bill.customerId()),
            new Label("Correo: " + displayOrDash(bill.customerEmail())),
            new Label("Telefono: " + displayOrDash(bill.customerPhone())),
            new Label("Direccion: " + displayOrDash(bill.customerAddress())),
            previewItems(bill.items()),
            new Label("Medio de pago: " + bill.paymentMethod()),
            new Label("Observaciones: " + bill.notes())
        );
    }

    private VBox previewItems(List<InvoiceItem> items) {
        VBox rows = new VBox(5);
        rows.getStyleClass().add("preview-items");
        Label title = new Label("DETALLE");
        title.getStyleClass().add("preview-kicker");
        rows.getChildren().add(title);
        for (InvoiceItem item : items) {
            Label row = new Label(item.quantity() + " x " + item.description() + "  |  " + COP.format(item.lineTotal()));
            row.getStyleClass().add("preview-item");
            rows.getChildren().add(row);
        }
        return rows;
    }

    private void refreshConsecutive() {
        invoiceNumber.setText(pad(service.nextConsecutive(selectedCompany)));
    }

    private void setContent(Parent content) {
        showLoader(() -> {
            shell.getChildren().setAll(content, loaderOverlay);
            animateEntrance(content);
        });
    }

    private void switchWorkspaceCenter(Node content, boolean refreshNumber) {
        showLoader(() -> {
            workspace.setCenter(content);
            if (refreshNumber) refreshConsecutive();
            animateEntrance(content);
        });
    }

    private void showLoader(Runnable action) {
        if (!shell.getChildren().contains(loaderOverlay)) {
            shell.getChildren().add(loaderOverlay);
        }

        loaderOverlay.setVisible(true);
        loaderOverlay.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(120), loaderOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(320));
        hold.setOnFinished(event -> action.run());

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), loaderOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> loaderOverlay.setVisible(false));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    private StackPane createLoaderOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("loader-overlay");
        overlay.setVisible(false);
        overlay.setPickOnBounds(true);

        GridPane blocks = new GridPane();
        blocks.getStyleClass().add("loader-grid");
        blocks.setHgap(10);
        blocks.setVgap(10);
        blocks.setAlignment(Pos.CENTER);

        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Rectangle block = new Rectangle(28, 28);
                block.getStyleClass().add("loader-block");
                block.setArcWidth(8);
                block.setArcHeight(8);
                block.setOpacity(0);
                animateLoaderBlock(block, index++);
                blocks.add(block, col, row);
            }
        }

        overlay.getChildren().add(blocks);
        return overlay;
    }

    private void animateLoaderBlock(Rectangle block, int index) {
        FadeTransition fade = new FadeTransition(Duration.millis(880), block);
        fade.setFromValue(0.1);
        fade.setToValue(1);
        fade.setAutoReverse(true);
        fade.setCycleCount(Timeline.INDEFINITE);
        fade.setDelay(Duration.millis(65L * (index + 1)));

        FillTransition pulse = new FillTransition(Duration.millis(880), block);
        pulse.setFromValue(Color.web("#0c3574"));
        pulse.setToValue(Color.web("#418cee"));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setDelay(Duration.millis(65L * (index + 1)));

        new ParallelTransition(fade, pulse).play();
    }

    private Button menuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private void addRow(GridPane grid, int row, String label, Node field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private GridPane grid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(125);
        labelColumn.setPrefWidth(155);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return grid;
    }

    private TextField numericField() {
        TextField field = new TextField();
        field.textProperty().addListener((obs, old, value) -> {
            String clean = digits(value);
            if (!clean.equals(value)) field.setText(clean);
        });
        return field;
    }

    private TextField moneyField() {
        TextField field = new TextField();
        field.textProperty().addListener((obs, old, value) -> {
            String clean = digits(value);
            if (!clean.equals(value)) field.setText(clean);
        });
        return field;
    }

    private final class InvoiceItemEditor {
        private final TextField description = new TextField();
        private final TextField quantity = numericField();
        private final TextField unitPrice = moneyField();
        private final Label lineTotal = new Label(COP.format(0));
        private final HBox row;

        private InvoiceItemEditor() {
            description.setPromptText("Descripcion del producto o servicio");
            quantity.setPromptText("Cant.");
            quantity.setText("1");
            quantity.setPrefWidth(76);
            unitPrice.setPromptText("Precio unitario");
            unitPrice.setPrefWidth(150);
            lineTotal.getStyleClass().add("item-line-total");
            Button remove = new Button("Quitar");
            remove.getStyleClass().add("ghost-danger-button");
            remove.setOnAction(event -> removeItemEditor(this));

            description.textProperty().addListener((obs, old, value) -> refreshLineTotal());
            quantity.textProperty().addListener((obs, old, value) -> refreshLineTotal());
            unitPrice.textProperty().addListener((obs, old, value) -> refreshLineTotal());
            row = new HBox(8, description, quantity, unitPrice, lineTotal, remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("item-row");
            HBox.setHgrow(description, Priority.ALWAYS);
            refreshLineTotal();
        }

        private Node node() {
            return row;
        }

        private InvoiceItem toItem() {
            return new InvoiceItem(description.getText().trim(), number(quantity.getText()), moneyValue(unitPrice.getText()));
        }

        private void refreshLineTotal() {
            lineTotal.setText(COP.format(toItem().lineTotal()));
            refreshInvoiceTotals();
        }
    }

    private ImageView companyLogo(Company company, double width) {
        return imageView(company.logoPath(), width);
    }

    private ImageView imageView(String filePath, double width) {
        Image image = (filePath == null || filePath.isBlank() || !Files.exists(Path.of(filePath)))
            ? resourceImage("/io/github/reinamarlon/localinvoices/images/icon.png")
            : new Image(Path.of(filePath).toUri().toString());
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setFitWidth(width);
        return view;
    }

    private Image resourceImage(String path) {
        return new Image(LocalInvoicesApp.class.getResourceAsStream(path));
    }

    private String applicationVersion() {
        String packagedVersion = LocalInvoicesApp.class.getPackage().getImplementationVersion();
        return packagedVersion == null || packagedVersion.isBlank() ? APP_VERSION : packagedVersion;
    }

    private Node spacer() {
        HBox box = new HBox();
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Node spacerVertical() {
        VBox box = new VBox();
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private ScrollPane centeredScroll(Node content) {
        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("scroll-content");
        StackPane.setAlignment(content, Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("workspace-scroll");
        return scroll;
    }

    private void animateEntrance(Node node) {
        node.setOpacity(0);
        node.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(Duration.millis(260), node);
        move.setToY(0);
        new ParallelTransition(fade, move).play();
    }

    private void animateCardHover(Node node) {
        node.setOnMouseEntered(event -> animateScale(node, 1.018));
        node.setOnMouseExited(event -> animateScale(node, 1));
    }

    private void animateScale(Node node, double scale) {
        javafx.animation.ScaleTransition transition = new javafx.animation.ScaleTransition(Duration.millis(130), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private void copyInvoiceToClipboard() {
        WritableImage image = invoiceNode.snapshot(new SnapshotParameters(), null);
        ClipboardContent content = new ClipboardContent();
        content.putImage(image);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void clearForm() {
        receivedFrom.clear();
        customerId.clear();
        customerPhone.clear();
        customerEmail.clear();
        customerAddress.clear();
        customerDocumentType.setValue("Cedula de ciudadania");
        paymentMethod.setValue("Efectivo");
        customPaymentMethod.clear();
        notes.clear();
        itemEditors.clear();
        itemRows.getChildren().clear();
        addItemEditor();
    }

    private String displayOrDash(String value) {
        return value == null || value.isBlank() ? "No registrado" : value;
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private int number(String value) {
        String clean = digits(value);
        return clean.isEmpty() ? 0 : Integer.parseInt(clean);
    }

    private int moneyValue(String value) {
        return number(value);
    }

    private String pad(int value) {
        return String.format("%03d", value);
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? ex.getMessage() : current.getMessage();
    }

    private void alert(Alert.AlertType type, String title, String message) {
        Alert alert = styledAlert(type, title, title, message);
        alert.showAndWait();
    }

    private Alert styledAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setGraphic(null);
        String stylesheet = LocalInvoicesApp.class.getResource("/io/github/reinamarlon/localinvoices/styles/app.css").toExternalForm();
        alert.getDialogPane().getStylesheets().add(stylesheet);
        alert.getDialogPane().getStyleClass().add("app-dialog");
        if (appSettings != null && "Oscuro".equals(appSettings.theme())) alert.getDialogPane().getStyleClass().add("theme-dark");
        if (appSettings != null) alert.getDialogPane().getStyleClass().add("accent-" + appSettings.accent().toLowerCase(Locale.ROOT));
        return alert;
    }

    private void startClock() {
        Thread clock = new Thread(() -> {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", COLOMBIA);
            while (dateTimeLabel != null && workspace != null) {
                Platform.runLater(() -> dateTimeLabel.setText(LocalDateTime.now().format(format)));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }, "local-invoices-clock");
        clock.setDaemon(true);
        clock.start();
    }

    @Override
    public void stop() throws Exception {
        if (database != null) database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
