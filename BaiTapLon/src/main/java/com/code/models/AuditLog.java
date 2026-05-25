package com.code.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lưu lịch sử thay đổi mà admin thực hiện với user.
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;

    private int adminId;
    private int targetUserId;

    /**
     * Ví dụ:
     * BAN_USER
     * UNBAN_USER
     * CHANGE_ROLE
     * UPDATE_BALANCE
     * DELETE_USER
     * RESET_PASSWORD
     */
    private String action;

    /**
     * Giá trị cũ trước khi sửa.
     */
    private String oldValue;

    /**
     * Giá trị mới sau khi sửa.
     */
    private String newValue;

    /**
     * Lý do admin thực hiện.
     */

    private LocalDateTime createdAt;

    // =========================
    // Constructors
    // =========================

    public AuditLog() {
    }

    public AuditLog(int adminId,
                    int targetUserId,
                    String action,
                    String oldValue,
                    String newValue) {
        this.adminId = adminId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = LocalDateTime.now();
    }

    // =========================
    // Getters & Setters
    // =========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(int targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public  void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
    public String getActionDescription() {
        try {
            return ActionType.valueOf(action).getDescription();
        } catch (IllegalArgumentException e) {
            return action;  // Fallback nếu không tìm thấy enum
        }
    }
    @Override
    public String toString() {
        return String.format(
                "AuditLog{id=%d, admin=%d, target=%d, action=%s, old=%s, new=%s, at=%s}",
                id, adminId, targetUserId, action, oldValue, newValue, createdAt
        );
    }

    /**
     * Định nghĩa tất cả loại hành động admin có thể log.
     * Sử dụng enum để tránh typo và dễ dàng refactor.
     */
    public enum ActionType {
        // User Management
        BAN_USER("Cấm người dùng"),
        UNBAN_USER("Gỡ cấm người dùng"),
        ADD_ROLE("Thêm vai trò"),
        REMOVE_ROLE("Xóa vai trò"),
        UPDATE_USERNAME("Cập nhật tên đăng nhập"),
        UPDATE_PASSWORD("Đổi mật khẩu"),
        UPDATE_BALANCE("Cập nhật số dư"),
        UPDATE_STATUS("Cập nhật trạng thái"),
        DELETE_USER("Xóa người dùng"),
        CREATE_ADMIN("Tạo tài khoản Admin"),
        CREATE_USER("Tạo tài khoản người dùng");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}