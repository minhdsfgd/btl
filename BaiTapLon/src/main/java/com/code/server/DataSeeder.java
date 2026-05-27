package com.code.server;

import com.code.dao.*;
import com.code.models.*;
import com.code.util.ItemType;
import com.code.util.ItemFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

/**
 * Tạo dữ liệu mặc định khi server khởi động lần đầu.
 *
 * <p>Chạy trước khi server bắt đầu nhận kết nối.
 * Kiểm tra xem DB đã có dữ liệu chưa — nếu chưa thì tạo.</p>
 *
 * <p><b>Tạo:</b>
 * <ul>
 *   <li>1 tài khoản Admin mặc định (admin / admin123)</li>
 *   <li>2 tài khoản test (seller1, bidder1)</li>
 *   <li>3 sản phẩm mẫu (Electronics, Art, Vehicle)</li>
 *   <li>1 phiên đấu giá đang RUNNING để test ngay</li>
 * </ul>
 * </p>
 */
public class DataSeeder {

    private final UserDAO    userDAO;
    private final ItemDAO    itemDAO;
    private final AuctionDAO auctionDAO;

    public DataSeeder(UserDAO userDAO, ItemDAO itemDAO, AuctionDAO auctionDAO) {
        this.userDAO    = userDAO;
        this.itemDAO    = itemDAO;
        this.auctionDAO = auctionDAO;
    }

    public void seed() {
        try {
            // Kiểm tra đã có dữ liệu chưa
            if (!userDAO.findAll().isEmpty()) {
                System.out.println("[Seeder] DB đã có dữ liệu, bỏ qua seed.");
                return;
            }

            System.out.println("[Seeder] Tạo dữ liệu mặc định...");

            // ── Tạo Admin ────────────────────────────────────────────────────
            Admin admin = new Admin(
                    0, "admin",  BCrypt.hashpw("admin123", BCrypt.gensalt()), 0.0);
            userDAO.save(admin);
            System.out.println("[Seeder] ✓ Admin: admin / admin123");

            // ── Tạo Seller test ──────────────────────────────────────────────
            RegularUser seller = new RegularUser(
                    0, "seller1", BCrypt.hashpw("seller123", BCrypt.gensalt()), 0.0,
                    Role.SELLER);
            userDAO.save(seller);
            System.out.println("[Seeder] ✓ Seller: seller1 / seller123");

            // ── Tạo Bidder test ──────────────────────────────────────────────
            RegularUser bidder = new RegularUser(
                    0, "bidder1",  BCrypt.hashpw("bidder123", BCrypt.gensalt()), 10_000_000.0,
                    Role.BIDDER);
            userDAO.save(bidder);
            System.out.println("[Seeder] ✓ Bidder: bidder1 / bidder123 (10 triệu VNĐ)");

            // ── Tạo sản phẩm mẫu ─────────────────────────────────────────────
            Electronics phone = new Electronics(
                    0, seller.getUserId(),
                    "iPhone 15 Pro Max",
                    "Điện thoại Apple mới nhất, 256GB",
                    20_000_000.0, "Apple", 12);
            itemDAO.save(phone);

            Art painting = new Art(
                    0, seller.getUserId(),
                    "Tranh Sơn Dầu Hoa Sen",
                    "Tác phẩm gốc, kích thước 60x80cm",
                    5_000_000.0, "Nguyễn Văn A", "Sơn dầu");
            itemDAO.save(painting);

            Vehicle car = new Vehicle(
                    0, seller.getUserId(),
                    "Toyota Camry 2022",
                    "Xe ít đi, còn mới 98%, đầy đủ giấy tờ",
                    850_000_000.0, "51G-12345", 2022);
            itemDAO.save(car);

            System.out.println("[Seeder] ✓ Đã tạo 3 sản phẩm mẫu");

            // ── Tạo phiên đấu giá đang chạy để test ngay ─────────────────────
            Auction liveAuction = new Auction(
                    0, phone, seller.getUserId(),
                    phone.getStartingPrice(),
                    500_000.0,                            // bước giá 500k
                    LocalDateTime.now().minusMinutes(5),  // đã bắt đầu 5 phút trước
                    LocalDateTime.now().plusHours(2)      // còn 2 tiếng
            );
            liveAuction.updateStatus(AuctionStatus.RUNNING);
            auctionDAO.save(liveAuction);

            System.out.println("[Seeder] ✓ Phiên đấu giá RUNNING: iPhone 15 Pro Max");
            System.out.println("[Seeder] ✓ Seed hoàn tất!\n");

        } catch (Exception e) {
            System.err.println("[Seeder] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}