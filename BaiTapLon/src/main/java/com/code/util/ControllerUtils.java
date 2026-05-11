package com.code.util;

import com.code.ClientApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import java.io.IOException;

public class ControllerUtils {
    public static void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(ControllerUtils.class.getResource(fxmlPath));
            ClientApp.getStage().setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy màn hình: " + fxmlPath);
        }
    }
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
