package com.code.service;

import com.code.dao.AuditLogDAO;
import com.code.dao.UserDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.*;
import org.mindrot.jbcrypt.BCrypt;

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
    private final AuditLogService auditLogService;

    public UserService(UserDAO userDAO, AuditLogDAO auditLogDAO) {
        this.userDAO = userDAO;
        this.auditLogService=new  AuditLogService(auditLogDAO);

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
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            RegularUser user = new RegularUser(
                    0, username, hashed, 0.0, Role.BIDDER, Role.SELLER
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
        try {
            if (userDAO.existsByUsername(username))
                throw new AuthenticationException("Username '" + username + "' đã tồn tại.");

            Admin admin = new Admin(0, username, BCrypt.hashpw(password, BCrypt.gensalt()), 0.0);
            userDAO.save(admin);

            // ✨ LOG: Tạo Admin mới
            auditLogService.logAction(
                    currentAdmin.getUserId(),
                    admin.getUserId(),
                    AuditLog.ActionType.CREATE_ADMIN,
                    null,
                    admin.getUsername()
            );

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
            if (user == null ||  !BCrypt.checkpw(password, user.getPassword()))
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

        boolean wasActive = target.isActive();
        target.setActive(false);

        try {
            userDAO.update(target);

            // ✨ LOG với thông tin rõ ràng
            auditLogService.logFieldChange(
                    admin.getUserId(),
                    userId,
                    AuditLog.ActionType.BAN_USER,
                    "active=" + wasActive,
                    "active=false"
            );
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

        User target = getUser(userId);
        if (target.hasRole(Role.ADMIN))
            throw new AuthenticationException("Không thể gỡ ban tài khoản Admin khác.");

        boolean wasActive = target.isActive();
        target.setActive(true);

        try {
            userDAO.update(target);

            // ✨ FIX: Dùng UNBAN_USER thay vì BAN_USER
            auditLogService.logFieldChange(
                    admin.getUserId(),
                    userId,
                    AuditLog.ActionType.UNBAN_USER,
                    "active=" + wasActive,
                    "active=true"
            );
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

        User target = getUser(userId);
        target.addRole(role);

        try {
            userDAO.update(target);

            auditLogService.logAction(
                    admin.getUserId(),
                    userId,
                    AuditLog.ActionType.ADD_ROLE,
                    null,
                    role.name()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Admin xóa role của user.
     */
    public void removeRole(User admin, int userId, Role role)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);

        User target = getUser(userId);
        target.removeRole(role);

        try {
            userDAO.update(target);

            auditLogService.logAction(
                    admin.getUserId(),
                    userId,
                    AuditLog.ActionType.REMOVE_ROLE,
                    role.name(),
                    null
            );
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Admin xem tất cả user.
     */
    public List<User> getAllUsers(User admin)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        try {
            return userDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Admin cập nhật thông tin user.
     * Các trường null sẽ bị bỏ qua (không cập nhật).
     */
    public void updateUser(User admin, int userId,
                           String newUsername, String newPassword,
                           Double newBalance, Boolean newActive, String rolesStr)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);

        User target = getUser(userId);

        if (target.hasRole(Role.ADMIN) && !admin.equals(target))
            throw new AuthenticationException("Không thể sửa tài khoản Admin khác.");

        try {
            // ✨ Log từng field thay đổi
            if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(target.getUsername())) {
                String oldUsername = target.getUsername();
                target.setUsername(newUsername);
                auditLogService.logFieldChange(
                        admin.getUserId(),
                        userId,
                        AuditLog.ActionType.UPDATE_USERNAME,
                        oldUsername,
                        newUsername
                );
            }

            if (newPassword != null && newPassword.length() >= 6) {
                target.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                auditLogService.logAction(
                        admin.getUserId(),
                        userId,
                        AuditLog.ActionType.UPDATE_PASSWORD,
                        "[HIDDEN]",  // Không log password thực tế
                        "[CHANGED]"
                );
            }

            if (newBalance != null && newBalance >= 0 && newBalance != target.getBalance()) {
                double oldBalance = target.getBalance();
                target.setBalance(newBalance);
                auditLogService.logFieldChange(
                        admin.getUserId(),
                        userId,
                        AuditLog.ActionType.UPDATE_BALANCE,
                        String.valueOf(oldBalance),
                        String.valueOf(newBalance)
                );
            }

            if (newActive != null && newActive != target.isActive()) {
                boolean oldActive = target.isActive();
                target.setActive(newActive);
                auditLogService.logFieldChange(
                        admin.getUserId(),
                        userId,
                        AuditLog.ActionType.UPDATE_STATUS,
                        "active=" + oldActive,
                        "active=" + newActive
                );
            }

            // Update roles
            if (rolesStr != null && !rolesStr.isBlank()) {
                target.getRoles().clear();
                for (String roleStr : rolesStr.split(",")) {
                    try {
                        target.addRole(Role.valueOf(roleStr.trim()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

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

            auditLogService.logAction(
                    admin.getUserId(),
                    userId,
                    AuditLog.ActionType.DELETE_USER,
                    target.getUsername(),
                    null
            );
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

    private User getUser(int userId) throws AuthenticationException {
        try {
            User u = userDAO.findById(userId);
            if (u == null)
                throw new AuthenticationException("Không tìm thấy user #" + userId);
            return u;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }
    public List<AuditLog> getUserAuditLogs(User admin, int targetUserId)
            throws AuthenticationException, UserBannedException {
        requireAdmin(admin);
        try {
            return auditLogService.getUserLogs(targetUserId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }
}