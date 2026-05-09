package com.code.controllers;

import com.code.models.Role;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class LoginController {
    private static final Map<String, String> USER_PASSWORDS = new HashMap<>();
    private static final Map<String, Set<Role>> USER_ROLES = new HashMap<>();
    private static String currentUsername = "";

    static {
        registerLocalUser("admin", "1234", Set.of(Role.ADMIN));
        registerLocalUser("seller", "1234", Set.of(Role.SELLER, Role.BIDDER));
        registerLocalUser("user", "1234", Set.of(Role.BIDDER));
    }

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink linkRegister;

    @FXML
    public void initialize() {
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

        if (!isValidCredentials(username, password)) {
            showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", "Sai tên đăng nhập hoặc mật khẩu.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }

        currentUsername = username;
        if (hasRole(username, Role.ADMIN)) navigateTo("/com/code/views/AdminPanel.fxml");
        else if (hasRole(username, Role.SELLER)) navigateTo("/com/code/views/SellerDashboard.fxml");
        else navigateTo("/com/code/views/AuctionList.fxml");
    }

    private boolean isValidCredentials(String username, String password) {
        String savedPassword = USER_PASSWORDS.get(username);
        return savedPassword != null && savedPassword.equals(password);
    }

    public static boolean registerLocalUser(String username, String password, Set<Role> roles) {
        if (USER_PASSWORDS.containsKey(username)) return false;
        USER_PASSWORDS.put(username, password);
        USER_ROLES.put(username, new HashSet<>(roles));
        return true;
    }

    public static String getCurrentUsername() {
        return currentUsername == null || currentUsername.isBlank() ? "username" : currentUsername;
    }

    public static boolean hasRole(String username, Role role) {
        Set<Role> roles = USER_ROLES.get(username);
        return roles != null && roles.contains(role);
    }

    public static boolean currentUserHasRole(Role role) {
        return hasRole(getCurrentUsername(), role);
    }

    public static void clearSession() {
        currentUsername = "";
    }
}