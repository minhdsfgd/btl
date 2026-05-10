module com.code {
    // ── JavaFX ────────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.formsfx;

    // ── Java I/O & SQL ────────────────────────────────────────────────────────
    requires java.sql;

    // ── Controllers cần mở để JavaFX inject @FXML ─────────────────────────────
    opens com.code.controllers to javafx.fxml;

    // ── Mở models cho javafx.base để TableView binding hoạt động ──────────────
    opens com.code.models to javafx.base;

    // ── Exports ───────────────────────────────────────────────────────────────
    exports com.code;
    exports com.code.client;
    exports com.code.models;
    exports com.code.network;
    exports com.code.service;
    exports com.code.exception;
    exports com.code.repository;
    exports com.code.util;
}