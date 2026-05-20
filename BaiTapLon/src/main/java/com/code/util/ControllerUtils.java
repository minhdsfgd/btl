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
            //load giao deẹn mới
            Parent root = FXMLLoader.load(ControllerUtils.class.getResource(fxmlPath));

            // lấy stage hiện tại từ client app
            Stage stage = ClientApp.getStage();

            // LƯU LẠI KÍCH THƯỚC VÀ TRẠNG THÁI TRƯỚC KHI CHUYỂN TRANG
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            boolean isMaximized = stage.isMaximized();

            //Đặt giao diện mới vào
            stage.setScene(new Scene(root));

            //TRẢ LẠI KÍCH THƯỚC CŨ
            if (resetSize) {
                stage.setMaximized(false);
                stage.sizeToScene();
                stage.centerOnScreen();
            } else if (isMaximized) {
                // Phải set false trước rồi mới set true lại
                // nếu không JavaFX đôi khi bỏ qua lệnh setMaximized(true)
                stage.setMaximized(false);
                Platform.runLater(() -> stage.setMaximized(true));
                // Chỉ set lại kích thước nếu giá trị hợp lệ (tránh lúc ứng dụng vừa khởi động chưa có size)
            } else if (!Double.isNaN(currentWidth) && currentWidth > 0) {
                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);
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
