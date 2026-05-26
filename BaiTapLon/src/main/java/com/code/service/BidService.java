package com.code.service;

import com.code.dao.*;
import com.code.exception.*;
import com.code.models.*;
import com.code.service.TransactionService;


import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Xử lý nghiệp vụ đặt giá và nạp tiền.
 *
 * <p>Thứ tự validate trong {@link #placeBid}:
 * <ol>
 *   <li>User bị ban?                  → UserBannedException</li>
 *   <li>Thiếu role BIDDER?            → InvalidBidException</li>
 *   <li>Bid sản phẩm của chính mình?  → SelfBidException</li>
 *   <li>Phiên không RUNNING?          → AuctionClosedException</li>
 *   <li>Phiên bị Admin ban?           → AuctionClosedException</li>
 *   <li>Số tiền < giá tối thiểu?   → InvalidBidException</li>
 *   <li>Số dư không đủ?               → InsufficientBalanceException</li>
 * </ol>
 * Bước 4–7 nằm trong ReentrantLock để tránh race condition.</p>
 */
public class BidService {

    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final TransactionService txService;

    /** Constructor — inject BidRepository. */
    public BidService(BidDAO bidDAO, AuctionDAO auctionDAO, UserDAO userDAO,
                      TransactionService txService) {
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.txService = txService;
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────

    /**
     * Đặt giá vào phiên đấu giá.
     *
     * @return Bid vừa tạo nếu thành công
     */
    public Bid placeBid(User user, Auction auction, double amount)
            throws UserBannedException, InvalidBidException,
            SelfBidException, AuctionClosedException,
            InsufficientBalanceException {

        // 1. User bị ban
        if (!user.isActive())
            throw new UserBannedException(user.getUsername());

        // 2. Thiếu role BIDDER
        if (!user.hasRole(Role.BIDDER))
            throw new InvalidBidException(
                    "Tài khoản không có quyền đặt giá. Vui lòng đăng ký vai trò Bidder.");

        // 3. Không bid sản phẩm của mình
        if (auction.getSellerId() == user.getUserId())
            throw new SelfBidException();

        // 4–7: Validate trong lock
        auction.getLock().lock();
        try {
            // 4. Phiên phải RUNNING
            if (!auction.getStatus().isActive())
                throw new AuctionClosedException(
                        "Phiên #" + auction.getAuctionId() + " không nhận giá. "
                                + "Trạng thái hiện tại: " + auction.getStatus() + ". "
                                + "Chỉ phiên ở trạng thái RUNNING mới nhận giá.");

            // 5. Phiên không bị Admin ban
            if (auction.isBanned())
                throw new AuctionClosedException(
                        "Phiên #" + auction.getAuctionId() + " đã bị Admin khoá.");

            // 6. Số tiền tối thiểu
            double minRequired = auction.getCurrentPrice() + auction.getBidIncrement();
            if (amount < minRequired)
                throw new InvalidBidException(String.format(
                        "Giá tối thiểu: %,.0f VNĐ (hiện tại %,.0f + bước %,.0f)",
                        minRequired, auction.getCurrentPrice(), auction.getBidIncrement()));



            // ── Tất cả hợp lệ ─────────────────────────────────────────────────

            LocalDateTime now = LocalDateTime.now();

            Bid bid = new Bid(
                    0,
                    auction.getAuctionId(),
                    user.getUserId(),
                    amount,
                    LocalDateTime.now()
            );

            long secondsLeft = ChronoUnit.SECONDS.between(now, auction.getEndTime());
            if (secondsLeft >= 0 && secondsLeft <= 10) {
                // Nếu còn <= 10 giây, tăng endTime thêm 1 phút
                auction.setEndTime(auction.getEndTime().plusMinutes(1));
                auction.notifyObservers(AuctionEvent.timeExtended(auction.getAuctionId()));

                // Cập nhật sự thay đổi này xuống Database
                try {
                    auctionDAO.update(auction);
                } catch (SQLException e) {
                    System.err.println("[Anti-sniping] Lỗi cập nhật endTime: " + e.getMessage());
                }
            }

// ===== Xử lý giữ tiền bidder =====

            int currentLeaderId = auction.getLeadingBidderId();
            double oldPrice = auction.getCurrentPrice();

            // Capture trước khi recordBid() thay đổi leadingBidderId
            boolean isSelfOutbid = (currentLeaderId == user.getUserId());

// ================================
// CASE 1: Tự vượt giá chính mình
// ================================
            if (currentLeaderId == user.getUserId()) {

                // Chỉ trừ thêm phần chênh lệch (không hoàn + trừ lại toàn bộ)
                double extraNeeded = (amount - oldPrice)*auction.getRatio();

                if (user.getBalance() < extraNeeded) {
                    throw new InsufficientBalanceException(
                            "Không đủ số dư để tăng giá thêm " + extraNeeded/auction.getRatio() + " VNĐ");
                }

                user.deductBalance(extraNeeded);

                // Ghi log: giữ thêm tiền chênh lệch
                txService.logBidHold(
                        user.getUserId(),
                        extraNeeded,
                        auction.getAuctionId()
                );
            }

// ================================
// CASE 2: Người khác bị vượt giá
// ================================
            else {
                // Hoàn tiền leader cũ
                if (currentLeaderId != -1) {
                    try {
                        User prevLeader = userDAO.findById(currentLeaderId);

                        if (prevLeader != null) {
                            prevLeader.deposit(oldPrice*auction.getRatio());
                            userDAO.update(prevLeader);

                            // Ghi log: hoàn tiền cho người bị vượt giá
                            txService.logRefund(
                                    prevLeader.getUserId(),
                                    oldPrice*auction.getRatio(),
                                    auction.getAuctionId()
                            );
                        }

                    } catch (SQLException e) {
                        throw new RuntimeException(
                                "Lỗi hoàn tiền người dẫn đầu trước",
                                e
                        );
                    }
                }

                // Kiểm tra bidder mới đủ tiền
                if (user.getBalance() < amount*auction.getRatio()) {
                    throw new InsufficientBalanceException("Không đủ số dư");
                }

                // Giữ tiền bidder mới
                user.deductBalance(amount*auction.getRatio());

                // Ghi log: giữ tiền bid mới
                txService.logBidHold(
                        user.getUserId(),
                        amount*auction.getRatio(),
                        auction.getAuctionId()
                );
            }
            // Update auction
            auction.setCurrentPrice(amount);
            auction.recordBid(bid);
            try {
                // Lưu bid
                bidDAO.save(bid);

                // Sync auction
                auctionDAO.update(auction);

                // QUAN TRỌNG:
                // Sync balance bidder mới xuống DB
                userDAO.update(user);

            } catch (SQLException e) {
                // Rollback balance trong bộ nhớ nếu DB lỗi
                if (isSelfOutbid) {
                    user.deposit(amount - oldPrice); // hoàn phần extraNeeded đã trừ
                } else {
                    user.deposit(amount);            // hoàn toàn bộ amount đã trừ
                }
                throw new RuntimeException("Lỗi lưu bid: " + e.getMessage(), e);
            }

// ── AUTO BID LOGIC ─────────────────────────────────────────────
            // Nếu có auto bid và bidder hiện tại KHÔNG phải auto-bidder
            if (auction.hasAutoBid() && user.getUserId() != auction.getAutoBidUserId()) {
                int autoBidderId = auction.getAutoBidUserId();
                double autoBidMax = auction.getAutoBidMaxAmount();
                double autoBidStep = auction.getAutoBidStep();
                double nextAutoBidAmount = amount + autoBidStep;

                // Nếu auto bid vẫn còn dưới giá trần → tự động đặt giá
                if (nextAutoBidAmount <= autoBidMax) {
                    try {
                        User autoBidder = userDAO.findById(autoBidderId);
                        if (autoBidder != null && autoBidder.isActive() &&
                                autoBidder.getBalance() >= nextAutoBidAmount * auction.getRatio()) {

                            // TỰA ĐỘNG CALL RECURSION: đặt giá tự động
                            // ⚠️ CẢNH CÁO: Để tránh infinite recursion, ta kiểm tra điều kiện trên
                            placeBid(autoBidder, auction, nextAutoBidAmount);
                        } else {
                            // Auto bidder không đủ tiền → tắt auto bid
                            auction.clearAutoBid();
                        }
                    } catch (Exception e) {
                        System.err.println("[Auto Bid] Lỗi: " + e.getMessage());
                        auction.clearAutoBid();
                    }
                } else {
                    // Giá đã vượt trần → tắt auto bid
                    auction.clearAutoBid();
                }
            }

            // ── Notify qua AuctionEvent (không dùng Bid giả) ──────────────────
            auction.notifyObservers(AuctionEvent.bidPlaced(auction.getAuctionId(), bid));

            return bid;

        } finally {
            auction.getLock().unlock();
        }
    }




}