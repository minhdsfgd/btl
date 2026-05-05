package com.code.service;

import com.code.exception.UserBannedException;
import com.code.models.User;

/**
 * Kiểm tra quyền truy cập trước khi xử lý bất kỳ request nào trên Server.
 *
 * <p>Gọi {@link #requireNotBanned(User)} ở đầu ClientHandler.run()
 * hoặc đầu RequestProcessor.process() trước mọi logic nghiệp vụ.</p>
 *
 * <pre>
 * // Trong ClientHandler.run():
 * while (true) {
 *     Request req = (Request) in.readObject();
 *     try {
 *         AuthGuard.requireNotBanned(currentUser);   // ← check ở đây
 *         Response res = RequestProcessor.process(req, currentUser);
 *         out.writeObject(res);
 *     } catch (UserBannedException e) {
 *         out.writeObject(new Response(false, e.getMessage(), null));
 *         break;   // đóng kết nối luôn
 *     }
 * }
 * </pre>
 */
public class AuthGuard {

    private AuthGuard() {}

    /**
     * Ném UserBannedException nếu user đang bị ban.
     * Gọi trước MỌI xử lý request.
     */
    public static void requireNotBanned(User user) throws UserBannedException {
        if (user != null && user.isBanned()) {
            throw new UserBannedException(user.getUsername());
        }
    }

    /**
     * Kiểm tra user có role cụ thể không.
     * Dùng để phân quyền các chức năng Admin/Seller.
     */
    public static void requireRole(User user,
                                   com.code.models.Role required)
            throws com.code.exception.InvalidBidException {
        if (user == null || !user.hasRole(required)) {
            throw new com.code.exception.InvalidBidException(
                    "Không có quyền thực hiện thao tác này. Cần quyền: " + required);
        }
    }
}