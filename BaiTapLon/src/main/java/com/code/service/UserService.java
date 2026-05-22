package com.code.service;

import com.code.dao.UserDAO;
import com.code.exception.AuctionClosedException;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.*;
import java.util.concurrent.ConcurrentHashMap;

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
     * Đăng ký tài khoản mới. Mặc định gán cả BIDDER và SELLER.
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

            // Mọi tài khoản mới đều có BIDDER + SELLER
            RegularUser user = new RegularUser(
                    0, username, password, 0.0, Role.BIDDER, Role.SELLER
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
        User target = getUser(userId);
        if (target.hasRole(Role.ADMIN))
            throw new AuthenticationException("Không thể ban tài khoản Admin khác.");
        target.setActive(false);
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
        getUser(userId).setActive(true);
        try{
            userDAO.update(getUser(userId));
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
        getUser(userId).addRole(role);
    }

    /**
     * Admin xóa role của user.
     */
    public void removeRole(User admin, int userId, Role role)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        getUser(userId).removeRole(role);
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

    /**
     * Admin cập nhật thông tin user.
     * Các trường null sẽ bị bỏ qua (không cập nhật).
     */
    public void updateUser(User admin, int userId, String username, String password,
                          Double balance, Boolean active, String rolesStr)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        User target = getUser(userId);
        
        if (target.hasRole(Role.ADMIN) && !admin.equals(target))
            throw new AuthenticationException("Không thể sửa tài khoản Admin khác.");
        
        if (username != null && !username.isBlank())
            target.setUsername(username);
        
        if (password != null && password.length() >= 6)
            target.setPassword(password);
        
        if (balance != null && balance >= 0)
            target.setBalance(balance);
        
        if (active != null)
            target.setActive(active);
        
        if (rolesStr != null && !rolesStr.isBlank()) {
            target.getRoles().clear();
            for (String roleStr : rolesStr.split(",")) {
                try {
                    target.addRole(Role.valueOf(roleStr.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        try {
            userDAO.update(target);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Admin xóa user.
     */
    public void deleteUser(User admin, int userId)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        if (userId == admin.getUserId())
            throw new AuthenticationException("Không thể xóa chính mình.");
        User target = getUser(userId);
        if (target.hasRole(Role.ADMIN))
            throw new AuthenticationException("Không thể xóa tài khoản Admin.");
        
        try {
            userDAO.delete(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void requireAdmin(User admin)
            throws UserBannedException, AuthenticationException {
        if (!admin.isActive()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuthenticationException("Chỉ Admin được thực hiện thao tác này.");
    }

    public User getUser(int userId) {
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