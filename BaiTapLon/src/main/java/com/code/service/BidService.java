package com.code.service;
import com.code.exception.*;
import com.code.models.*;
import com.code.util.IdGenerator;

import java.time.LocalDateTime;

public class BidService {

    /**
     * Đặt giá vào phiên đấu giá.
     *
     * @param user    người đặt giá
     * @param auction phiên đấu giá muốn tham gia
     * @param amount  số tiền muốn đặt (VNĐ)
     */
    public void placeBid(User user, Auction auction, double amount)
            throws UserBannedException,
            InvalidBidException,
            SelfBidException,
            AuctionClosedException,
            InsufficientBalanceException {

        // ── 1. Kiểm tra user bị ban ──────────────────────────────────────────
        if (user.isBanned()) {
            throw new UserBannedException(user.getUsername());
        }

        // ── 2. Kiểm tra quyền BIDDER ─────────────────────────────────────────
        if (!user.hasRole(Role.BIDDER)) {
            throw new InvalidBidException(
                    "Tài khoản không có quyền đặt giá. Vui lòng đăng ký vai trò Bidder.");
        }

        // ── 3. Không được bid sản phẩm của chính mình ────────────────────────
        if (auction.getSellerId() == user.getUserId()) {
            throw new SelfBidException();
        }

        // ── 4 & 5 & 6 & 7: Validate trong lock để tránh race condition ───────
        auction.getLock().lock();
        try {
            // 4. Phiên phải đang RUNNING
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        "Phiên đấu giá #" + auction.getAuctionId() + " không còn nhận giá "
                                + "(trạng thái: " + auction.getStatus() + ").");
            }

            // 5. Phiên không bị banned bởi Admin
            if (auction.isBanned()) {
                throw new AuctionClosedException(
                        "Phiên đấu giá #" + auction.getAuctionId() + " đã bị khoá bởi Admin.");
            }

            // 6. Số tiền phải >= currentPrice + bidIncrement
            double minRequired = auction.getCurrentPrice() + auction.getBidIncrement();
            if (amount < minRequired) {
                throw new InvalidBidException(String.format(
                        "Giá đặt tối thiểu là %,.0f VNĐ (hiện tại: %,.0f + bước giá: %,.0f).",
                        minRequired, auction.getCurrentPrice(), auction.getBidIncrement()));
            }

            // 7. Số dư trong tài khoản phải đủ
            if (user.getBalance() < amount) {
                throw new InsufficientBalanceException(String.format(
                        "Số dư không đủ: cần %,.0f VNĐ, tài khoản có %,.0f VNĐ.",
                        amount, user.getBalance()));
            }

            // ── Tất cả hợp lệ → tạo bid ──────────────────────────────────────
            Bid bid = new Bid(
                    IdGenerator.getId(),
                    auction.getAuctionId(),
                    user.getUserId(),
                    amount,
                    LocalDateTime.now()      // timestamp đúng lúc đặt giá
            );

            auction.setCurrentPrice(amount);
            auction.recordBid(bid);          // ghi bid + cập nhật leadingBidderId
            auction.notifyObservers(bid);    // push realtime tới tất cả client

        } finally {
            auction.getLock().unlock();      // luôn unlock dù có exception
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Nạp tiền vào tài khoản.
     *
     * @param user   tài khoản muốn nạp
     * @param amount số tiền nạp (VNĐ), phải > 0
     */
    public void deposit(User user, double amount)
            throws UserBannedException, InvalidBidException {
        if (user.isBanned()) {
            throw new UserBannedException(user.getUsername());
        }
        if (amount <= 0) {
            throw new InvalidBidException("Số tiền nạp phải lớn hơn 0 VNĐ.");
        }
        user.deposit(amount);
    }
}