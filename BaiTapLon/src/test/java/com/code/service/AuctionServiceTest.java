package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.UserDAO;
import com.code.exception.AuctionClosedException;
import com.code.models.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionServiceTest {

    private AuctionDAO mockAuctionDAO;
    private UserDAO mockUserDAO;
    private TransactionService mockTxService;
    private AuctionService auctionService;

    private RegularUser seller;
    private RegularUser bidder;
    private Admin admin;
    private Electronics item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() throws Exception {
        mockAuctionDAO = Mockito.mock(AuctionDAO.class);
        mockUserDAO = Mockito.mock(UserDAO.class);
        mockTxService = Mockito.mock(TransactionService.class);

        // Reset Singleton instance
        Field instanceField = AuctionService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        AuctionService.init(mockAuctionDAO, mockUserDAO, mockTxService);
        auctionService = AuctionService.getInstance();

        seller = new RegularUser(1, "seller", "pass", 100_000, Role.SELLER);
        bidder = new RegularUser(2, "bidder", "pass", 1_000_000, Role.BIDDER);
        admin = new Admin(99, "admin", "pass", 0);

        item = new Electronics(10, seller.getUserId(), "Laptop", "Gaming", 15000);

        startTime = LocalDateTime.now().plusMinutes(5);
        endTime = startTime.plusHours(1);
    }

    @AfterEach
    void tearDown() {
        if (auctionService != null) {
            auctionService.shutdown();
        }
    }

    @Nested
    @DisplayName("Tests Tạo phiên đấu giá (createAuction)")
    class CreateAuctionTests {

        @Test
        @DisplayName("Tạo phiên thành công")
        void testCreateAuction_Success() throws Exception {
            when(mockAuctionDAO.isItemLocked(item.getItemId())).thenReturn(false);

            Auction created = auctionService.createAuction(item, seller, 500, startTime, endTime);

            assertNotNull(created);
            assertEquals(1, created.getSellerId());
            assertEquals(AuctionStatus.OPEN, created.getStatus());
            verify(mockAuctionDAO, times(1)).save(any(Auction.class));
        }

        @Test
        @DisplayName("Ném lỗi khi Item đã bị khóa ở phiên khác")
        void testCreateAuction_ItemLocked() throws Exception {
            when(mockAuctionDAO.isItemLocked(item.getItemId())).thenReturn(true);

            Exception ex = assertThrows(Exception.class, () -> {
                auctionService.createAuction(item, seller, 500, startTime, endTime);
            });
            assertTrue(ex.getMessage().contains("đang được đấu giá hoặc đã bán"));
        }
    }

    @Nested
    @DisplayName("Tests Thay đổi trạng thái & Tài chính")
    class StatusUpdateTests {

        private Auction mockAuction;

        @BeforeEach
        void setUpMockAuction() throws Exception {
            // KHÔNG DÙNG SPY NỮA, dùng đối tượng thật để tránh lỗi Mockito với hàm static/final
            mockAuction = new Auction(1, item, seller.getUserId(), 15000, 500, startTime, endTime);
            when(mockAuctionDAO.findById(1)).thenReturn(mockAuction);
        }

        @Test
        @DisplayName("Admin khóa (ban) phiên đấu giá thành công")
        void testBanAuction_Success() throws Exception {
            auctionService.banAuction(1, admin);
            assertTrue(mockAuction.isBanned());
            verify(mockAuctionDAO, times(1)).update(mockAuction);
        }

        @Test
        @DisplayName("Hủy (Cancel) phiên -> Hoàn tiền lại cho Leading Bidder")
        void testCancelAuction_RefundsLeader() throws Exception {
            // 1. Setup trạng thái chạy hợp lệ
            mockAuction.updateStatus(AuctionStatus.RUNNING);

            // 2. Setup dữ liệu người dẫn đầu bằng hàm thật của model
            mockAuction.setCurrentPrice(50_000.0);
            Bid dummyBid = new Bid(0, 1, bidder.getUserId(), 50_000.0, LocalDateTime.now());
            mockAuction.recordBid(dummyBid); // Hàm này sẽ tự động gán leadingBidderId

            when(mockUserDAO.findById(bidder.getUserId())).thenReturn(bidder);

            double balanceBefore = bidder.getBalance();
            double ratio = Auction.getRatio(); // Lấy tỷ lệ thật từ class tĩnh

            // 3. Gọi Hủy phiên
            auctionService.cancelAuction(1, admin);

            // 4. Kiểm tra kết quả
            assertEquals(AuctionStatus.CANCELED, mockAuction.getStatus());
            // Bidder phải được nhận lại tiền cọc (Price * Ratio)
            assertEquals(balanceBefore + (50_000.0 * ratio), bidder.getBalance());
            verify(mockUserDAO, times(1)).update(bidder);
        }

        @Test
        @DisplayName("Đánh dấu thanh toán (markAsPaid) -> Chuyển tiền trừ cọc")
        void testMarkAsPaid_BalanceTransfer() throws Exception {
            // Bước 1: Cho phiên bắt đầu CHẠY
            mockAuction.updateStatus(AuctionStatus.RUNNING);

            // Bước 2: Thêm lượt đặt giá (Bid) NGAY LÚC ĐANG CHẠY
            mockAuction.setCurrentPrice(100_000.0);
            Bid dummyBid = new Bid(0, 1, bidder.getUserId(), 100_000.0, LocalDateTime.now());
            mockAuction.recordBid(dummyBid); // Phải recordBid lúc đang RUNNING

            // Bước 3: Bây giờ mới KẾT THÚC phiên
            mockAuction.updateStatus(AuctionStatus.FINISHED);

            // --- Thiết lập Mock Database ---
            when(mockUserDAO.findById(bidder.getUserId())).thenReturn(bidder);
            when(mockUserDAO.findById(seller.getUserId())).thenReturn(seller);

            double bidderBalanceBefore = bidder.getBalance();
            double sellerBalanceBefore = seller.getBalance();
            double ratio = Auction.getRatio();

            // Bước 4: Gọi hàm thanh toán
            auctionService.markAsPaid(1);

            // Bước 5: Kiểm tra kết quả
            assertEquals(AuctionStatus.PAID, mockAuction.getStatus());

            double actualPayAmount = 100_000.0 * (1 - ratio);

            // Bidder bị trừ tiền, Seller được cộng tiền
            assertEquals(bidderBalanceBefore - actualPayAmount, bidder.getBalance());
            assertEquals(sellerBalanceBefore + actualPayAmount, seller.getBalance());

            verify(mockUserDAO, times(1)).update(bidder);
            verify(mockUserDAO, times(1)).update(seller);
            verify(mockAuctionDAO, times(1)).update(mockAuction);
        }
    }
}