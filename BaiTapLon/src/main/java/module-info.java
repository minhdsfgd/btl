module com.code {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens com.code.controllers to javafx.fxml;
    exports com.code.models;
    exports com.code.service;
    exports com.code.util;
    exports com.code;
}