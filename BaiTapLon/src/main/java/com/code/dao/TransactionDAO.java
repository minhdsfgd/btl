package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionDAO — thao tác với bảng {@code transactions}.
 * Transaction là immutable — chỉ INSERT và SELECT.
 */
public class TransactionDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Ghi ──────────────────────────────────────────────────────────────────

    /**
     * Lưu giao dịch mới.
     * from_user_id = NULL nếu là DEPOSIT (fromUserId == -1).
     * auction_id   = NULL nếu không liên quan phiên (auctionId == -1).
     * ID được MySQL tự sinh.
     */
    public void save(Transaction t) throws SQLException {
        String sql = """
            INSERT INTO transactions
                (from_user_id, to_user_id, amount, auction_id, type, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // from_user_id: -1 trong model → NULL trong DB
            if (t.getFromUserId() == -1) ps.setNull(1, Types.INTEGER);
            else                         ps.setInt (1, t.getFromUserId());

            ps.setInt      (2, t.getToUserId());
            ps.setDouble   (3, t.getAmount());

            // auction_id: -1 trong model → NULL trong DB
            if (t.getAuctionId() == -1) ps.setNull(4, Types.INTEGER);
            else                        ps.setInt (4, t.getAuctionId());

            ps.setString   (5, t.getType().name());
            ps.setTimestamp(6, Timestamp.valueOf(t.getCreatedAt()));
            ps.executeUpdate();

            // Lấy ID vừa tạo từ DB
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1));
                }
            }
        }
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /** Tất cả giao dịch — Admin xem báo cáo. */
    public List<Transaction> findAll() throws SQLException {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Giao dịch của một user (cả gửi lẫn nhận) — màn hình "Lịch sử giao dịch". */
    public List<Transaction> findByUserId(int userId) throws SQLException {
        String sql = """
            SELECT * FROM transactions
            WHERE from_user_id = ? OR to_user_id = ?
            ORDER BY created_at DESC
            """;
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Giao dịch của một phiên — Admin kiểm tra thanh toán. */
    public List<Transaction> findByAuctionId(int auctionId) throws SQLException {
        String sql = """
            SELECT * FROM transactions
            WHERE auction_id = ?
            ORDER BY created_at ASC
            """;
        List<Transaction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Transaction mapRow(ResultSet rs) throws SQLException {
        // NULL trong DB → -1 trong model (convention)
        int fromUserId = -1;
        Object fuid = rs.getObject("from_user_id");
        if (fuid != null) fromUserId = (int) fuid;

        int auctionId = -1;
        Object aid = rs.getObject("auction_id");
        if (aid != null) auctionId = (int) aid;

        return new Transaction(
                rs.getInt      ("id"),
                fromUserId,
                rs.getInt      ("to_user_id"),
                rs.getDouble   ("amount"),
                auctionId,
                Transaction.Type.valueOf(rs.getString("type"))
        );
    }
}