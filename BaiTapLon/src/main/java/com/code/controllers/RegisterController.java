package com.code.controllers;

import com.code.client.SocketClient;
import com.code.models.Role;
import com.code.network.LoginData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;
    @FXML private Button        registerButton;
    @FXML private Label         linkLogin;

    @FXML
    public void initialize() {
        fullNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) usernameField.requestFocus();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus();
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) confirmPasswordField.requestFocus();
        });
        confirmPasswordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleRegister();
        });

        registerButton.setOnAction(e -> handleRegister());
        linkLogin.setOnMouseClicked(e -> navigateTo("/com/code/views/Login.fxml"));

        fullNameField.textProperty().addListener((obs, old, val) -> clearError());
        usernameField.textProperty().addListener((obs, old, val) -> clearError());
        passwordField.textProperty().addListener((obs, old, val) -> clearError());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> clearError());
    }

    private void handleRegister() {
        String fullName         = fullNameField.getText().trim();
        String username         = usernameField.getText().trim();
        String password         = passwordField.getText();
        String confirmPassword  = confirmPasswordField.getText();

        if (!validate(fullName, username, password, confirmPassword)) return;

        registerButton.setDisable(true);
        registerButton.setText("Đang xử lý...");

        new Thread(() -> {
            try {
                // Server nhận LoginData; username = username, password = password
                // fullName hiện tại chưa được model User hỗ trợ riêng → bỏ qua hoặc truyền thêm
                Request req = Request.of(RequestType.REGISTER,
                        new LoginData(username, password, Role.BIDDER));
                Response res = SocketClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Đăng ký");

                    if (res.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Tài khoản đã được tạo!\nVui lòng đăng nhập.");
                        navigateTo("/com/code/views/Login.fxml");
                    } else {
                        showError(res.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Đăng ký");
                    showError("Lỗi kết nối: " + ex.getMessage());
                });
            }
        }, "register-thread").start();
    }

    private boolean validate(String fullName, String username,
                             String password, String confirmPassword) {
        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ tất cả các trường.");
            return false;
        }
        if (username.length() < 4) {
            showError("Tên đăng nhập phải có ít nhất 4 ký tự.");
            return false;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}