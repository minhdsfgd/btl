package com.code.server;

import com.code.dao.*;
import com.code.models.*;
import com.code.util.ItemType;
import com.code.util.ItemFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tạo dữ liệu mặc định khi server khởi động lần đầu.
 *
 * <p>Chạy trước khi server bắt đầu nhận kết nối.
 * Kiểm tra xem DB đã có dữ liệu chưa — nếu chưa thì tạo.</p>
 *
 * <p><b>Tạo:</b>
 * <ul>
 *   <li>1 tài khoản Admin mặc định (admin / admin123)</li>
 *   <li>2 tài khoản test (user1, user2)</li>
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
            // Luôn chạy fixPlaintextPasswords trước để fix mọi password plaintext còn sót
            fixPlaintextPasswords();

            // Kiểm tra đã có dữ liệu chưa — nếu rồi thì dừng
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

            // ── Tạo Regular User 1 test ──────────────────────────────────────
            RegularUser user1 = new RegularUser(
                    0, "user1", BCrypt.hashpw("user123", BCrypt.gensalt()), 10_000_000.0,
                    Role.SELLER, Role.BIDDER);
            userDAO.save(user1);
            System.out.println("[Seeder] ✓ Regular User 1: user1 / user123 (10 triệu VNĐ)");

            // ── Tạo Regular User 2 test ──────────────────────────────────────
            RegularUser user2 = new RegularUser(
                    0, "user2",  BCrypt.hashpw("user123", BCrypt.gensalt()), 10_000_000.0,
                    Role.SELLER, Role.BIDDER);
            userDAO.save(user2);
            System.out.println("[Seeder] ✓ Regular User 2: user2 / user123 (10 triệu VNĐ)");

            // ── Tạo sản phẩm mẫu ─────────────────────────────────────────────
            Electronics phone = new Electronics(
                    0, user1.getUserId(),
                    "iPhone 15 Pro Max",
                    "Điện thoại Apple mới nhất, 256GB",
                    20_000_000.0, "Apple", 12);
            itemDAO.save(phone);

            Art painting = new Art(
                    0, user1.getUserId(),
                    "Tranh Sơn Dầu Hoa Sen",
                    "Tác phẩm gốc, kích thước 60x80cm",
                    5_000_000.0, "Nguyễn Văn A", "Sơn dầu");
            itemDAO.save(painting);

            Vehicle car = new Vehicle(
                    0, user1.getUserId(),
                    "Toyota Camry 2022",
                    "Xe ít đi, còn mới 98%, đầy đủ giấy tờ",
                    850_000_000.0, "51G-12345", 2022);
            itemDAO.save(car);

            System.out.println("[Seeder] ✓ Đã tạo 3 sản phẩm mẫu");

            // ── Tạo phiên đấu giá đang chạy để test ngay ─────────────────────
            Auction liveAuction = new Auction(
                    0, phone, user1.getUserId(),
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

    /**
     * Duyệt toàn bộ users, phát hiện password KHÔNG phải BCrypt hash
     * (BCrypt hash luôn bắt đầu bằng "$2a$" hoặc "$2b$"),
     * rồi tự động hash lại và cập nhật DB.
     *
     * Giải quyết trường hợp schema.sql insert plaintext password vào DB trước.
     */
    private void fixPlaintextPasswords() {
        try {
            List<User> users = userDAO.findAll();
            int fixed = 0;
            for (User user : users) {
                String pwd = user.getPassword();
                // BCrypt hash hợp lệ luôn bắt đầu bằng "$2a$" hoặc "$2b$"
                if (pwd == null || (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$"))) {
                    String hashed = BCrypt.hashpw(pwd != null ? pwd : "", BCrypt.gensalt());
                    user.setPassword(hashed);
                    userDAO.update(user);
                    System.out.println("[Seeder] ✓ Đã hash password cho user: " + user.getUsername());
                    fixed++;
                }
            }
            if (fixed > 0) {
                System.out.println("[Seeder] Đã fix " + fixed + " password plaintext.");
            }
        } catch (Exception e) {
            System.err.println("[Seeder] Lỗi khi fix password: " + e.getMessage());
        }
    }
}