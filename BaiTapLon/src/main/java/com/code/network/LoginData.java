package com.code.network;
import com.code.models.Role;

import java.io.Serializable;

/**
 * Dùng cho: LOGIN, REGISTER, CREATE_ADMIN.
 *
 * <pre>
 * // Đăng nhập:
 * Request.of(LOGIN, new LoginData("alice", "pass123", null))
 *
 * // Đăng ký:
 * Request.of(REGISTER, new LoginData("bob", "pass456", Role.BIDDER))
 * </pre>
 */
public class LoginData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String username;
    public final String password;
    public final Role   primaryRole; // null khi LOGIN

    public LoginData(String username, String password, Role primaryRole) {
        this.username    = username;
        this.password    = password;
        this.primaryRole = primaryRole;
    }

    /** Constructor tiện ích cho LOGIN (không cần role). */
    public LoginData(String username, String password) {
        this(username, password, null);
    }
}