package com.code.network;

import com.code.models.Role;

import java.io.Serializable;

/**
 * Dùng cho: ADD_ROLE, REMOVE_ROLE.
 *
 * <pre>
 * Request.of(ADD_ROLE, new RoleChangeData(userId, Role.SELLER))
 * </pre>
 */
public class RoleChangeData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int  userId;
    public final Role role;

    public RoleChangeData(int userId, Role role) {
        this.userId = userId;
        this.role   = role;
    }
}