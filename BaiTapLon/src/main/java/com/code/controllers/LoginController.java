package com.code.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink linkRegister;

    @FXML
    public void initialize() {

        // Nhập xong username → Enter → nhảy xuống password
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        // Đang ở password → Enter → click đăng nhập luôn
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        // Nút đăng nhập
        loginButton.setOnAction(e -> handleLogin());

        // Link đăng ký
        linkRegister.setOnAction(e -> handleRegister());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đủ thông tin.");
            return;
        }

        if (authenticate(username, password)) {
            navigateTo("/com/code/views/Dashboard.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Sai thông tin", "Tên đăng nhập hoặc mật khẩu không đúng.");
            passwordField.clear();
            passwordField.requestFocus(); // focus lại ô mật khẩu
        }
    }

    private void handleRegister() {
        navigateTo("/com/code/views/Register.fxml");
    }

    private boolean authenticate(String username, String password) {
        // TODO: thay bằng truy vấn DB
        return username.equals("admin") && password.equals("1234");
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy màn hình: " + fxmlPath);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}