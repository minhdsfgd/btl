package com.code.util;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.ClientApp;
import com.code.network.Response;
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

    /**
     * Kiểm tra Response có phải do user bị ban không.
     * Nếu có: dọn session, navigate về Login, return true.
     * Nếu không: return false để caller xử lý bình thường.
     *
     * <pre>
     * // Cách dùng trong controller:
     * if (ControllerUtils.handleBanResponse(res)) return;
     * // xử lý lỗi bình thường...
     * </pre>
     */
    public static boolean handleBanResponse(Response res) {
        if (res == null || res.isSuccess()) return false;
        String msg = res.getMessage();
        if (msg != null && (msg.contains("bị cấm") || msg.contains("Tài khoản bị cấm")
                || msg.contains("UserBannedException"))) {
            Platform.runLater(() -> {
                try { SocketClient.getInstance().stopListening(); } catch (Exception ignored) {}
                SessionManager.clear();
                showAlert(Alert.AlertType.WARNING, "Tài khoản bị khóa",
                        "Tài khoản của bạn đã bị quản trị viên khóa.\nBạn sẽ được chuyển về trang đăng nhập.");
                navigateTo("/com/code/views/Login.fxml", true);
            });
            return true;
        }
        return false;
    }
}
