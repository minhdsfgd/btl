package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionDAO — thao tác CRUD với bảng {@code auctions}.
 *
 * <p><b>JOIN với items:</b> Mỗi Auction chứa một Item object.
 * AuctionDAO JOIN với bảng items khi load để tái tạo đầy đủ Auction.</p>
 *
 * <p><b>leadingBidderId:</b> Trong DB, cột này là NULL nếu chưa có bid.
 * Khi load, chuyển NULL → -1 (convention của model).</p>
 */
public class AuctionDAO {

    private final ItemDAO itemDAO = new ItemDAO();
    private final BidDAO bidDAO = new BidDAO();

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /** Tìm Auction theo ID. Trả về null nếu không tìm thấy. */
    public Auction findById(int id) throws SQLException {
        // JOIN lấy luôn thông tin item
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** OPEN + RUNNING + FINISHED + PAID — Bidder xem danh sách phiên công khai (bao gồm phiên đã kết thúc). */
    public List<Auction> findActiveAuctions() throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.status IN ('OPEN','RUNNING','FINISHED','PAID')
              AND a.banned = 0
            ORDER BY a.end_time ASC
            """;
        return queryList(sql);
    }

    /** Tất cả phiên — Admin quản lý, Scheduler dùng. */
    public List<Auction> findAll() throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            ORDER BY a.created_at DESC
            """;
        return queryList(sql);
    }

    /** Phiên của một Seller — Seller dashboard. */
    public List<Auction> findBySellerId(int sellerId) throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.seller_id = ?
            ORDER BY a.created_at DESC
            """;
        List<Auction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Ghi dữ liệu ──────────────────────────────────────────────────────────

    /** Lưu Auction mới (trạng thái OPEN). ID được MySQL tự sinh. */
    public void save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions
                (item_id, seller_id, current_price, bid_increment,
                 start_time, end_time, status, banned, leading_bidder_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt       (1, auction.getItem().getItemId());
            ps.setInt       (2, auction.getSellerId());
            ps.setDouble    (3, auction.getCurrentPrice());
            ps.setDouble    (4, auction.getBidIncrement());
            ps.setTimestamp (5, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp (6, Timestamp.valueOf(auction.getEndTime()));
            ps.setString    (7, auction.getStatus().name());
            ps.setBoolean   (8, auction.isBanned());
            setLeadingBidder(ps, 9, auction.getLeadingBidderId());
            ps.executeUpdate();

            // Lấy ID vừa tạo từ DB
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    auction.setAuctionId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Cập nhật trạng thái, giá hiện tại, người dẫn đầu, và cờ ban.
     * Gọi sau mỗi bid mới hoặc khi status thay đổi.
     */
    public void update(Auction auction) throws SQLException {
        String sql = """
            UPDATE auctions
            SET current_price = ?, status = ?, banned = ?,
                leading_bidder_id = ?, end_time = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble   (1, auction.getCurrentPrice());
            ps.setString   (2, auction.getStatus().name());
            ps.setBoolean  (3, auction.isBanned());
            setLeadingBidder(ps, 4, auction.getLeadingBidderId());
            ps.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            ps.setInt      (6, auction.getAuctionId());
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Chạy SQL và trả về List<Auction>. */
    private List<Auction> queryList(String sql) throws SQLException {
        List<Auction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Map ResultSet → Auction.
     *
     * Vì Auction.leadingBidderId và Auction.status là private và chỉ được
     * đặt qua recordBid()/updateStatus() trong runtime, khi load từ DB
     * cần dùng factory method Auction.loadFromDB() để bypass restriction này.
     */
    private Auction mapRow(ResultSet rs) throws SQLException {
        // ── Tái tạo Item từ JOIN columns ──────────────────────────────────────
        Item item = itemDAO.mapRow(rs); // dùng lại mapRow của ItemDAO

        // ── Đọc các cột của Auction ───────────────────────────────────────────
        int             auctionId    = rs.getInt      ("a.id");
        int             sellerId     = rs.getInt      ("a.seller_id");
        double          currentPrice = rs.getDouble   ("current_price");
        double          bidIncrement = rs.getDouble   ("bid_increment");
        LocalDateTime   startTime    = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime   endTime      = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus   status       = AuctionStatus.valueOf(rs.getString("status"));
        boolean         banned       = rs.getBoolean  ("banned");

        // leading_bidder_id có thể NULL → -1 (convention của model)
        int leadingBidderId = -1;
        Object lbId = rs.getObject("leading_bidder_id");
        if (lbId != null) leadingBidderId = (int) lbId;

        // ── Dùng factory method để tái tạo Auction từ DB ──────────────────────
        Auction auction = Auction.loadFromDB(
                auctionId, item, sellerId,
                currentPrice, bidIncrement,
                startTime, endTime,
                status, banned, leadingBidderId
        );

        // ── Tải danh sách bids cho phiên này ────────────────────────────────────
        try {
            List<Bid> bids = bidDAO.findByAuctionId(auctionId);
            for (Bid bid : bids) {
                auction.addBidToList(bid);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Lỗi tải bids cho phiên #" + auctionId + ": " + e.getMessage());
        }

        return auction;
    }

    /**
     * Set leading_bidder_id — NULL nếu chưa có bid (-1).
     */
    private void setLeadingBidder(PreparedStatement ps, int paramIndex, int id)
            throws SQLException {
        if (id == -1) ps.setNull(paramIndex, Types.INTEGER);
        else          ps.setInt (paramIndex, id);
    }
}