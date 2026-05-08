package com.code.controllers;

import com.code.util.ControllerUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink linkRegister;

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
        if (authenticateUser(username, password)) {
            navigateTo("/com/code/views/AuctionList.fxml");
        }
        if (authenticateAdmin(username, password)) {
            navigateTo("/com/code/views/AdminPanel.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Sai thông tin", "Tên đăng nhập hoặc mật khẩu không đúng.");
            passwordField.clear();
            passwordField.requestFocus(); // focus lại ô mật khẩu
        }
    }

    private void handleRegister() {
        navigateTo("/com/code/views/Register.fxml");
    }

    private boolean authenticateUser(String username, String password){
        // TODO: the same as the one below
        return username.equals("user") && password.equals("1234");
    }
    private boolean authenticateAdmin(String username, String password) {
        // TODO: thay bằng truy vấn DB
        return username.equals("admin") && password.equals("1234");
    }
}
