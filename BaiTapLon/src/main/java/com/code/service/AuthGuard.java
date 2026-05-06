package com.code.service;

import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.Role;
import com.code.models.User;

import java.util.Objects;

/**
 * Kiểm tra quyền truy cập trước khi xử lý request trên Server.
 * Gọi ở đầu ClientHandler.run() trước mọi logic nghiệp vụ.
 */
public final class AuthGuard {

    private AuthGuard() {}

    /**
     * FIX: Ném exception nếu user null (anonymous request) HOẶC bị ban.
     * Trước đây: user=null được bỏ qua → anonymous request lọt qua.
     */
    public static void requireNotBanned(User user) throws UserBannedException {
        Objects.requireNonNull(user, "User chưa đăng nhập.");
        if (user.isBanned())
            throw new UserBannedException(user.getUsername());
    }

    /**
     * FIX: Ném AuthenticationException (không phải InvalidBidException).
     * Lỗi phân quyền không liên quan đến bidding.
     */
    public static void requireRole(User user, Role required)
            throws AuthenticationException, UserBannedException {
        requireNotBanned(user);
        if (!user.hasRole(required))
            throw new AuthenticationException(
                    "Không có quyền thực hiện thao tác này. Cần quyền: " + required);
    }

    /** Kiểm tra user là Admin (dùng nhiều nơi). */
    public static void requireAdmin(User user)
            throws AuthenticationException, UserBannedException {
        requireRole(user, Role.ADMIN);
    }

    /** Kiểm tra user có thể đặt giá (có role BIDDER). */
    public static void requireBidder(User user)
            throws AuthenticationException, UserBannedException {
        requireRole(user, Role.BIDDER);
    }

    /** Kiểm tra user có thể bán hàng (có role SELLER). */
    public static void requireSeller(User user)
            throws AuthenticationException, UserBannedException {
        requireRole(user, Role.SELLER);
    }
}