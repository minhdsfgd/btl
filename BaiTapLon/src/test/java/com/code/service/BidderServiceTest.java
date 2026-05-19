package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.BidDAO;
import com.code.exception.*;
import com.code.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

    // đối tượng dùng chung cho mọi test
    private BidService bidService;
    private FakeBidDAO fakeBidDAO;
    private FakeAuctionDAO fakeAuctionDAO;

    private RegularUser bidder;       // người đặt giá hợp lệ
    private RegularUser bannedUser;   // bị ban
    private RegularUser noBidderRole; // không có role BIDDER
    private RegularUser seller;       // chủ phiên đấu giá

    private Auction runningAuction;   // phiên đang RUNNING, sẵn sàng nhận bid
    private Auction openAuction;      // phiên ở trạng thái OPEN ch bắt đầu

    private static final int    AUCTION_ID      = 1;
    private static final double STARTING_PRICE  = 100_000;
    private static final double BID_INCREMENT   = 10_000;
    private static final double BIDDER_BALANCE  = 500_000;

    // khởi tạo lại từ đầu trước mỗi test
    @BeforeEach
    void setUp() {
        fakeBidDAO     = new FakeBidDAO();
        fakeAuctionDAO = new FakeAuctionDAO();

        // userDAO không dùng trong BidService.placeBid()  truyền null
        // txService cũng dùng null trong test (không kiểm tra transaction log)
        bidService = new BidService(fakeBidDAO, fakeAuctionDAO, null, null);

        //tạpo user
        bidder = new RegularUser(10, "alice", "pass123", BIDDER_BALANCE, Role.BIDDER);
        bidder.setActive(true);
        bidder.setBanned(false);

        bannedUser = new RegularUser(11, "bad_guy", "pass", 999_999, Role.BIDDER);
        bannedUser.setActive(true);
        bannedUser.setBanned(true);   //  ban

        noBidderRole = new RegularUser(12, "viewer", "pass", 999_999, Role.SELLER);
        noBidderRole.setActive(true);
        noBidderRole.setBanned(false); //kh có BIDDER role

        seller = new RegularUser(20, "bob_seller", "pass", 0, Role.SELLER);
        seller.setActive(true);
        seller.setBanned(false);

        //tạo item
        Item item = new Electronics(1, seller.getUserId(), "Laptop Gaming",
                "RTX 4070, 16GB RAM", STARTING_PRICE);

        //tạo phiên running
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end   = LocalDateTime.now().plusHours(1);

        runningAuction = new Auction(AUCTION_ID, item, seller.getUserId(),
                STARTING_PRICE, BID_INCREMENT, start, end);
        runningAuction.updateStatus(AuctionStatus.RUNNING); // open => running
        fakeAuctionDAO.store(runningAuction);

        // tạo phiên ch bắt đàua
        openAuction = new Auction(2, item, seller.getUserId(),
                STARTING_PRICE, BID_INCREMENT,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2));
        fakeAuctionDAO.store(openAuction);
    }

    // PLACE BID

    @Test
    @DisplayName("TC01: Đặt giá thành công — trả về Bid và cập nhật currentPrice")
    void testPlaceBid_Success() throws Exception {
        double bidAmount = STARTING_PRICE + BID_INCREMENT; // 110_000

        Bid result = bidService.placeBid(bidder, runningAuction, bidAmount);

        // Bid phải được tạo đúng
        assertNotNull(result, "Bid trả về không được null");
        assertEquals(bidder.getUserId(),          result.getUserId(),    "userId trong Bid phải khớp");
        assertEquals(runningAuction.getAuctionId(), result.getAuctionId(), "auctionId phải khớp");
        assertEquals(bidAmount, result.getAmount(), 0.01,                 "amount phải khớp");

        // Giá phiên phải tăng
        assertEquals(bidAmount, runningAuction.getCurrentPrice(), 0.01,
                "currentPrice phải cập nhật theo bid mới");

        // Leading bidder phải được ghi nhận
        assertEquals(bidder.getUserId(), runningAuction.getLeadingBidderId(),
                "leadingBidderId phải là bidder vừa đặt");

        // Số dư phải giảm
        assertEquals(BIDDER_BALANCE - bidAmount, bidder.getBalance(), 0.01,
                "Số dư phải bị trừ sau khi đặt giá");

        // Fake database phải có bid này
        assertEquals(1, fakeBidDAO.getSavedBids().size(), "Bid phải được lưu vào 'DB'");
    }

    @Test
    @DisplayName("TC02: Đặt giá thành công — người sau outbid người trước")
    void testPlaceBid_SecondBid_HigherThanFirst() throws Exception {
        double firstBid  = STARTING_PRICE + BID_INCREMENT;  // 110_000
        double secondBid = firstBid + BID_INCREMENT;         // 120_000

        RegularUser bidder2 = new RegularUser(13, "charlie", "pass", 999_999, Role.BIDDER);
        bidder2.setActive(true);
        bidder2.setBanned(false);

        bidService.placeBid(bidder,  runningAuction, firstBid);
        bidService.placeBid(bidder2, runningAuction, secondBid);

        assertEquals(secondBid, runningAuction.getCurrentPrice(), 0.01,
                "Giá phải bằng bid cao nhất");
        assertEquals(bidder2.getUserId(), runningAuction.getLeadingBidderId(),
                "Người dẫn đầu phải là bidder2");
        assertEquals(2, fakeBidDAO.getSavedBids().size(), "Phải có 2 bid được lưu");
    }



    @Test
    @DisplayName("TC03: Tài khoản bị ban => ném UserBannedException")
    void testPlaceBid_UserBanned_ThrowsException() {
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        UserBannedException ex = assertThrows(UserBannedException.class,
                () -> bidService.placeBid(bannedUser, runningAuction, bidAmount));

        assertTrue(ex.getMessage().contains("bad_guy") || ex.getMessage() != null,
                "Message phải đề cập đến user bị ban");
        assertEquals(0, fakeBidDAO.getSavedBids().size(), "Bid không được lưu khi user bị ban");
    }

    @Test
    @DisplayName("TC04: Thiếu role BIDDER => ném InvalidBidException")
    void testPlaceBid_NoRoleBidder_ThrowsException() {
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(noBidderRole, runningAuction, bidAmount),
                "Phải ném InvalidBidException khi không có role BIDDER");
    }

    @Test
    @DisplayName("TC05: Chủ phiên tự bid sản phẩm của mình => ném SelfBidException")
    void testPlaceBid_SelfBid_ThrowsException() {
        // seller cũng có BIDDER role
        seller.addRole(Role.BIDDER);
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(SelfBidException.class,
                () -> bidService.placeBid(seller, runningAuction, bidAmount),
                "Phải ném SelfBidException khi tự bid sản phẩm của mình");
    }

    @Test
    @DisplayName("TC06: Phiên chưa RUNNING (đang OPEN) => ném AuctionClosedException")
    void testPlaceBid_AuctionNotRunning_ThrowsException() {
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(AuctionClosedException.class,
                () -> bidService.placeBid(bidder, openAuction, bidAmount),
                "Phải ném AuctionClosedException khi phiên chưa RUNNING");
    }

    @Test
    @DisplayName("TC07: Phiên bị Admin ban => ném AuctionClosedException")
    void testPlaceBid_AuctionBanned_ThrowsException() {
        runningAuction.setBanned(true);
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(AuctionClosedException.class,
                () -> bidService.placeBid(bidder, runningAuction, bidAmount),
                "Phải ném AuctionClosedException khi phiên bị ban");
    }

    @Test
    @DisplayName("TC08: Số tiền đặt giá thấp hơn mức tối thiểu => ném InvalidBidException")
    void testPlaceBid_AmountTooLow_ThrowsException() {
        // min = currentPrice + bidIncrement = 110_000; đặt 105_000 → không đủ
        double tooLow = STARTING_PRICE + BID_INCREMENT - 1; // 109_999

        InvalidBidException ex = assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(bidder, runningAuction, tooLow));

        assertNotNull(ex.getMessage(), "Message không được null");
        assertTrue(ex.getMessage().contains("tối thiểu") || ex.getMessage().contains("110"),
                "Message nên ghi rõ mức giá tối thiểu");
    }

    @Test
    @DisplayName("TC09: Số dư tài khoản không đủ => ném InsufficientBalanceException")
    void testPlaceBid_InsufficientBalance_ThrowsException() {
        // Trừ hết tiền trước
        try { bidder.deductBalance(BIDDER_BALANCE); }
        catch (InsufficientBalanceException ignored) {}

        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(InsufficientBalanceException.class,
                () -> bidService.placeBid(bidder, runningAuction, bidAmount),
                "Phải ném InsufficientBalanceException khi số dư không đủ");
    }

    @Test
    @DisplayName("TC10: Phiên FINISHED không nhận bid => ném AuctionClosedException")
    void testPlaceBid_AuctionFinished_ThrowsException() {
        runningAuction.updateStatus(AuctionStatus.FINISHED); // RUNNING → FINISHED
        double bidAmount = STARTING_PRICE + BID_INCREMENT;

        assertThrows(AuctionClosedException.class,
                () -> bidService.placeBid(bidder, runningAuction, bidAmount),
                "Phải ném AuctionClosedException khi phiên đã FINISHED");
    }


    //  DEPOSIT

    @Test
    @DisplayName("TC11: Nạp tiền thành công — số dư tăng, Transaction được tạo")
    void testDeposit_Success() throws Exception {
        double depositAmount = 200_000;
        double balanceBefore = bidder.getBalance();

        Transaction tx = bidService.deposit(bidder, depositAmount);

        assertNotNull(tx, "Transaction không được null");
        assertEquals(balanceBefore + depositAmount, bidder.getBalance(), 0.01,
                "Số dư phải tăng đúng sau khi nạp tiền");
        assertEquals(depositAmount, tx.getAmount(), 0.01,
                "Số tiền trong Transaction phải khớp");
    }

    @Test
    @DisplayName("TC12: Nạp tiền với tài khoản bị ban => ném UserBannedException")
    void testDeposit_BannedUser_ThrowsException() {
        assertThrows(UserBannedException.class,
                () -> bidService.deposit(bannedUser, 100_000),
                "Phải ném UserBannedException khi tài khoản bị ban");
    }

    @Test
    @DisplayName("TC13: Nạp tiền với số âm => ném IllegalArgumentException")
    void testDeposit_NegativeAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bidService.deposit(bidder, -50_000),
                "Phải ném IllegalArgumentException khi số tiền âm");
    }

    @Test
    @DisplayName("TC14: Nạp tiền với số bằng 0 => ném IllegalArgumentException")
    void testDeposit_ZeroAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bidService.deposit(bidder, 0),
                "Phải ném IllegalArgumentException khi số tiền = 0");
    }


    //  FAKE DAO — in-memory, không kết nối DB
    //BidDAO giả — lưu bid vào List trong RAM thay vì MySQL
    static class FakeBidDAO extends BidDAO {
        private final List<Bid> savedBids = new CopyOnWriteArrayList<>();

        @Override
        public void save(Bid bid) throws SQLException {
            bid.setBidId(savedBids.size() + 1); // gán ID giả
            savedBids.add(bid);
        }

        @Override
        public List<Bid> findByAuctionId(int auctionId) throws SQLException {
            return savedBids.stream()
                    .filter(b -> b.getAuctionId() == auctionId)
                    .toList();
        }

        @Override
        public List<Bid> findByUserId(int userId) throws SQLException {
            return savedBids.stream()
                    .filter(b -> b.getUserId() == userId)
                    .toList();
        }

        @Override
        public Optional<Bid> findHighestBid(int auctionId) throws SQLException {
            return savedBids.stream()
                    .filter(b -> b.getAuctionId() == auctionId)
                    .max(Comparator.comparingDouble(Bid::getAmount));
        }

        // test để kiểm tra bao nhiêu bid đã được lưu
        List<Bid> getSavedBids() { return savedBids; }
    }

    // AuctionDAO giả — lưu Auction vào Map trong RAM
    static class FakeAuctionDAO extends AuctionDAO {
        private final Map<Integer, Auction> store = new LinkedHashMap<>();

        void store(Auction auction) {
            store.put(auction.getAuctionId(), auction);
        }

        @Override
        public Auction findById(int id) throws SQLException {
            return store.get(id);
        }

        @Override
        public List<Auction> findAll() throws SQLException {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Auction> findActiveAuctions() throws SQLException {
            return store.values().stream()
                    .filter(a -> a.getStatus().isActive())
                    .toList();
        }

        @Override
        public List<Auction> findBySellerId(int sellerId) throws SQLException {
            return store.values().stream()
                    .filter(a -> a.getSellerId() == sellerId)
                    .toList();
        }

        @Override
        public void save(Auction auction) throws SQLException {
            store.put(auction.getAuctionId(), auction);
        }

        @Override
        public void update(Auction auction) throws SQLException {
            store.put(auction.getAuctionId(), auction);
        }
    }
}