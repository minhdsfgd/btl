package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.*;

import java.sql.*;
import java.util.*;

/**
 * UserDAO — thao tác CRUD với bảng {@code users}.
 *
 * <p><b>Mapping roles:</b> DB lưu chuỗi "BIDDER,SELLER".
 * DAO tự chuyển sang {@code EnumSet<Role>} khi load, và ngược lại khi save.</p>
 *
 * <p><b>Phân biệt Admin / RegularUser:</b>
 * Nếu roles chứa "ADMIN" → tạo {@link Admin}, còn lại → tạo {@link RegularUser}.</p>
 */
public class UserDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /** Tìm user theo ID. Trả về null nếu không tìm thấy. */
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Tìm user theo username. Trả về null nếu không tìm thấy. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Kiểm tra username đã tồn tại chưa. */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Lấy tất cả user — Admin quản lý. */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Ghi dữ liệu ──────────────────────────────────────────────────────────

    /**
     * Lưu user mới vào DB. ID được MySQL tự sinh (AUTO_INCREMENT).
     * Sau khi save, KHÔNG cần gọi lại — id đã được gán từ IdGenerator.
     */
    public void save(User user) throws SQLException {
        String sql = """
            INSERT INTO users (id, username, password, balance, active, banned, roles)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt   (1, user.getUserId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setDouble(4, user.getBalance());
            ps.setBoolean(5, user.isActive());
            ps.setBoolean(6, user.isBanned());
            ps.setString(7, rolesToString(user.getRoles()));
            ps.executeUpdate();
        }
    }

    /**
     * Cập nhật thông tin user (balance, banned, active, roles, password).
     * ID không được thay đổi.
     */
    public void update(User user) throws SQLException {
        String sql = """
            UPDATE users
            SET username = ?, password = ?, balance = ?,
                active = ?, banned = ?, roles = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString (1, user.getUsername());
            ps.setString (2, user.getPassword());
            ps.setDouble (3, user.getBalance());
            ps.setBoolean(4, user.isActive());
            ps.setBoolean(5, user.isBanned());
            ps.setString (6, rolesToString(user.getRoles()));
            ps.setInt    (7, user.getUserId());
            ps.executeUpdate();
        }
    }

    /** Xóa user theo ID. */
    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chuyển ResultSet → User object.
     * Nếu roles chứa ADMIN → Admin, còn lại → RegularUser.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        int     id       = rs.getInt    ("id");
        String  username = rs.getString ("username");
        String  password = rs.getString ("password");
        double  balance  = rs.getDouble ("balance");
        boolean active   = rs.getBoolean("active");
        boolean banned   = rs.getBoolean("banned");
        Set<Role> roles  = stringToRoles(rs.getString("roles"));

        User user;
        if (roles.contains(Role.ADMIN)) {
            user = new Admin(id, username, password, balance, roles);
        } else {
            user = new RegularUser(id, username, password, balance, roles);
        }
        user.setActive(active);
        user.setBanned(banned);
        // setBalance dùng protected — DAO được phép gọi vì load từ DB
        user.setBalance(balance);
        return user;
    }

    /** "BIDDER,SELLER" → EnumSet{BIDDER, SELLER} */
    private Set<Role> stringToRoles(String rolesStr) {
        Set<Role> set = EnumSet.noneOf(Role.class);
        if (rolesStr == null || rolesStr.isBlank()) return set;
        for (String r : rolesStr.split(",")) {
            try { set.add(Role.valueOf(r.trim())); }
            catch (IllegalArgumentException ignored) {}
        }
        return set;
    }

    /** EnumSet{BIDDER, SELLER} → "BIDDER,SELLER" */
    private String rolesToString(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Role r : roles) {
            if (sb.length() > 0) sb.append(',');
            sb.append(r.name());
        }
        return sb.toString();
    }
}