package com.code.client;

import com.code.models.Role;
import com.code.models.User;

/**
 * Lưu trữ thông tin user đang đăng nhập — dùng trên toàn app (không lưu password).
 * Thread-safe nhờ volatile.
 */
public class SessionManager {

    private static volatile User currentUser;

    private SessionManager() {}

    public static void setUser(User user) {
        currentUser = user;
    }

    public static User getUser() {
        return currentUser;
    }

    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "unknown";
    }

    public static int getUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.hasRole(role);
    }

    public static void clear() {
        currentUser = null;
    }
}