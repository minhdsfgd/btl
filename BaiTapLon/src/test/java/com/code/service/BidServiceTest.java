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

    // Sẽ lấy tự động từ class Auction
    private double AUCTION_RATIO;

    @BeforeEach
    void setUp() {
        mockBidDAO = Mockito.mock(BidDAO.class);
        mockAuctionDAO = Mockito.mock(AuctionDAO.class);
        mockUserDAO = Mockito.mock(UserDAO.class);
        mockTxService = Mockito.mock(TransactionService.class);

        bidService = new BidService(mockBidDAO, mockAuctionDAO, mockUserDAO, mockTxService);

        seller = new RegularUser(1, "seller", "pass", 0, Role.SELLER);
        bidder1 = new RegularUser(2, "bidder1", "pass", 500_000, Role.BIDDER);
        bidder2 = new RegularUser(3, "bidder2", "pass", 500_000, Role.BIDDER);

        Item item = new Electronics(10, seller.getUserId(), "Laptop", "Gaming", 100_000);

        // KHÔNG DÙNG SPY NỮA, dùng đối tượng Auction thật để tránh lỗi Mockito
        auction = new Auction(100, item, seller.getUserId(), 100_000, 10_000,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));

        // Lấy tỷ lệ cọc trực tiếp từ model
        AUCTION_RATIO = Auction.getRatio();

        auction.updateStatus(AuctionStatus.RUNNING);
    }

    @Nested
    @DisplayName("Tests Validate Ngoại lệ (Failures)")
    class PlaceBidFailureTests {
        @Test
        @DisplayName("Ném lỗi UserBannedException nếu User bị ban")
        void testPlaceBid_UserBanned() {
            // Sửa thành setActive(false) để khớp với logic của BidService
            bidder1.setActive(false);

            assertThrows(UserBannedException.class, () -> bidService.placeBid(bidder1, auction, 120_000));
        }

        @Test
        @DisplayName("Ném lỗi InvalidBidException nếu User không có quyền BIDDER")
        void testPlaceBid_NoBidderRole() {
            RegularUser viewer = new RegularUser(4, "viewer", "p", 1000, Role.SELLER);
            assertThrows(InvalidBidException.class, () -> bidService.placeBid(viewer, auction, 120_000));
        }

        @Test
        @DisplayName("Ném lỗi SelfBidException nếu tự đấu giá đồ của mình")
        void testPlaceBid_SelfBid() {
            seller.addRole(Role.BIDDER);
            assertThrows(SelfBidException.class, () -> bidService.placeBid(seller, auction, 120_000));
        }

        @Test
        @DisplayName("Ném lỗi AuctionClosedException nếu Phiên không RUNNING")
        void testPlaceBid_NotRunning() {
            auction.updateStatus(AuctionStatus.FINISHED); // Đổi trạng thái để test
            assertThrows(AuctionClosedException.class, () -> bidService.placeBid(bidder1, auction, 120_000));
        }

        @Test
        @DisplayName("Ném lỗi AuctionClosedException nếu Phiên bị Banned")
        void testPlaceBid_BannedAuction() {
            auction.setBanned(true);
            assertThrows(AuctionClosedException.class, () -> bidService.placeBid(bidder1, auction, 120_000));
        }

        @Test
        @DisplayName("Ném lỗi InvalidBidException nếu số tiền đặt < Giá hiện tại + Bước giá")
        void testPlaceBid_AmountTooLow() {
            assertThrows(InvalidBidException.class, () -> bidService.placeBid(bidder1, auction, 105_000));
        }

        @Test
        @DisplayName("Ném lỗi InsufficientBalanceException nếu không đủ tiền cọc")
        void testPlaceBid_InsufficientBalance() {
            // Giá 6_000_000 => Tiền cọc cần 10% = 600_000. Balance chỉ có 500_000
            assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(bidder1, auction, 6_000_000));
        }
    }

    @Nested
    @DisplayName("Tests Luồng Thành công (Success & Transactions)")
    class PlaceBidSuccessTests {

        @Test
        @DisplayName("Đặt giá lần đầu -> Trừ đúng % tiền cọc")
        void testPlaceBid_FirstBid_Success() throws Exception {
            double bidAmount = 110_000;
            double depositRequired = bidAmount * AUCTION_RATIO; // 11_000

            Bid bid = bidService.placeBid(bidder1, auction, bidAmount);

            assertEquals(500_000 - depositRequired, bidder1.getBalance());
            assertEquals(110_000, auction.getCurrentPrice());
            assertEquals(bidder1.getUserId(), auction.getLeadingBidderId());

            verify(mockTxService, times(1)).logBidHold(bidder1.getUserId(), depositRequired, auction.getAuctionId());
            verify(mockBidDAO, times(1)).save(any(Bid.class));
        }

        @Test
        @DisplayName("Vượt giá -> Hoàn cọc người cũ, trừ cọc người mới")
        void testPlaceBid_OutbidSomeoneElse() throws Exception {
            // 1. Bidder1 đặt giá trước 110k
            bidService.placeBid(bidder1, auction, 110_000);
            when(mockUserDAO.findById(bidder1.getUserId())).thenReturn(bidder1);

            // 2. Bidder2 vượt giá 130k
            bidService.placeBid(bidder2, auction, 130_000);

            // Bidder2 bị trừ cọc
            assertEquals(500_000 - (130_000 * AUCTION_RATIO), bidder2.getBalance());
            // Bidder1 được hoàn lại cọc ban đầu -> Về 500k
            assertEquals(500_000, bidder1.getBalance());
            assertEquals(130_000, auction.getCurrentPrice());

            verify(mockTxService, times(1)).logRefund(eq(bidder1.getUserId()), eq(110_000 * AUCTION_RATIO), anyInt());
        }

        @Test
        @DisplayName("Tự vượt giá chính mình -> Chỉ trừ thêm % chênh lệch")
        void testPlaceBid_SelfOutbid() throws Exception {
            // 1. Bidder1 đặt 110k
            bidService.placeBid(bidder1, auction, 110_000);

            // 2. Bidder1 TỰ nâng lên 150k
            bidService.placeBid(bidder1, auction, 150_000);

            // Tổng cọc bị trừ là 150_000 * Ratio. Số dư sau cùng phải khớp
            double finalBalance = 500_000 - (150_000 * AUCTION_RATIO);
            assertEquals(finalBalance, bidder1.getBalance());
            assertEquals(150_000, auction.getCurrentPrice());
        }
    }

    @Nested
    @DisplayName("Tests Các tính năng đặc biệt (Sniping & AutoBid)")
    class SpecialFeaturesTests {
        @Test
        @DisplayName("Anti-sniping: Cộng thêm 1 phút nếu đặt giá <= 10s cuối")
        void testAntiSniping() throws Exception {
            LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(5);
            auction.setEndTime(nearEnd);

            bidService.placeBid(bidder1, auction, 110_000);

            // Kiểm tra thời gian kết thúc đã được cộng thêm 1 phút
            assertTrue(auction.getEndTime().isAfter(nearEnd.plusSeconds(50)));
            verify(mockAuctionDAO, atLeastOnce()).update(auction);
        }

        @Test
        @DisplayName("Auto-Bid: Tự động vượt giá người khác nếu trong ngưỡng max")
        void testAutoBid_Success() throws Exception {
            // Setup Auto-bid cho bidder1 DÙNG HÀM THẬT của model (Không dùng Mockito)
            auction.setAutoBidUserId(bidder1.getUserId());
            auction.setAutoBidMaxAmount(200_000.0);
            auction.setAutoBidStep(10_000.0);

            // Trả về user khi hàm Auto-Bid đệ quy cần hoàn tiền / kiểm tra số dư
            when(mockUserDAO.findById(bidder1.getUserId())).thenReturn(bidder1);
            when(mockUserDAO.findById(bidder2.getUserId())).thenReturn(bidder2);

            // Bidder2 đặt 110k
            bidService.placeBid(bidder2, auction, 110_000);

            // Tự động vượt lên 120_000 (110_000 + 10_000 bước nhảy)
            assertEquals(120_000, auction.getCurrentPrice());
            assertEquals(bidder1.getUserId(), auction.getLeadingBidderId());

            // Kiểm tra bidder 2 đã được hoàn tiền cọc vì bị Auto-bid đè
            assertEquals(500_000, bidder2.getBalance());
        }
    }
}