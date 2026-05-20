package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.UserDAO;
import com.code.exception.AuctionClosedException;
import com.code.models.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.SQLException;
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
        // 1. Tạo các Mock Object
        mockAuctionDAO = Mockito.mock(AuctionDAO.class);
        mockUserDAO = Mockito.mock(UserDAO.class);
        mockTxService = Mockito.mock(TransactionService.class);

        // 2. Reset Singleton instance bằng Reflection (Cực kỳ quan trọng để các test độc lập)
        resetSingleton(AuctionService.class, "instance");

        // 3. Khởi tạo Service với Mock DAOs
        AuctionService.init(mockAuctionDAO, mockUserDAO, mockTxService);
        auctionService = AuctionService.getInstance();

        // 4. Chuẩn bị dữ liệu mẫu (Dummy data)
        seller = new RegularUser(1, "seller", "pass", 0, Role.SELLER);
        bidder = new RegularUser(2, "bidder", "pass", 100000, Role.BIDDER);
        admin = new Admin(99, "admin", "pass", 0);

        item = new Electronics(10, 1, "Laptop", "Gaming", 15000, "Dell", 12);

        startTime = LocalDateTime.now().plusMinutes(5);
        endTime = startTime.plusHours(1);
    }

    @AfterEach
    void tearDown() {
        // Tắt luồng chạy ngầm (Scheduler) để không bị treo bộ nhớ sau khi test xong
        if (auctionService != null) {
            auctionService.shutdown();
        }
    }

    /** Helper function để phá vỡ Singleton phục vụ việc test */
    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        Field instanceField = clazz.getDeclaredField(fieldName);
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // =========================================================================
    // 1. TEST TẠO PHIÊN ĐẤU GIÁ (CREATE AUCTION)
    // =========================================================================
    @Nested
    @DisplayName("Tests Tạo phiên đấu giá (createAuction)")
    class CreateAuctionTests {

        @Test
        @DisplayName("TC01: Tạo phiên thành công khi dữ liệu hợp lệ")
        void testCreateAuction_Success() throws Exception {
            // Giả lập DB trả về false (Item chưa bị khóa)
            when(mockAuctionDAO.isItemLocked(item.getItemId())).thenReturn(false);

            Auction created = auctionService.createAuction(item, seller, 500, startTime, endTime);

            assertNotNull(created);
            assertEquals(1, created.getSellerId());
            assertEquals(15000, created.getCurrentPrice());
            assertEquals(AuctionStatus.OPEN, created.getStatus());

            // Xác minh hàm save() của DAO đã được gọi đúng 1 lần
            verify(mockAuctionDAO, times(1)).save(any(Auction.class));
        }

        @Test
        @DisplayName("TC02: Ném lỗi khi User không có quyền SELLER")
        void testCreateAuction_NotSellerRole() {
            Exception exception = assertThrows(AuctionClosedException.class, () -> {
                auctionService.createAuction(item, bidder, 500, startTime, endTime); // bidder ko có Role.SELLER
            });
            assertTrue(exception.getMessage().contains("Cần vai trò SELLER"));
        }

        @Test
        @DisplayName("TC03: Ném lỗi khi Item đã bị khóa (đang đấu giá ở nơi khác)")
        void testCreateAuction_ItemLocked() throws Exception {
            when(mockAuctionDAO.isItemLocked(item.getItemId())).thenReturn(true); // Bị khóa

            Exception exception = assertThrows(Exception.class, () -> {
                auctionService.createAuction(item, seller, 500, startTime, endTime);
            });
            assertTrue(exception.getMessage().contains("đang được đấu giá hoặc đã bán"));
        }

        @Test
        @DisplayName("TC04: Ném lỗi khi người tạo không phải chủ sở hữu Item")
        void testCreateAuction_NotItemOwner() {
            // Đổi chủ của item sang ID = 999
            Electronics stolenItem = new Electronics(10, 999, "Laptop", "Gaming", 15000);

            Exception exception = assertThrows(Exception.class, () -> {
                auctionService.createAuction(stolenItem, seller, 500, startTime, endTime);
            });
            assertTrue(exception.getMessage().contains("không phải chủ sở hữu"));
        }
    }

    // =========================================================================
    // 2. TEST LẤY THÔNG TIN PHIÊN ĐẤU GIÁ (GET AUCTION)
    // =========================================================================
    @Nested
    @DisplayName("Tests Truy vấn (getAuction)")
    class GetAuctionTests {

        @Test
        @DisplayName("TC05: Lấy Auction thành công từ Database (Cache miss)")
        void testGetAuction_FromDB() throws Exception {
            Auction mockAuction = new Auction(100, item, 1, 15000, 500, startTime, endTime);
            when(mockAuctionDAO.findById(100)).thenReturn(mockAuction);

            Auction result = auctionService.getAuction(100);

            assertNotNull(result);
            assertEquals(100, result.getAuctionId());
            verify(mockAuctionDAO, times(1)).findById(100);
        }

        @Test
        @DisplayName("TC06: Ném lỗi khi không tìm thấy Auction trong DB")
        void testGetAuction_NotFound() throws Exception {
            when(mockAuctionDAO.findById(999)).thenReturn(null); // Không tìm thấy

            Exception exception = assertThrows(AuctionClosedException.class, () -> {
                auctionService.getAuction(999);
            });
            assertTrue(exception.getMessage().contains("Không tìm thấy phiên đấu giá"));
        }
    }

    // =========================================================================
    // 3. TEST THAY ĐỔI TRẠNG THÁI (START, CANCEL, BAN)
    // =========================================================================
    @Nested
    @DisplayName("Tests Cập nhật Trạng thái (Update Status)")
    class StatusUpdateTests {

        private Auction mockAuction;

        @BeforeEach
        void setUpMockAuction() throws Exception {
            mockAuction = new Auction(1, item, seller.getUserId(), 15000, 500, startTime, endTime);
            // Ép Auction vào cache luôn để bỏ qua bước chọc xuống DB khi test hàm start/cancel
            when(mockAuctionDAO.findById(1)).thenReturn(mockAuction);
        }

        @Test
        @DisplayName("TC07: Owner bắt đầu (Start) phiên đấu giá thành công")
        void testStartAuction_Success_ByOwner() throws Exception {
            auctionService.startAuction(1, seller);

            assertEquals(AuctionStatus.RUNNING, mockAuction.getStatus());
            verify(mockAuctionDAO, times(1)).update(mockAuction);
        }

        @Test
        @DisplayName("TC08: Admin hủy (Cancel) phiên đấu giá thành công")
        void testCancelAuction_Success_ByAdmin() throws Exception {
            // Phải cho nó chạy trước khi hủy (để test logic state machine)
            mockAuction.updateStatus(AuctionStatus.RUNNING);

            auctionService.cancelAuction(1, admin); // Admin có quyền hủy

            assertEquals(AuctionStatus.CANCELED, mockAuction.getStatus());
            verify(mockAuctionDAO, times(1)).update(mockAuction);
        }

        @Test
        @DisplayName("TC09: Ném lỗi khi User thường đòi Start/Cancel phiên của người khác")
        void testAction_Fail_NotOwnerOrAdmin() {
            Exception exception = assertThrows(AuctionClosedException.class, () -> {
                auctionService.startAuction(1, bidder); // Bidder không phải chủ của Item này
            });
            assertTrue(exception.getMessage().contains("Không có quyền bắt đầu"));
        }

        @Test
        @DisplayName("TC10: Đánh dấu đã thanh toán (markAsPaid) thành công")
        void testMarkAsPaid_Success() throws Exception {
            // Giả lập trạng thái đã FINISHED
            mockAuction = Auction.loadFromDB(1, item, seller.getUserId(), 20000, 500, startTime, endTime, AuctionStatus.FINISHED, false, 2);
            when(mockAuctionDAO.findById(1)).thenReturn(mockAuction);

            auctionService.markAsPaid(1);

            assertEquals(AuctionStatus.PAID, mockAuction.getStatus());
            verify(mockAuctionDAO, times(1)).update(mockAuction);
        }
    }
}