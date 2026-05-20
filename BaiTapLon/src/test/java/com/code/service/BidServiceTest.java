package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.BidDAO;
import com.code.dao.UserDAO;
import com.code.exception.*;
import com.code.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BidServiceTest {

    private BidDAO mockBidDAO;
    private AuctionDAO mockAuctionDAO;
    private UserDAO mockUserDAO;
    private TransactionService mockTxService;
    private BidService bidService;

    private RegularUser seller;
    private RegularUser bidder1;
    private RegularUser bidder2;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 1. Tạo Mocks
        mockBidDAO = Mockito.mock(BidDAO.class);
        mockAuctionDAO = Mockito.mock(AuctionDAO.class);
        mockUserDAO = Mockito.mock(UserDAO.class);
        mockTxService = Mockito.mock(TransactionService.class);

        // 2. Khởi tạo Service
        bidService = new BidService(mockBidDAO, mockAuctionDAO, mockUserDAO, mockTxService);

        // 3. Chuẩn bị dữ liệu mẫu
        seller = new RegularUser(1, "seller", "pass", 0, Role.SELLER);
        bidder1 = new RegularUser(2, "bidder1", "pass", 500_000, Role.BIDDER);
        bidder2 = new RegularUser(3, "bidder2", "pass", 500_000, Role.BIDDER);

        Item item = new Electronics(10, seller.getUserId(), "Laptop", "Gaming", 100_000);
        auction = new Auction(100, item, seller.getUserId(), 100_000, 10_000,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));

        // Mặc định cho phiên đang chạy để các test không bị vướng lỗi "AuctionClosed"
        auction.updateStatus(AuctionStatus.RUNNING);
    }

    // =========================================================================
    // 1. TEST LOGIC NẠP TIỀN (DEPOSIT)
    // =========================================================================
    @Nested
    @DisplayName("Tests Chức năng Nạp tiền (deposit)")
    class DepositTests {

        @Test
        @DisplayName("TC01: Nạp tiền thành công")
        void testDeposit_Success() throws UserBannedException {
            Transaction tx = bidService.deposit(bidder1, 50_000);

            assertEquals(550_000, bidder1.getBalance());
            assertNotNull(tx);
            assertEquals(50_000, tx.getAmount());
        }

        @Test
        @DisplayName("TC02: Ném lỗi nếu User bị ban")
        void testDeposit_BannedUser() {
            bidder1.setBanned(true);
            assertThrows(UserBannedException.class, () -> bidService.deposit(bidder1, 50_000));
        }

        @Test
        @DisplayName("TC03: Ném lỗi nếu số tiền <= 0")
        void testDeposit_InvalidAmount() {
            assertThrows(IllegalArgumentException.class, () -> bidService.deposit(bidder1, 0));
            assertThrows(IllegalArgumentException.class, () -> bidService.deposit(bidder1, -100));
        }
    }

    // =========================================================================
    // 2. TEST LOGIC ĐẶT GIÁ: CÁC LUỒNG THẤT BẠI (VALIDATION)
    // =========================================================================
    @Nested
    @DisplayName("Tests Đặt giá (placeBid) - Ngoại lệ (Failures)")
    class PlaceBidFailureTests {

        @Test
        @DisplayName("TC04: Ném lỗi UserBannedException nếu User bị ban")
        void testPlaceBid_UserBanned() {
            bidder1.setBanned(true);
            assertThrows(UserBannedException.class, () -> bidService.placeBid(bidder1, auction, 120_000));
        }

        @Test
        @DisplayName("TC05: Ném lỗi InvalidBidException nếu User không có quyền BIDDER")
        void testPlaceBid_NoBidderRole() {
            RegularUser viewer = new RegularUser(4, "viewer", "p", 1000, Role.SELLER); // Chỉ có SELLER
            assertThrows(InvalidBidException.class, () -> bidService.placeBid(viewer, auction, 120_000));
        }

        @Test
        @DisplayName("TC06: Ném lỗi SelfBidException nếu Seller tự đấu giá đồ của mình")
        void testPlaceBid_SelfBid() {
            seller.addRole(Role.BIDDER); // Cho mượn quyền Bidder
            assertThrows(SelfBidException.class, () -> bidService.placeBid(seller, auction, 120_000));
        }

        @Test
        @DisplayName("TC07: Ném lỗi AuctionClosedException nếu Phiên chưa chạy hoặc đã kết thúc")
        void testPlaceBid_NotRunning() {
            Auction closedAuction = new Auction(101, auction.getItem(), 1, 100_000, 10_000,
                    LocalDateTime.now(), LocalDateTime.now().plusHours(1));
            // Trạng thái OPEN mặc định chưa chạy
            assertThrows(AuctionClosedException.class, () -> bidService.placeBid(bidder1, closedAuction, 120_000));
        }

        @Test
        @DisplayName("TC08: Ném lỗi InvalidBidException nếu số tiền đặt < Giá hiện tại + Bước giá")
        void testPlaceBid_AmountTooLow() {
            // Giá hiện tại 100k, bước giá 10k => Tối thiểu phải đặt 110k
            assertThrows(InvalidBidException.class, () -> bidService.placeBid(bidder1, auction, 105_000));
        }

        @Test
        @DisplayName("TC09: Ném lỗi InsufficientBalanceException nếu User không đủ tiền")
        void testPlaceBid_InsufficientBalance() {
            // Đặt 600k trong khi tài khoản chỉ có 500k
            assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(bidder1, auction, 600_000));
        }
    }

    // =========================================================================
    // 3. TEST LOGIC ĐẶT GIÁ: CÁC LUỒNG THÀNH CÔNG (TRỪ TIỀN / HOÀN TIỀN)
    // =========================================================================
    @Nested
    @DisplayName("Tests Đặt giá (placeBid) - Luồng Thành công (Success)")
    class PlaceBidSuccessTests {

        @Test
        @DisplayName("TC10: Đặt giá lần đầu (Chưa có ai bid) -> Trừ đủ tiền")
        void testPlaceBid_FirstBid_Success() throws Exception {
            Bid bid = bidService.placeBid(bidder1, auction, 110_000);

            // Kiểm tra số dư (bị giam 110k)
            assertEquals(390_000, bidder1.getBalance());

            // Kiểm tra cập nhật giá phiên
            assertEquals(110_000, auction.getCurrentPrice());
            assertEquals(bidder1.getUserId(), auction.getLeadingBidderId());

            // Kiểm tra gọi xuống Database
            verify(mockBidDAO, times(1)).save(any(Bid.class));
            verify(mockAuctionDAO, times(1)).update(auction);
            verify(mockUserDAO, times(1)).update(bidder1);
        }

        @Test
        @DisplayName("TC11: Vượt giá người khác -> Hoàn tiền người cũ, Trừ tiền người mới")
        void testPlaceBid_OutbidSomeoneElse() throws Exception {
            // 1. Bidder1 đặt giá trước (110k)
            bidService.placeBid(bidder1, auction, 110_000);
            assertEquals(390_000, bidder1.getBalance());

            // Giả lập DB trả về Bidder1 khi cần hoàn tiền
            when(mockUserDAO.findById(bidder1.getUserId())).thenReturn(bidder1);

            // 2. Bidder2 vào vượt giá (130k)
            bidService.placeBid(bidder2, auction, 130_000);

            // Kết quả mong đợi:
            // - Bidder 2 bị trừ 130k
            assertEquals(370_000, bidder2.getBalance(), "Bidder 2 phải bị trừ tiền");

            // - Bidder 1 được hoàn lại 110k ban đầu (390k + 110k = 500k)
            assertEquals(500_000, bidder1.getBalance(), "Bidder 1 phải được hoàn tiền");

            // - Giá mới phải là 130k
            assertEquals(130_000, auction.getCurrentPrice());

            // Ghi log giao dịch refund
            verify(mockTxService, times(1)).logRefund(eq(bidder1.getUserId()), eq(110_000.0), anyInt());
        }

        @Test
        @DisplayName("TC12: Tự vượt giá chính mình (Self-Outbid) -> Chỉ trừ tiền chênh lệch")
        void testPlaceBid_SelfOutbid() throws Exception {
            // 1. Bidder1 đặt 110k
            bidService.placeBid(bidder1, auction, 110_000);
            assertEquals(390_000, bidder1.getBalance()); // 500k - 110k

            // 2. Bidder1 TỰ đặt thêm lên 150k (để đè bẹp đối thủ chưa kịp bid)
            bidService.placeBid(bidder1, auction, 150_000);

            // Kết quả mong đợi: Bidder1 chỉ bị trừ THÊM 40k chênh lệch (150k - 110k)
            assertEquals(350_000, bidder1.getBalance(), "Chỉ bị trừ thêm tiền chênh lệch");
            assertEquals(150_000, auction.getCurrentPrice());
        }
    }

    // =========================================================================
    // 4. TEST ROLLBACK KHI DATABASE LỖI
    // =========================================================================
    @Nested
    @DisplayName("Tests Rollback dữ liệu khi DB lỗi")
    class RollbackTests {

        @Test
        @DisplayName("TC13: Trả lại tiền (Rollback balance) nếu lưu Bid xuống DB bị Exception")
        void testPlaceBid_DBException_RollbacksBalance() throws Exception {
            // Giả lập hệ thống lưu Bid bị lỗi (sập DB)
            doThrow(new SQLException("Mất kết nối DB")).when(mockBidDAO).save(any(Bid.class));

            // Số dư ban đầu của bidder1 là 500_000

            // Thực hiện đặt giá
            assertThrows(RuntimeException.class, () -> bidService.placeBid(bidder1, auction, 120_000));

            // CHỨNG MINH: Dù bị lỗi văng ra giữa chừng, số dư của Bidder1 VẪN PHẢI LÀ 500k
            // (Hàm catch trong BidService đã gọi `user.deposit()` để rollback tiền bị giữ)
            assertEquals(500_000, bidder1.getBalance(), "Tiền bị giam phải được trả lại khi DB lỗi");
        }
    }
}