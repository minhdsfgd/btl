package com.code.models;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Quản trị viên hệ thống — luôn có Role.ADMIN.
 * Chỉ được tạo nội bộ (seed data hoặc Admin tạo Admin).
 * KHÔNG thể đăng ký qua UserService.register().
 */
public class Admin extends User implements Serializable {
    private static final long serialVersionUID = 1L;

    public Admin(int userId, String username, String password, double balance) {
        super(userId, username, password, balance, EnumSet.of(Role.ADMIN));
    }

    /** Admin có thêm role SELLER/BIDDER nếu cần. */
    public Admin(int userId, String username, String password,
                 double balance, Set<Role> extraRoles) {
        super(userId, username, password, balance, mergeRoles(Role.ADMIN, extraRoles));
    }

    @Override
    public String toString() {
        return "Admin{id=" + getUserId() + ", username='" + getUsername() + "'}";
    }
}