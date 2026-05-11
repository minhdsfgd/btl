package com.code.service;

import com.code.dao.AuctionDAO;
import com.code.dao.BidDAO;
import com.code.dao.UserDAO;
import com.code.exception.*;
import com.code.models.*;


import java.sql.SQLException;
import java.time.LocalDateTime;

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

    /** Constructor — inject BidRepository. */
    public BidService(BidDAO bidDAO,AuctionDAO auctionDAO, UserDAO userDAO) {
        this.bidDAO= bidDAO;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
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
        if (user.isBanned())
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
            Bid bid = new Bid(
                    0,
                    auction.getAuctionId(),
                    user.getUserId(),
                    amount,
                    LocalDateTime.now()
            );

            auction.setCurrentPrice(amount);
            auction.recordBid(bid);
            user.deductBalance(amount);  // trừ tiền trong memory

            try {

                bidDAO.save(bid);
                auctionDAO.update(auction); // sync giá + leadingBidderId về DB
            } catch (SQLException e) {
                throw new RuntimeException("Lỗi lưu bid: " + e.getMessage(), e);
            }


            // ── Notify qua AuctionEvent (không dùng Bid giả) ──────────────────
            auction.notifyObservers(AuctionEvent.bidPlaced(auction.getAuctionId(), bid));

            return bid;

        } finally {
            auction.getLock().unlock();
        }
    }

    // ── Nạp tiền ──────────────────────────────────────────────────────────────

    /**
     * Nạp tiền vào tài khoản.
     *
     * @throws UserBannedException   nếu tài khoản bị ban
     * @throws IllegalArgumentException nếu amount <= 0  (unchecked — lỗi lập trình viên)
     */
    public Transaction deposit(User user, double amount) throws UserBannedException {
        if (user.isBanned())
            throw new UserBannedException(user.getUsername());
        if (amount <= 0)
            // FIX: dùng IllegalArgumentException thay vì InvalidBidException
            throw new IllegalArgumentException("Số tiền nạp phải > 0 VNĐ.");

        user.deposit(amount);
        return Transaction.deposit(0, user.getUserId(), amount);
    }
}