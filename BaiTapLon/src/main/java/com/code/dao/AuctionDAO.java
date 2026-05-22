package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private final ItemDAO itemDAO = new ItemDAO();
    private final BidDAO  bidDAO  = new BidDAO();

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    public Auction findById(int id) throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made,
                   i.image_url
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

    public List<Auction> findActiveAuctions() throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made,
                   i.image_url
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.status IN ('OPEN','RUNNING','FINISHED','PAID')
              AND a.banned = 0
            ORDER BY a.end_time ASC
            """;
        return queryList(sql);
    }

    public List<Auction> findAll() throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made,
                   i.image_url
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            ORDER BY a.created_at DESC
            """;
        return queryList(sql);
    }

    public List<Auction> findBySellerId(int sellerId) throws SQLException {
        String sql = """
            SELECT a.*, i.seller_id AS item_seller_id, i.name, i.description,
                   i.starting_price, i.item_type,
                   i.brand, i.warranty_months,
                   i.artist_name, i.medium,
                   i.license_plate, i.year_made,
                   i.image_url
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

    public void save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions
                (item_id, seller_id, current_price, bid_increment,
                 start_time, end_time, status, banned, leading_bidder_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt      (1, auction.getItem().getItemId());
            ps.setInt      (2, auction.getSellerId());
            ps.setDouble   (3, auction.getCurrentPrice());
            ps.setDouble   (4, auction.getBidIncrement());
            ps.setTimestamp(5, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(auction.getEndTime()));
            ps.setString   (7, auction.getStatus().name());
            ps.setBoolean  (8, auction.isBanned());
            setLeadingBidder(ps, 9, auction.getLeadingBidderId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) auction.setAuctionId(rs.getInt(1));
            }
        }
    }

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

    private List<Auction> queryList(String sql) throws SQLException {
        List<Auction> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        Item item = itemDAO.mapRow(rs);

        int           auctionId    = rs.getInt      ("a.id");
        int           sellerId     = rs.getInt      ("a.seller_id");
        double        currentPrice = rs.getDouble   ("current_price");
        double        bidIncrement = rs.getDouble   ("bid_increment");
        LocalDateTime startTime    = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime      = rs.getTimestamp("end_time").toLocalDateTime();
        AuctionStatus status       = AuctionStatus.valueOf(rs.getString("status"));
        boolean       banned       = rs.getBoolean  ("banned");

        int leadingBidderId = -1;
        Object lbId = rs.getObject("leading_bidder_id");
        if (lbId != null) leadingBidderId = (int) lbId;

        Auction auction = Auction.loadFromDB(
                auctionId, item, sellerId,
                currentPrice, bidIncrement,
                startTime, endTime,
                status, banned, leadingBidderId
        );

        try {
            List<Bid> bids = bidDAO.findByAuctionId(auctionId);
            for (Bid bid : bids) auction.addBidToList(bid);
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Loi tai bids cho phien #"
                    + auctionId + ": " + e.getMessage());
        }

        return auction;
    }

    private void setLeadingBidder(PreparedStatement ps, int paramIndex, int id)
            throws SQLException {
        if (id == -1) ps.setNull(paramIndex, Types.INTEGER);
        else          ps.setInt (paramIndex, id);
    }

    public boolean isItemLocked(int itemId) throws SQLException {
        String sql = """
            SELECT 1 FROM auctions
            WHERE item_id = ?
              AND status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID')
            LIMIT 1
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
