package com.code.util;

import com.code.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerUtils {
    public static void navigateTo(String fxmlPath) {
        navigateTo(fxmlPath, false);
    }
    public static void navigateTo(String fxmlPath, boolean resetSize) {
        try {
            Parent root = FXMLLoader.load(ControllerUtils.class.getResource(fxmlPath));
            Stage stage = ClientApp.getStage();

            if (resetSize) {
                // Màn hình login/splash cần reset kích thước → vẫn dùng scene mới
                stage.setMaximized(false);
                stage.setScene(new Scene(root));
                stage.sizeToScene();
                stage.centerOnScreen();
            } else {
                // Chỉ đổi root, stage không bị động gì cả
                stage.getScene().setRoot(root);
            }

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
