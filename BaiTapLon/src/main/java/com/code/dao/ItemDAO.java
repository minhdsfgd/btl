package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.*;
import com.code.util.ItemType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO — thao tác CRUD với bảng {@code items}.
 *
 * <p><b>Single Table Inheritance:</b> Electronics, Art, Vehicle đều
 * lưu chung một bảng. Field nào không thuộc subclass đó thì để NULL/default.
 * Khi load, dùng cột {@code item_type} để quyết định tạo class nào.</p>
 */
public class ItemDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /** Tìm Item theo ID. Trả về null nếu không tìm thấy. */
    public Item findById(int id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Tất cả sản phẩm — Admin quản lý. */
    public List<Item> findAll() throws SQLException {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Sản phẩm của một Seller — Seller dashboard. */
    public List<Item> findBySellerId(int sellerId) throws SQLException {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Ghi dữ liệu ──────────────────────────────────────────────────────────

    /** Lưu Item mới. ID được MySQL tự sinh. */
    public void save(Item item) throws SQLException {
        String sql = """
            INSERT INTO items
                (seller_id, name, description, starting_price, item_type,
                 brand, warranty_months, artist_name, medium, license_plate, year_made, image_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, item.getSellerId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getStartingPrice());
            ps.setString(5, item.getType().name());
            setSubclassFields(ps, item); // fill field riêng của subclass từ index 6
            ps.setString(12, item.getImageUrl());
            ps.executeUpdate();

            // Lấy ID vừa tạo từ DB
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setItemId(rs.getInt(1));
                }
            }
        }
    }

    /** Cập nhật Item đã tồn tại (tên, mô tả, giá, field riêng của subclass). */
    public void update(Item item) throws SQLException {
        String sql = """
            UPDATE items
            SET name = ?, description = ?, starting_price = ?,
                brand = ?, warranty_months = ?,
                artist_name = ?, medium = ?,
                license_plate = ?, year_made = ?,image_url = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());

            // Subclass fields — null nếu không phải loại đó
            if (item instanceof Electronics e) {
                ps.setString(4, e.getBrand());
                ps.setInt   (5, e.getWarrantyMonths());
            } else {
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.INTEGER);
            }

            if (item instanceof Art a) {
                ps.setString(6, a.getArtistName());
                ps.setString(7, a.getMedium());
            } else {
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.VARCHAR);
            }

            if (item instanceof Vehicle v) {
                ps.setString(8, v.getLicensePlate());
                ps.setInt   (9, v.getYearMade());
            } else {
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.INTEGER);
            }

            ps.setString(10, item.getImageUrl());
            ps.setInt(11, item.getItemId());
            ps.executeUpdate();
        }
    }

    /** Xóa Item theo ID. */
    public void delete(int itemId) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Chuyển ResultSet → Item subclass đúng loại.
     * Đọc item_type để quyết định tạo Electronics / Art / Vehicle.
     */
    Item mapRow(ResultSet rs) throws SQLException {
        int    id            = rs.getInt   ("id");
        int    sellerId      = rs.getInt   ("seller_id");
        String name          = rs.getString("name");
        String description   = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        String typeStr       = rs.getString("item_type");
        String imageUrl      = rs.getString("image_url"); // THÊM

        Item item = switch (ItemType.valueOf(typeStr)) {
            case ELECTRONICS -> new Electronics(
                    id, sellerId, name, description, startingPrice,
                    rs.getString("brand"),
                    rs.getInt   ("warranty_months")
            );
            case ART -> new Art(
                    id, sellerId, name, description, startingPrice,
                    rs.getString("artist_name"),
                    rs.getString("medium")
            );
            case VEHICLE -> new Vehicle(
                    id, sellerId, name, description, startingPrice,
                    rs.getString("license_plate"),
                    rs.getInt   ("year_made")
            );
        };

        item.setImageUrl(imageUrl); // THÊM
        return item;
    }

    /** Set field riêng theo subclass (dùng trong save). */
    private void setSubclassFields(PreparedStatement ps, Item item) throws SQLException {
        if (item instanceof Electronics e) {
            ps.setString(6,  e.getBrand());
            ps.setInt   (7,  e.getWarrantyMonths());
            ps.setString(8,  "Khuyết danh");
            ps.setString(9,  "");
            ps.setString(10, "");
            ps.setInt   (11, 0);
        } else if (item instanceof Art a) {
            ps.setString(6,  "");
            ps.setInt   (7,  0);
            ps.setString(8,  a.getArtistName());
            ps.setString(9,  a.getMedium());
            ps.setString(10, "");
            ps.setInt   (11, 0);
        } else if (item instanceof Vehicle v) {
            ps.setString(6,  "");
            ps.setInt   (7,  0);
            ps.setString(8,  "Khuyết danh");
            ps.setString(9,  "");
            ps.setString(10, v.getLicensePlate());
            ps.setInt   (11, v.getYearMade());
        }
    }
}