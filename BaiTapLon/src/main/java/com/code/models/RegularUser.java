package com.code.models;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

/**
 * Người dùng thông thường — có thể mang cả vai trò BIDDER lẫn SELLER.
 *
 * <p>Thay thế 2 class Bidder và Seller cũ. Lý do gộp:
 * một user thực tế vừa có thể đặt giá (BIDDER) vừa có thể
 * đăng sản phẩm (SELLER), và không được phép bid sản phẩm
 * của chính mình — kiểm tra ở BidService.</p>
 *
 * <pre>
 * // Tạo user chỉ bidder
 * RegularUser u1 = new RegularUser(1, "alice", "a@x.com", "pass", 500_000, Role.BIDDER);
 *
 * // Tạo user vừa bidder vừa seller
 * RegularUser u2 = new RegularUser(2, "bob", "b@x.com", "pass", 0,
 *                                  Role.BIDDER, Role.SELLER);
 *
 * // Thêm quyền seller về sau
 * u1.addRole(Role.SELLER);
 * </pre>
 */
public class RegularUser extends User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor chính — nhận một hoặc nhiều Role.
     *
     * @param primaryRole  Role bắt buộc (BIDDER hoặc SELLER)
     * @param extraRoles   Role bổ sung tuỳ chọn
     */
    public RegularUser(int userId, String username, String email,
                       String password, double balance,
                       Role primaryRole, Role... extraRoles) {
        super(userId, username, password, balance,
                buildRoles(primaryRole, extraRoles));
    }

    /**
     * Constructor load từ DB — nhận Set role có sẵn.
     */
    public RegularUser(int userId, String username, String email,
                       String password, double balance, Set<Role> roles) {
        super(userId, username, password, balance, roles);
    }

    private static Set<Role> buildRoles(Role primary, Role[] extra) {
        EnumSet<Role> set = EnumSet.of(primary);
        if (extra != null) {
            for (Role r : extra) {
                if (r != null) set.add(r);
            }
        }
        return set;
    }

    /** Tiện ích: user này có quyền đặt giá không? */
    public boolean canBid()  { return hasRole(Role.BIDDER); }

    /** Tiện ích: user này có quyền đăng sản phẩm không? */
    public boolean canSell() { return hasRole(Role.SELLER); }

    @Override
    public String toString() {
        return "RegularUser{id=" + getUserId()
                + ", username='" + getUsername() + "'"
                + ", roles=" + getRoles() + "}";
    }
}