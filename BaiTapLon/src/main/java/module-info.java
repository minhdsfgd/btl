module com.code {
    // ── JavaFX ────────────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires com.dlsc.formsfx;

    // ── FIX: Thêm java.sql cho JDBC (kết nối MySQL sau này) ──────────────────
    requires java.sql;

    // ── Controllers cần mở để JavaFX inject @FXML ─────────────────────────────
    opens com.code.controllers to javafx.fxml;

    // ── FIX: Mở models cho javafx.base để TableView binding hoạt động ─────────
    opens com.code.models to javafx.base;

    // ── Exports ───────────────────────────────────────────────────────────────
    exports com.code;
    exports com.code.models;
    exports com.code.service;

    // ── FIX: Thêm các package còn thiếu ──────────────────────────────────────
    exports com.code.exception;
    exports com.code.repository;
    exports com.code.util;
}