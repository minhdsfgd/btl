package com.code.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


class AuctionModelTest {

    private Auction auction;
    private Item    item;
    private static final int SELLER_ID      = 20;
    private static final double START_PRICE = 100_000;
    private static final double INCREMENT   = 10_000;

    @BeforeEach
    void setUp() {
        item = new Electronics(1, SELLER_ID, "iPhone 15 Pro",
                "Màn hình 6.1 inch, chip A17 Pro", START_PRICE);

        auction = new Auction(
                1, item, SELLER_ID,
                START_PRICE, INCREMENT,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2)
        );
    }


    //  Khởi tạo
    @Test
    @DisplayName("TC01: Auction mới phải ở trạng thái OPEN")
    void testNewAuction_StatusIsOpen() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus(),
                "Auction mới phải có status = OPEN");
    }

    @Test
    @DisplayName("TC02: Auction mới chưa có người dẫn đầu (leadingBidderId = -1)")
    void testNewAuction_NoLeadingBidder() {
        assertEquals(-1, auction.getLeadingBidderId(),
                "Chưa có bid thì leadingBidderId phải là -1");
    }

    @Test
    @DisplayName("TC03: endTime phải trước startTime => ném IllegalArgumentException")
    void testConstructor_EndBeforeStart_ThrowsException() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(2, item, SELLER_ID, START_PRICE, INCREMENT,
                        now.plusHours(2), now), // endTime < startTime
                "Phải ném IllegalArgumentException khi endTime < startTime");
    }

    @Test
    @DisplayName("TC04: bidIncrement <= 0 => ném IllegalArgumentException")
    void testConstructor_ZeroIncrement_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(2, item, SELLER_ID, START_PRICE, 0,
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1)),
                "Phải ném IllegalArgumentException khi bidIncrement = 0");
    }


    //  State Machine: OPEN → RUNNING → FINISHED → PAID

    @Test
    @DisplayName("TC05: OPEN → RUNNING hợp lệ")
    void testStateTransition_OpenToRunning() {
        auction.updateStatus(AuctionStatus.RUNNING);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("TC06: OPEN → CANCELED hợp lệ")
    void testStateTransition_OpenToCanceled() {
        auction.updateStatus(AuctionStatus.CANCELED);
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    @DisplayName("TC07: RUNNING → FINISHED hợp lệ")
    void testStateTransition_RunningToFinished() {
        auction.updateStatus(AuctionStatus.RUNNING);
        auction.updateStatus(AuctionStatus.FINISHED);
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    @DisplayName("TC08: FINISHED → PAID hợp lệ")
    void testStateTransition_FinishedToPaid() {
        auction.updateStatus(AuctionStatus.RUNNING);
        auction.updateStatus(AuctionStatus.FINISHED);
        auction.updateStatus(AuctionStatus.PAID);
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    @DisplayName("TC09: OPEN → FINISHED trực tiếp là không hợp lệ → ném IllegalStateException")
    void testStateTransition_OpenToFinished_Invalid() {
        assertThrows(IllegalStateException.class,
                () -> auction.updateStatus(AuctionStatus.FINISHED),
                "OPEN → FINISHED trực tiếp phải ném IllegalStateException");
    }

    @Test
    @DisplayName("TC10: RUNNING → PAID trực tiếp là không hợp lệ → ném IllegalStateException")
    void testStateTransition_RunningToPaid_Invalid() {
        auction.updateStatus(AuctionStatus.RUNNING);
        assertThrows(IllegalStateException.class,
                () -> auction.updateStatus(AuctionStatus.PAID),
                "RUNNING → PAID trực tiếp phải ném IllegalStateException");
    }

    @Test
    @DisplayName("TC11: Đổi trạng thái từ CANCELED (terminal) phải ném IllegalStateException")
    void testStateTransition_FromCanceled_Invalid() {
        auction.updateStatus(AuctionStatus.CANCELED);
        assertThrows(IllegalStateException.class,
                () -> auction.updateStatus(AuctionStatus.RUNNING),
                "Không thể đổi trạng thái từ CANCELED");
    }

    //  recordBid

    @Test
    @DisplayName("TC12: recordBid khi RUNNING → bid được ghi, leadingBidderId cập nhật")
    void testRecordBid_WhenRunning_Success() {
        auction.updateStatus(AuctionStatus.RUNNING);

        Bid bid = new Bid(0, 1, 99, START_PRICE + INCREMENT, LocalDateTime.now());
        auction.setCurrentPrice(bid.getAmount());
        auction.recordBid(bid);

        assertEquals(1, auction.getBids().size(), "Phải có 1 bid trong lịch sử");
        assertEquals(99, auction.getLeadingBidderId(), "leadingBidderId phải cập nhật");
    }

    @Test
    @DisplayName("TC13: recordBid khi OPEN → ném IllegalStateException")
    void testRecordBid_WhenOpen_ThrowsException() {
        Bid bid = new Bid(0, 1, 99, START_PRICE + INCREMENT, LocalDateTime.now());
        assertThrows(IllegalStateException.class,
                () -> auction.recordBid(bid),
                "recordBid khi OPEN phải ném IllegalStateException");
    }

    @Test
    @DisplayName("TC14: recordBid với bid từ auction khác → ném IllegalArgumentException")
    void testRecordBid_WrongAuctionId_ThrowsException() {
        auction.updateStatus(AuctionStatus.RUNNING);
        Bid wrongBid = new Bid(0, 999, 99, START_PRICE + INCREMENT, LocalDateTime.now()); // auctionId=999
        assertThrows(IllegalArgumentException.class,
                () -> auction.recordBid(wrongBid),
                "Bid từ phiên khác phải ném IllegalArgumentException");
    }


    //  setCurrentPrice

    @Test
    @DisplayName("TC15: setCurrentPrice cao hơn → hợp lệ")
    void testSetCurrentPrice_Higher_Success() {
        double newPrice = START_PRICE + INCREMENT;
        auction.setCurrentPrice(newPrice);
        assertEquals(newPrice, auction.getCurrentPrice(), 0.01);
    }

    @Test
    @DisplayName("TC16: setCurrentPrice thấp hơn giá hiện tại → ném IllegalArgumentException")
    void testSetCurrentPrice_Lower_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> auction.setCurrentPrice(START_PRICE - 1),
                "Giá mới thấp hơn giá hiện tại phải ném IllegalArgumentException");
    }

    //  AuctionStatus helpers


    @Test
    @DisplayName("TC17: isActive() đúng cho OPEN và RUNNING")
    void testAuctionStatus_IsActive() {
        assertTrue(AuctionStatus.OPEN.isActive(),    "OPEN phải là active");
        assertTrue(AuctionStatus.RUNNING.isActive(), "RUNNING phải là active");
    }

    @Test
    @DisplayName("TC18: isTerminal() đúng cho FINISHED, PAID, CANCELED")
    void testAuctionStatus_IsTerminal() {
        assertTrue(AuctionStatus.FINISHED.isTerminal(), "FINISHED phải là terminal");
        assertTrue(AuctionStatus.PAID.isTerminal(),     "PAID phải là terminal");
        assertTrue(AuctionStatus.CANCELED.isTerminal(), "CANCELED phải là terminal");
    }

    @Test
    @DisplayName("TC19: isTerminal() sai cho OPEN và RUNNING")
    void testAuctionStatus_NotTerminal() {
        assertFalse(AuctionStatus.OPEN.isTerminal(),    "OPEN không phải terminal");
        assertFalse(AuctionStatus.RUNNING.isTerminal(), "RUNNING không phải terminal");
    }

    //  Observer Pattern

    @Test
    @DisplayName("TC20: Observer nhận được event khi notifyObservers được gọi")
    void testObserver_ReceivesEvent() {
        List<AuctionEvent> received = new ArrayList<>();
        Auction.AuctionObserver observer = received::add;

        auction.addObserver(observer);
        auction.updateStatus(AuctionStatus.RUNNING);

        AuctionEvent event = AuctionEvent.statusChanged(1, AuctionStatus.RUNNING);
        auction.notifyObservers(event);

        assertEquals(1, received.size(), "Observer phải nhận đúng 1 event");
        assertEquals(AuctionEvent.EventType.STATUS_CHANGED, received.get(0).getType());
    }

    @Test
    @DisplayName("TC21: Sau khi removeObserver, không còn nhận event")
    void testObserver_RemoveObserver_NoLongerReceives() {
        List<AuctionEvent> received = new ArrayList<>();
        Auction.AuctionObserver observer = received::add;

        auction.addObserver(observer);
        auction.removeObserver(observer);

        AuctionEvent event = AuctionEvent.statusChanged(1, AuctionStatus.RUNNING);
        auction.notifyObservers(event);

        assertTrue(received.isEmpty(), "Không được nhận event sau khi remove observer");
    }

    @Test
    @DisplayName("TC22: Nhiều observer đều nhận được event")
    void testObserver_MultipleObservers_AllReceive() {
        List<AuctionEvent> list1 = new ArrayList<>();
        List<AuctionEvent> list2 = new ArrayList<>();

        auction.addObserver(list1::add);
        auction.addObserver(list2::add);

        auction.notifyObservers(AuctionEvent.statusChanged(1, AuctionStatus.RUNNING));

        assertEquals(1, list1.size(), "Observer 1 phải nhận event");
        assertEquals(1, list2.size(), "Observer 2 phải nhận event");
    }


    //  Thread safety cơ bản

    @Test
    @DisplayName("TC23: setCurrentPrice đồng thời từ nhiều thread — giá cuối phải là cao nhất")
    void testConcurrency_SetCurrentPrice_NoDirtyWrite() throws InterruptedException {
        auction.updateStatus(AuctionStatus.RUNNING);

        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        // Mỗi thread đặt một giá khác nhau (đều cao hơn START_PRICE)
        for (int i = 1; i <= threadCount; i++) {
            final double price = START_PRICE + INCREMENT * i;
            pool.submit(() -> {
                try {
                    latch.await();
                    // Dùng lock của auction để tránh race condition
                    auction.getLock().lock();
                    try {
                        if (price > auction.getCurrentPrice()) {
                            auction.setCurrentPrice(price);
                        }
                    } finally {
                        auction.getLock().unlock();
                    }
                } catch (InterruptedException ignored) {}
            });
        }

        latch.countDown(); // Bắt đầu tất cả thread đồng thời
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        double expectedMax = START_PRICE + INCREMENT * threadCount; // giá cao nhất
        assertEquals(expectedMax, auction.getCurrentPrice(), 0.01,
                "Sau khi nhiều thread đặt giá, giá phải là cao nhất không bị lost update");
    }

    @Test
    @DisplayName("TC24: deposit đồng thời từ nhiều thread — số dư phải cộng đúng (không mất)")
    void testConcurrency_Deposit_NoLostUpdate() throws Exception {
        RegularUser user = new RegularUser(1, "tester", "pass", 0, Role.BIDDER);

        int threadCount = 50;
        double depositEach = 10_000;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    user.deposit(depositEach);
                } catch (InterruptedException ignored) {}
            });
        }

        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        double expected = depositEach * threadCount; // 500_000
        assertEquals(expected, user.getBalance(), 0.01,
                "Số dư phải cộng đúng khi nhiều thread deposit đồng thời (deposit() là synchronized)");
    }
}