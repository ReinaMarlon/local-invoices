module io.github.reinamarlon.localinvoices {
    requires javafx.controls;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports io.github.reinamarlon.localinvoices;
    exports io.github.reinamarlon.localinvoices.domain.model;
    exports io.github.reinamarlon.localinvoices.domain.service;
    exports io.github.reinamarlon.localinvoices.infrastructure.persistence;
}
