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
 * Quản lý vòng đời phiên đấu giá — Singleton.
 *
 * <p><b>Vai trò:</b>
 * <ul>
 *   <li>Tạo / bắt đầu / kết thúc / hủy phiên đấu giá</li>
 *   <li>Scheduler tự động chuyển OPEN→RUNNING→FINISHED mỗi 30 giây</li>
 *   <li>Kiểm tra quyền Seller/Admin trước khi cho phép thao tác</li>
 * </ul>
 * </p>
 *
 * <p><b>Singleton — thread-safe</b> với double-checked locking.</p>
 *
 * <p><b>KHÔNG</b> xử lý logic đặt giá — việc đó là của {@link BidService}.</p>
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

    private final AuctionRepository     auctionRepository;
    private final ReentrantLock         managerLock = new ReentrantLock();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "auction-scheduler");
                t.setDaemon(true); // tự tắt khi JVM tắt
                return t;
            });

    // ── Tạo phiên ─────────────────────────────────────────────────────────────

    /**
     * Seller tạo phiên đấu giá mới (OPEN).
     *
     * @param item         sản phẩm đấu giá
     * @param seller       người tạo phiên — phải có role SELLER, không bị ban
     * @param bidIncrement bước giá tối thiểu mỗi lần đặt
     * @param startTime    thời điểm bắt đầu nhận bid
     * @param endTime      thời điểm kết thúc
     */
    public Auction createAuction(Item item, User seller,
                                 double bidIncrement,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime)
            throws UserBannedException, AuctionClosedException {

        if (seller.isBanned())
            throw new UserBannedException(seller.getUsername());
        if (!seller.hasRole(Role.SELLER))
            throw new AuctionClosedException(
                    "Tài khoản không có quyền tạo phiên. Cần vai trò SELLER.");

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
     * Lấy phiên theo ID. Ném IllegalArgumentException nếu không tìm thấy.
     */
    public Auction getAuction(int auctionId) {
        Auction a = auctionRepository.findAuctionById(auctionId);
        if (a == null)
            throw new IllegalArgumentException("Không tìm thấy phiên #" + auctionId);
        return a;
    }

    /** OPEN + RUNNING — Bidder xem danh sách. */
    public List<Auction> getActiveAuctions() {
        return auctionRepository.findActiveAuctions();
    }

    /** Tất cả phiên — Admin quản lý. */
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    /** Phiên của một Seller cụ thể — Seller dashboard. */
    public List<Auction> getAuctionsBySeller(int sellerId) {
        return auctionRepository.findBySellerId(sellerId);
    }

    // ── Thay đổi trạng thái ──────────────────────────────────────────────────

    /**
     * Bắt đầu phiên sớm (OPEN → RUNNING).
     * Chỉ chủ phiên (Seller) hoặc Admin.
     */
    public void startAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (requester.isBanned()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "bắt đầu");

        managerLock.lock();
        try { auction.updateStatus(AuctionStatus.RUNNING); }
        finally { managerLock.unlock(); }
    }

    /**
     * Kết thúc phiên (RUNNING → FINISHED) — gọi bởi scheduler hoặc Admin.
     * Sau FINISHED: leadingBidderId là người thắng.
     */
    public void finishAuction(int auctionId) {
        Auction auction = auctionRepository.findAuctionById(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

        managerLock.lock();
        try {
            auction.updateStatus(AuctionStatus.FINISHED);
            // Notify client biết phiên kết thúc (dùng bid giả với bidId = -1)
            Bid endSignal = new Bid(-1, auctionId,
                    auction.getLeadingBidderId(),
                    auction.getCurrentPrice(),
                    LocalDateTime.now());
            auction.notifyObservers(endSignal);
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Hủy phiên (OPEN/RUNNING → CANCELED).
     * Chỉ chủ phiên hoặc Admin.
     */
    public void cancelAuction(int auctionId, User requester)
            throws UserBannedException, AuctionClosedException {
        if (requester.isBanned()) throw new UserBannedException(requester.getUsername());
        Auction auction = getAuction(auctionId);
        requireOwnerOrAdmin(auction, requester, "hủy");

        managerLock.lock();
        try { auction.updateStatus(AuctionStatus.CANCELED); }
        finally { managerLock.unlock(); }
    }

    /**
     * Xác nhận thanh toán (FINISHED → PAID). Chỉ Admin.
     */
    public void markAsPaid(int auctionId, User admin)
            throws UserBannedException, AuctionClosedException {
        if (admin.isBanned()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuctionClosedException("Chỉ Admin được xác nhận thanh toán.");
        managerLock.lock();
        try { getAuction(auctionId).updateStatus(AuctionStatus.PAID); }
        finally { managerLock.unlock(); }
    }

    /**
     * Admin ban phiên đấu giá — phiên bị lock, không nhận bid mới.
     */
    public void banAuction(int auctionId, User admin)
            throws UserBannedException, AuctionClosedException {
        if (admin.isBanned()) throw new UserBannedException(admin.getUsername());
        if (!admin.hasRole(Role.ADMIN))
            throw new AuctionClosedException("Chỉ Admin được ban phiên đấu giá.");
        getAuction(auctionId).setBanned(true);
    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    /**
     * Quét mỗi 30 giây, tự động chuyển trạng thái theo thời gian:
     * OPEN → RUNNING (khi đến startTime)
     * RUNNING → FINISHED (khi qua endTime)
     */
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
                        try { a.updateStatus(AuctionStatus.RUNNING); }
                        catch (Exception ignored) {}
                        finally { managerLock.unlock(); }
                    }
                    // RUNNING → FINISHED
                    if (a.getStatus() == AuctionStatus.RUNNING
                            && a.getEndTime() != null
                            && now.isAfter(a.getEndTime())) {
                        finishAuction(a.getAuctionId());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    /** Dừng scheduler khi server shutdown. */
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