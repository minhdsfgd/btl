module com.example.baitaplon {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens com.example.baitaplon to javafx.fxml;
    exports com.example.baitaplon;
}