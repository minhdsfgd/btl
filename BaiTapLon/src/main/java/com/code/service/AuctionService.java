package com.code.service;

import com.code.exception.AuctionClosedException;
import com.code.exception.UserBannedException;
import com.code.models.*;
import com.code.repository.AuctionRepository;
import com.code.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý vòng đời phiên đấu giá — Singleton (double-checked locking).
 * KHÔNG xử lý logic đặt giá — việc đó là của {@link BidService}.
 */
public class AuctionService {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile AuctionService instance;

    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) instance = new AuctionService();
            }
        }
        return instance;
    }

    private AuctionService() {
        this.auctionRepository = new AuctionRepository();
        startScheduler();
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final AuctionRepository        auctionRepository;
    private final ReentrantLock            managerLock = new ReentrantLock();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-scheduler");
                t.setDaemon(true);
                return t;
            });

    // ── Tạo phiên ─────────────────────────────────────────────────────────────

    public Auction createAuction(Item item, User seller,
                                 double bidIncrement,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime)
            throws UserBannedException, AuctionClosedException {

        if (seller.isBanned()) throw new UserBannedException(seller.getUsername());
        if (!seller.hasRole(Role.SELLER))
            throw new AuctionClosedException("Cần vai trò SELLER để tạo phiên đấu giá.");

        managerLock.lock();
        try {
            Auction auction = new Auction(
                    IdGenerator.getId(), item, seller.getUserId(),
                    item.getStartingPrice(), bidIncrement, startTime, endTime
            );
            auctionRepository.save(auction);
            return auction;
        } finally {
            managerLock.unlock();
        }
    }

    // ── Truy vấn ─────────────────────────────────────────────────────────────

    /**
     * FIX: Ném AuctionClosedException (checked) thay vì IllegalArgumentException (unchecked).
     * Caller được nhắc nhở phải xử lý trường hợp không tìm thấy phiên.
     */
    public Auction getAuction(int auctionId) throws AuctionClosedException {
        Auction a = auctionRepository.findAuctionById(auctionId);
        if (a == null)
            throw new AuctionClosedException("Không tìm thấy phiên đấu giá #" + auctionId);
        return a;
    }

    public List<Auction> getActiveAuctions() { return auctionRepository.findActiveAuctions(); }
    public List<Auction> getAllAuctions()     { return auctionRepository.findAll(); }
    public List<Auction> getAuctionsBySeller(int sellerId) {
        return auctionRepository.findBySellerId(sellerId);
    }

    // ── Thay đổi trạng thái ──────────────────────────────────────────────────

    public void startAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (requester.isBanned()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "bắt đầu");
        managerLock.lock();
        try {
            auction.updateStatus(AuctionStatus.RUNNING);
            auction.notifyObservers(
                    AuctionEvent.statusChanged(auctionId, AuctionStatus.RUNNING));
        } finally { managerLock.unlock(); }
    }

    /**
     * FIX: Dùng AuctionEvent.auctionFinished() thay vì Bid giả bidId=-1.
     */
    public void finishAuction(int auctionId) {
        Auction auction = auctionRepository.findAuctionById(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

        managerLock.lock();
        try {
            auction.updateStatus(AuctionStatus.FINISHED);
            markAsPaid(auctionId);
            // Gửi event kết thúc — observer phân biệt qua EventType.AUCTION_FINISHED
            auction.notifyObservers(
                    AuctionEvent.auctionFinished(auctionId, auction.getLeadingBidderId()));
        } catch (AuctionClosedException e) {
            throw new RuntimeException(e);
        } finally { managerLock.unlock(); }
    }

    public void cancelAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (requester.isBanned()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "hủy");
        managerLock.lock();
        try {
            auction.updateStatus(AuctionStatus.CANCELED);
            auction.notifyObservers(AuctionEvent.auctionCanceled(auctionId));
        } finally { managerLock.unlock(); }
    }

    public void markAsPaid(int auctionId)
            throws AuctionClosedException {
        managerLock.lock();
        try {
            Auction auction = getAuction(auctionId);

            if (auction.getStatus() != AuctionStatus.FINISHED) {
                throw new AuctionClosedException(
                        "Auction phải ở trạng thái FINISHED."
                );
            }

            auction.updateStatus(AuctionStatus.PAID);

        } finally {
            managerLock.unlock();
        }
    }

    public void banAuction(int auctionId, User admin)
            throws UserBannedException, AuctionClosedException {
        if (admin.isBanned()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuctionClosedException("Chỉ Admin được ban phiên đấu giá.");
        getAuction(auctionId).setBanned(true);
    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                for (Auction a : auctionRepository.findAll()) {

                    // OPEN → RUNNING
                    if (a.getStatus() == AuctionStatus.OPEN
                            && !a.isBanned()
                            && a.getStartTime() != null
                            && !now.isBefore(a.getStartTime())) {
                        managerLock.lock();
                        try {
                            a.updateStatus(AuctionStatus.RUNNING);
                            a.notifyObservers(
                                    AuctionEvent.statusChanged(a.getAuctionId(), AuctionStatus.RUNNING));
                        } catch (Exception e) {
                            // FIX: log lỗi thay vì bỏ qua im lặng
                            System.err.println("[Scheduler] OPEN→RUNNING phiên #"
                                    + a.getAuctionId() + ": " + e.getMessage());
                        } finally { managerLock.unlock(); }
                    }

                    // RUNNING → FINISHED
                    if (a.getStatus() == AuctionStatus.RUNNING
                            && a.getEndTime() != null
                            && now.isAfter(a.getEndTime())) {
                        finishAuction(a.getAuctionId());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Lỗi không xác định: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void shutdown() { scheduler.shutdownNow(); }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void requireOwnerOrAdmin(Auction auction, User user, String action)
            throws AuctionClosedException {
        boolean isOwner = auction.getSellerId() == user.getUserId();
        boolean isAdmin = user.hasRole(Role.ADMIN);
        if (!isOwner && !isAdmin)
            throw new AuctionClosedException(
                    "Không có quyền " + action + " phiên #" + auction.getAuctionId() + ".");
    }
}