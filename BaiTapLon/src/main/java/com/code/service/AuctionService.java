package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.UserDAO;
import com.code.exception.AuctionClosedException;
import com.code.exception.AuthenticationException;
import com.code.exception.InsufficientBalanceException;
import com.code.exception.UserBannedException;
import com.code.models.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.code.models.AuctionStatus.FINISHED;

/**
 * Quản lý vòng đời phiên đấu giá — Singleton (double-checked locking).
 * KHÔNG xử lý logic đặt giá — việc đó là của {@link BidService}.
 */
public class AuctionService {
    private final ConcurrentHashMap<Integer, Auction> liveAuctions = new ConcurrentHashMap<>();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile AuctionService instance;
    private static AuctionDAO auctionDAO_holder;
    private static UserDAO userDAO_holder;
    private static TransactionService txService_holder;

    public static void init(AuctionDAO auctionDAO, UserDAO userDAO, TransactionService txService) {
        auctionDAO_holder = auctionDAO;
        userDAO_holder = userDAO;
        txService_holder = txService;
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService(auctionDAO_holder, userDAO_holder, txService_holder);
                }
            }
        }
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final TransactionService txService;
    private final ReentrantLock managerLock = new ReentrantLock();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-scheduler");
                t.setDaemon(true);
                return t;
            });

    // ── Constructor ────────────────────────────────────────────────────────────

    private AuctionService(AuctionDAO auctionDAO, UserDAO userDAO, TransactionService txService) {
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.txService = txService;
        startScheduler();
    }

    // ── Tạo phiên ─────────────────────────────────────────────────────────────

    public Auction createAuction(Item item, User seller,
                                 double bidIncrement,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime)
            throws Exception {

        if (!seller.isActive()) throw new UserBannedException(seller.getUsername());
        if (!seller.hasRole(Role.SELLER))
            throw new AuctionClosedException("Cần vai trò SELLER để tạo phiên đấu giá.");
        if (item.getSellerId() != seller.getUserId()) {
            throw new Exception("Bạn không phải chủ sở hữu của sản phẩm này!");
        }

        // 2. [FIX BUG]: Kiểm tra xem vật phẩm đã bị khóa trong phiên khác chưa
        if (auctionDAO.isItemLocked(item.getItemId())) {
            throw new Exception("Sản phẩm này đang được đấu giá hoặc đã bán ở một phiên khác!");
        }
        managerLock.lock();
        try {
            Auction auction = new Auction(
                    0, item, seller.getUserId(),
                    item.getStartingPrice(), bidIncrement, startTime, endTime
            );
            auctionDAO.save(auction);
            // Inside createAuction(), after auctionDAO.save(auction):
            liveAuctions.put(auction.getAuctionId(), auction);

            return auction;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
        finally {
            managerLock.unlock();
        }
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    public Auction getAuction(int auctionId) throws AuctionClosedException {
        // Return cached live instance if present (preserves observers)
        Auction cached = liveAuctions.get(auctionId);
        if (cached != null) return cached;

        try {
            Auction a = auctionDAO.findById(auctionId);
            if (a == null)
                throw new AuctionClosedException("Không tìm thấy phiên đấu giá #" + auctionId);
            liveAuctions.put(auctionId, a);
            return a;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    public List<Auction> getActiveAuctions() {
        try{
            return auctionDAO.findActiveAuctions();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    public List<Auction> getAllAuctions() {
        try{
            return auctionDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    public List<Auction> getAuctionsBySeller(int sellerId) {
        try{
            return auctionDAO.findBySellerId(sellerId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm các phiên RUNNING trong liveAuctions có leadingBidderId == userId.
     * Dùng khi ban bidder — để hủy phiên mà họ đang dẫn đầu.
     */
    public List<Auction> getRunningAuctionsWhereLeading(int userId) {
        return liveAuctions.values().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                        && a.getLeadingBidderId() == userId)
                .collect(java.util.stream.Collectors.toList());
    }

    // ── Thay đổi trạng thái ──────────────────────────────────────────────────

    public void startAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (!requester.isActive()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "bắt đầu");
        managerLock.lock();
        try {
            auction.updateStatus(AuctionStatus.RUNNING);
            auctionDAO.update(auction);   // ← Lưu vào DB
            auction.notifyObservers(
                    AuctionEvent.statusChanged(auctionId, AuctionStatus.RUNNING));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi bắt đầu phiên: " + e.getMessage(), e);
        } finally { managerLock.unlock(); }
    }

    private void finishAuction(int auctionId) {
        try {
            Auction auction = liveAuctions.get(auctionId);
            if (auction == null) {auction = auctionDAO.findById(auctionId);}
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;
            managerLock.lock();
            try {
                int winnerId = auction.getLeadingBidderId();
                if (winnerId == -1){
                    cancelAuction(auctionId,userDAO.findById(auction.getSellerId()));
                    return;
                }
                auction.updateStatus(FINISHED);
                auctionDAO.update(auction);
                liveAuctions.remove(auctionId);


                try {
                    User seller = userDAO.findById(auction.getSellerId());
                    if (seller != null) {
                        double winningAmount = auction.getCurrentPrice()*Auction.getRatio();
                        seller.deposit(winningAmount);
                        userDAO.update(seller);
                        txService.logPaymentToSeller(
                                winnerId,
                                seller.getUserId(),
                                winningAmount,
                                auctionId
                        );
                        System.out.println("[Auction] Phan #" + auctionId
                                + ": chuyen " + winningAmount
                                + " VND tu bidder #" + winnerId
                                + " -> seller #" + seller.getUserId());
                    }
                } catch (Exception e) {
                    System.err.println("[Auction] Loi thanh toan phan #"
                            + auctionId + ": " + e.getMessage());
                }


                auction.notifyObservers(
                        AuctionEvent.auctionFinished(auctionId, winnerId));
            } catch (AuctionClosedException e) {
                throw new RuntimeException(e);
            } catch (UserBannedException e) {
                throw new UserBannedException("user banned");
            } finally {
                managerLock.unlock();
            }
        } catch (SQLException | UserBannedException e) {
            System.err.println("[Scheduler] Loi ket thuc phan #" + auctionId + ": " + e.getMessage());
        }
    }

    public void cancelAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (!requester.isActive()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "hủy");

        managerLock.lock();
        try {
            // 1. Lấy thông tin người dẫn đầu và số tiền cần hoàn
            int leadingBidderId = auction.getLeadingBidderId();
            double refundAmount = auction.getCurrentPrice()*Auction.getRatio();

            // 2. Cập nhật trạng thái hủy
            auction.updateStatus(AuctionStatus.CANCELED);
            auctionDAO.update(auction);   // ← Lưu vào DB

            // 3. Xử lý hoàn tiền cho người dẫn đầu (nếu có)
            if (leadingBidderId != -1) {
                try {
                    User leadingBidder = userDAO.findById(leadingBidderId);
                    if (leadingBidder != null) {
                        leadingBidder.deposit(refundAmount); // Trả lại tiền vào ví
                        userDAO.update(leadingBidder);       // Cập nhật DB

                        // Tuỳ chọn: Ghi log hoàn tiền nếu TransactionService có hỗ trợ
                        // txService.logRefund(leadingBidderId, refundAmount, auctionId);

                        System.out.println("[Auction] Phiên #" + auctionId
                                + " bị hủy: Đã hoàn " + refundAmount
                                + " VND cho bidder #" + leadingBidderId);
                    }
                } catch (Exception e) {
                    // Try-catch riêng để đảm bảo lỗi hoàn tiền không làm crash toàn bộ luồng hủy phiên
                    System.err.println("[Auction] Lỗi hoàn tiền khi hủy phiên #"
                            + auctionId + ": " + e.getMessage());
                }
            }

            // 4. Dọn dẹp cache và thông báo
            liveAuctions.remove(auction.getAuctionId());
            auction.notifyObservers(AuctionEvent.auctionCanceled(auctionId));

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi hủy phiên: " + e.getMessage(), e);
        } finally {
            managerLock.unlock();
        }
    }

    public void markAsPaid(int auctionId)
            throws AuctionClosedException, UserBannedException, AuthenticationException, InsufficientBalanceException{
        managerLock.lock();
        try {
            Auction auction = liveAuctions.get(auctionId);
            if  (auction == null) {auction = getAuction(auctionId);}


            if (auction.getStatus() != FINISHED) {
                throw new AuctionClosedException(
                        "Auction phải ở trạng thái FINISHED."
                );
            }

            User bidder = userDAO.findById(auction.getLeadingBidderId());
            User seller = userDAO.findById(auction.getSellerId());

            if (bidder == null || seller == null) {
                throw new IllegalArgumentException("Không tìm thấy thông tin bidder hoặc seller.");
            }
            
            if (bidder.equals(seller)) {throw new AuthenticationException("Bidder và seller là cùng 1 người");
            }
            if (!bidder.isActive()) throw new UserBannedException(bidder.getUsername());
            if (!seller.isActive()) throw new UserBannedException(seller.getUsername());

            double amount = auction.getCurrentPrice();
            double actualPayAmount = amount * (1 - Auction.getRatio());

            if (bidder.getBalance() < amount*(1-Auction.getRatio())) {
                throw new InsufficientBalanceException("Tài khoản ko đủ số dư");
            }
            // --- BẮT ĐẦU PHẦN SỬA LỖI ---
            // Cập nhật số dư trực tiếp trên object User
            bidder.setBalance(bidder.getBalance() - actualPayAmount);
            seller.setBalance(seller.getBalance() + actualPayAmount);

            // Lưu trực tiếp xuống Database qua DAO (Server-side)
            userDAO.update(bidder);
            userDAO.update(seller);

            auction.updateStatus(AuctionStatus.PAID);
            auctionDAO.update(auction);   // ← Lưu vào DB
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi đánh dấu đã thanh toán: " + e.getMessage(), e);
        } finally {
            managerLock.unlock();
        }
    }

    public void banAuction(int auctionId, User admin)
            throws UserBannedException, AuctionClosedException {
        if (!admin.isActive()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuctionClosedException("Chỉ Admin được ban phiên đấu giá.");
        Auction  auction = getAuction(auctionId);
        auction.setBanned(true);
        try{
            auctionDAO.update(auction);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi ban phiên: " + e.getMessage(), e);
        }

    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    // Dòng 214-247 trong AuctionService.java
    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                for (Auction a : auctionDAO.findAll()) {

                    // OPEN → RUNNING

                    if (a.getStatus() == AuctionStatus.OPEN
                            && !a.isBanned()
                            && a.getStartTime() != null
                            && !now.isBefore(a.getStartTime())) {
                        Auction live = liveAuctions.computeIfAbsent(a.getAuctionId(), id -> a);
                        managerLock.lock();
                        try {
                            live.updateStatus(AuctionStatus.RUNNING);
                            auctionDAO.update(live);
                            live.notifyObservers(AuctionEvent.statusChanged(live.getAuctionId(), AuctionStatus.RUNNING));
                        } catch (Exception e) {
                            System.err.println("[Scheduler] OPEN→RUNNING phiên #"
                                    + a.getAuctionId() + ": " + e.getMessage());
                        } finally { managerLock.unlock(); }
                    }

                    // RUNNING → FINISHED
                    if (a.getStatus() == AuctionStatus.RUNNING
                            && a.getEndTime() != null
                            && now.isAfter(a.getEndTime())) {
                        Auction live = liveAuctions.getOrDefault(a.getAuctionId(), a);
                        finishAuction(live.getAuctionId()); // finishAuction already calls auctionDAO.findById internally
                    }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi không xác định: " + e.getMessage());
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void shutdown() { scheduler.shutdownNow(); }

    // ── Helper ───────────────────────────────────────────────────────────────
    //TODO: Move to AuthGuard
    private void requireOwnerOrAdmin(Auction auction, User user, String action)
            throws AuctionClosedException {
        boolean isOwner = auction.getSellerId() == user.getUserId();
        boolean isAdmin = user.hasRole(Role.ADMIN);
        if (!isOwner && !isAdmin)
            throw new AuctionClosedException(
                    "Không có quyền " + action + " phiên #" + auction.getAuctionId() + ".");
    }
    //update auction
    public void updateAuction(Auction auction) throws SQLException {
        auctionDAO.update(auction);
    }

}