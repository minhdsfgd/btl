package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.Bid;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BidDAO — thao tác với bảng {@code bids}.
 * Bid là immutable — chỉ có INSERT và SELECT, không có UPDATE hay DELETE.
 */
public class BidDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Ghi ──────────────────────────────────────────────────────────────────

    /** Lưu bid mới. Gọi ngay sau BidService.placeBid() thành công. */
    public void save(Bid bid) throws SQLException {
        String sql = """
            INSERT INTO bids (id, auction_id, user_id, amount, bid_time)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt      (1, bid.getBidId());
            ps.setInt      (2, bid.getAuctionId());
            ps.setInt      (3, bid.getUserId());
            ps.setDouble   (4, bid.getAmount());
            ps.setTimestamp(5, Timestamp.valueOf(bid.getTimestamp()));
            ps.executeUpdate();
        }
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /** Tất cả bid của một phiên, sắp xếp theo thời gian. */
    public List<Bid> findByAuctionId(int auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bids WHERE auction_id = ?
            ORDER BY bid_time ASC
            """;
        List<Bid> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Lịch sử đặt giá của một user — màn hình "Lịch sử của tôi". */
    public List<Bid> findByUserId(int userId) throws SQLException {
        String sql = """
            SELECT * FROM bids WHERE user_id = ?
            ORDER BY bid_time DESC
            """;
        List<Bid> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Bid cao nhất của phiên — xác nhận lại người thắng. */
    public Optional<Bid> findHighestBid(int auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bids WHERE auction_id = ?
            ORDER BY amount DESC
            LIMIT 1
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Bid mapRow(ResultSet rs) throws SQLException {
        return new Bid(
                rs.getInt      ("id"),
                rs.getInt      ("auction_id"),
                rs.getInt      ("user_id"),
                rs.getDouble   ("amount"),
                rs.getTimestamp("bid_time").toLocalDateTime()
        );
    }
}