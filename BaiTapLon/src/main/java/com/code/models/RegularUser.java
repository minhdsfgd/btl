package com.code.models;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Người dùng thông thường — có thể mang cả vai trò BIDDER lẫn SELLER.
 * Thay thế 2 class Bidder và Seller cũ.
 *
 * <pre>
 * // Chỉ bidder
 * new RegularUser(1, "alice", "pass", 500_000, Role.BIDDER);
 *
 * // Vừa bidder vừa seller
 * new RegularUser(2, "bob", "pass", 0, Role.BIDDER, Role.SELLER);
 * </pre>
 */
public class RegularUser extends User implements Serializable {
    private static final long serialVersionUID = 1L;

    public RegularUser(int userId, String username, String password,
                       double balance, Role primaryRole, Role... extraRoles) {
        super(userId, username, password, balance, buildRoles(primaryRole, extraRoles));
    }

    /** Constructor load từ DB — nhận Set role có sẵn. */
    public RegularUser(int userId, String username, String password,
                       double balance, Set<Role> roles) {
        super(userId, username, password, balance, roles);
    }

    private static Set<Role> buildRoles(Role primary, Role[] extra) {
        EnumSet<Role> set = EnumSet.of(primary);
        if (extra != null) for (Role r : extra) if (r != null) set.add(r);
        return set;
    }

    public boolean canBid()  { return hasRole(Role.BIDDER); }
    public boolean canSell() { return hasRole(Role.SELLER); }

    @Override
    public String toString() {
        return "RegularUser{id=" + getUserId() + ", username='" + getUsername()
                + "', roles=" + getRoles() + "}";
    }
}