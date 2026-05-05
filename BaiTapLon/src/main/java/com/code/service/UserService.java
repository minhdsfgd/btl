package com.code.service;

import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.*;
import com.code.repository.UserRepository;
import com.code.util.IdGenerator;

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

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (userRepository.existsByUsername(username))
            throw new AuthenticationException("Username '" + username + "' đã tồn tại.");

        RegularUser user = new RegularUser(
                IdGenerator.getId(), username, password, 0.0, primaryRole
        );
        userRepository.save(user);
        return user;
    }

    // ── Đăng nhập ────────────────────────────────────────────────────────────

    /**
     * Đăng nhập — trả về User nếu thành công.
     *
     * @throws AuthenticationException nếu sai thông tin hoặc tài khoản bị ban/inactive
     */
    public User login(String username, String password)
            throws AuthenticationException, UserBannedException {
        User user = userRepository.findByUsername(username);

        if (user == null || !user.getPassword().equals(password))
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu.");

        if (!user.isActive())
            throw new AuthenticationException("Tài khoản đã bị vô hiệu hóa.");

        if (user.isBanned())
            throw new UserBannedException(username);

        return user;
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
        target.setBanned(true);
        userRepository.update(target);
    }

    /**
     * Admin gỡ ban tài khoản.
     */
    public void unbanUser(User admin, int userId)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
        User target = getOrThrow(userId);
        target.setBanned(false);
        userRepository.update(target);
    }

    /**
     * Admin thêm role cho user (vd: nâng Bidder thành Seller).
     */
    public void addRole(User admin, int userId, Role role)
            throws UserBannedException, AuthenticationException {
        requireAdmin(admin);
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
        return userRepository.findAll();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void requireAdmin(User admin)
            throws UserBannedException, AuthenticationException {
        if (admin.isBanned()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuthenticationException("Chỉ Admin được thực hiện thao tác này.");
    }

    private User getOrThrow(int userId) {
        User u = userRepository.findById(userId);
        if (u == null)
            throw new IllegalArgumentException("Không tìm thấy user #" + userId);
        return u;
    }
}