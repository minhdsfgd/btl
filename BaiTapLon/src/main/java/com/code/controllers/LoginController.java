package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Role;
import com.code.models.User;
import com.code.network.LoginData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Hyperlink     linkRegister;
    @FXML ImageView backgroundImage;
    @FXML Region rootPane;

    @FXML
    public void initialize() {
        if (backgroundImage != null && rootPane != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
        
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus();
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
        loginButton.setOnAction(e -> handleLogin());
        linkRegister.setOnAction(e -> navigateTo("/com/code/views/Register.fxml"));
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đủ thông tin.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        new Thread(() -> {
            try {
                Request req = Request.of(RequestType.LOGIN,
                        new LoginData(username, password));
                Response res = SocketClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Đăng nhập");

                    if (res.isSuccess()) {
                        User user = res.getDataAs(User.class);
                        SessionManager.setUser(user);

                        if (user.hasRole(Role.ADMIN)) {
                            navigateTo("/com/code/views/AdminPanel.fxml");
                        } else if (user.hasRole(Role.SELLER)) {
                            navigateTo("/com/code/views/SellerDashboard.fxml");
                        } else {
                            navigateTo("/com/code/views/AuctionList.fxml");
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", res.getMessage());
                        passwordField.clear();
                        passwordField.requestFocus();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Đăng nhập");
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                            "Không thể liên lạc với server: " + ex.getMessage());
                });
            }
        }, "login-thread").start();
    }

    /** Xóa session khi logout — gọi từ các controller khác. */
    public static void clearSession() {
        SessionManager.clear();
    }

    // ─── Backward-compat helpers (các controller khác gọi) ──────────────────

    public static String getCurrentUsername() {
        return SessionManager.getUsername();
    }

    public static boolean currentUserHasRole(Role role) {
        return SessionManager.hasRole(role);
    }
}