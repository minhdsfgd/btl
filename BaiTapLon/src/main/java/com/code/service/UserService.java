package com.code.service;

import com.code.dao.UserDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Xử lý nghiệp vụ liên quan đến tài khoản người dùng.
 *
 * <p><b>Vai trò:</b>
 * <ul>
 *   <li>Đăng ký, đăng nhập</li>
 *   <li>Admin: ban/unban, thêm/xóa quyền</li>
 *   <li>Kiểm tra tài khoản active/banned trước thao tác</li>
 * </ul>
 * </p>
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // ── Đăng ký ──────────────────────────────────────────────────────────────

    /**
     * Đăng ký tài khoản mới với role BIDDER mặc định.
     *
     * @param username  tên đăng nhập (không được trùng)
     * @param password  mật khẩu (nên hash trước khi truyền vào)
     * @param primaryRole role chính: BIDDER hoặc SELLER
     * @return RegularUser vừa tạo
     * @throws AuthenticationException nếu username đã tồn tại
     */
    public RegularUser register(String username, String password, Role primaryRole)
            throws AuthenticationException {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được để trống.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password phải ít nhất 6 ký tự.");

        // FIX: Bảo mật — không cho tự đăng ký thành Admin
        if (primaryRole == Role.ADMIN)
            throw new AuthenticationException(
                    "Không thể tự đăng ký tài khoản Admin. Liên hệ quản trị viên.");

        try{
            if (userDAO.existsByUsername(username))
                throw new AuthenticationException("Username '" + username + "' đã tồn tại.");

            RegularUser user = new RegularUser(
                    0, username, password, 0.0, primaryRole
            );
            userDAO.save(user);
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Chỉ Admin hiện tại mới được tạo tài khoản Admin mới.
     */
    public Admin createAdmin(User currentAdmin, String username, String password)
            throws AuthenticationException, UserBannedException {
        requireAdmin(currentAdmin);
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được để trống.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password phải ít nhất 6 ký tự.");
        try{
            if (userDAO.existsByUsername(username))
                throw new AuthenticationException("Username '" + username + "' đã tồn tại.");

            Admin admin = new Admin(0, username, password, 0.0);
            userDAO.save(admin);
            return admin;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    // ── Đăng nhập ────────────────────────────────────────────────────────────

    /**
     * Đăng nhập — trả về User nếu thành công.
     *
     * @throws AuthenticationException nếu sai thông tin hoặc tài khoản bị ban/inactive
     */
    public User login(String username, String password)
            throws AuthenticationException, UserBannedException {
        try{
            User user =userDAO.findByUsername(username);
            // FIX: cùng message để tránh timing attack (không lộ username có tồn tại không)
            if (user == null || !user.getPassword().equals(password))
                throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");

            if (!user.isActive())
                throw new AuthenticationException("Tài khoản đã bị vô hiệu hóa.");

            if (user.isBanned())
                throw new UserBannedException(username);

            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    // ── Admin: quản lý tài khoản ─────────────────────────────────────────────

    /**
     * Admin ban tài khoản vi phạm.
     *
     * @param admin  người thực hiện — phải có role ADMIN
     * @param userId ID tài khoản cần ban
     */
    public void banUser(User admin, int userId)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        User target = getOrThrow(userId);
        if (target.hasRole(Role.ADMIN))
            throw new AuthenticationException("Không thể ban tài khoản Admin khác.");
        target.setBanned(true);
        try{
            userDAO.update(target);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }


    }

    /**
     * Admin gỡ ban tài khoản.
     */
    public void unbanUser(User admin, int userId)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        getOrThrow(userId).setBanned(false);
        try{
            userDAO.update(getOrThrow(userId));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Admin thêm role cho user (vd: nâng Bidder thành Seller).
     */
    public void addRole(User admin, int userId, Role role)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        if (role == Role.ADMIN)
            throw new AuthenticationException("Dùng createAdmin() để tạo Admin mới.");
        getOrThrow(userId).addRole(role);
    }

    /**
     * Admin xóa role của user.
     */
    public void removeRole(User admin, int userId, Role role)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        getOrThrow(userId).removeRole(role);
    }

    /**
     * Admin xem tất cả user.
     */
    public List<User> getAllUsers(User admin)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        try{
            return userDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void requireAdmin(User admin)
            throws UserBannedException, AuthenticationException {
        if (admin.isBanned()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuthenticationException("Chỉ Admin được thực hiện thao tác này.");
    }

    private User getOrThrow(int userId) {
        try{
            User u = userDAO.findById(userId);
            if (u == null)
                throw new IllegalArgumentException("Không tìm thấy user #" + userId);
            return u;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }


    }
}