module com.code {
    // ── JavaFX ────────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.formsfx;

    // ── Java I/O & SQL ────────────────────────────────────────────────────────
    requires java.sql;
    requires jbcrypt;

    // ── Mở com.code để javafx.graphics có thể gọi Application.start() ─────────
    // FIX: thiếu dòng này → NullPointerException khi launch ClientApp
    opens com.code to javafx.fxml, javafx.graphics;

    // ── Controllers cần mở để JavaFX inject @FXML ─────────────────────────────
    opens com.code.controllers to javafx.fxml, javafx.base;

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