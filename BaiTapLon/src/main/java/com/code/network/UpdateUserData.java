package com.code.network;

import java.io.Serializable;

/**
 * Dùng cho: UPDATE_USER.
 * Chứa thông tin cần update cho user.
 *
 * <pre>
 * Request.of(UPDATE_USER, new UpdateUserData(
 *     userId, "newUsername", "newPassword", 5000.0, true, "BIDDER,SELLER"
 * ))
 * </pre>
 */
public class UpdateUserData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int     userId;
    public final String  username;
    public final String  password;
    public final Double  balance;  // null = không thay đổi
    public final Boolean active;   // null = không thay đổi
    public final String  roles;    // "BIDDER,SELLER" or null = không thay đổi

    public UpdateUserData(int userId, String username, String password,
                          Double balance, Boolean active, String roles) {
        this.userId    = userId;
        this.username  = username;
        this.password  = password;
        this.balance   = balance;
        this.active    = active;
        this.roles     = roles;
    }
}
