package com.code.service;
import com.code.exception.*;
import com.code.models.*;
import com.code.repository.UserRepository;
import com.code.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;

public class BidService {
    private final UserRepository userRepository;

    // Bổ sung thêm TransactionService ở đây nì
    private final TransactionService transactionService;

    // Cập nhật lại Constructor để Inject nó vô
    public BidService(UserRepository userRepository, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    /**
     * Đặt giá vào phiên đấu giá.
     */
    public void placeBid(User user, Auction auction, double amount)
            throws UserBannedException,
            InvalidBidException,
            SelfBidException,
            AuctionClosedException,
            InsufficientBalanceException {

        if (user.isBanned()) {
            throw new UserBannedException(user.getUsername());
        }

        if (!user.hasRole(Role.BIDDER)) {
            throw new InvalidBidException(
                    "Tài khoản không có quyền đặt giá. Vui lòng đăng ký vai trò Bidder.");
        }

        if (auction.getSellerId() == user.getUserId()) {
            throw new SelfBidException();
        }

        auction.getLock().lock();
        try {
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        "Phiên đấu giá #" + auction.getAuctionId() + " không còn nhận giá "
                                + "(trạng thái: " + auction.getStatus() + ").");
            }

            if (auction.isBanned()) {
                throw new AuctionClosedException(
                        "Phiên đấu giá #" + auction.getAuctionId() + " đã bị khoá bởi Admin.");
            }

            double minRequired = auction.getCurrentPrice() + auction.getBidIncrement();
            if (amount < minRequired) {
                throw new InvalidBidException(String.format(
                        "Giá đặt tối thiểu là %,.0f VNĐ (hiện tại: %,.0f + bước giá: %,.0f).",
                        minRequired, auction.getCurrentPrice(), auction.getBidIncrement()));
            }

            if (user.getBalance() < amount) {
                throw new InsufficientBalanceException(String.format(
                        "Số dư không đủ: cần %,.0f VNĐ, tài khoản có %,.0f VNĐ.",
                        amount, user.getBalance()));
            }

            List<Bid> historyBids = auction.getBids();
            if (historyBids != null && !historyBids.isEmpty()) {
                Bid previousHighestBid = historyBids.get(historyBids.size() - 1);
                User previousBidder = userRepository.findById(previousHighestBid.getUserId());

                if (previousBidder != null) {
                    double refundAmount = previousHighestBid.getAmount();
                    previousBidder.setBalance(previousBidder.getBalance() + refundAmount);
                    userRepository.update(previousBidder);

                    // Đã khai báo TransactionService ở trên rồi nên chừ gọi chạy ngon ơ
                    transactionService.logRefund(previousBidder.getUserId(), refundAmount, auction.getAuctionId());
                }
            }

            user.setBalance(user.getBalance() - amount);
            userRepository.update(user);

            transactionService.logBidHold(user.getUserId(), amount, auction.getAuctionId());

            Bid bid = new Bid(
                    IdGenerator.getId(),
                    auction.getAuctionId(),
                    user.getUserId(),
                    amount,
                    LocalDateTime.now()
            );

            auction.setCurrentPrice(amount);
            auction.recordBid(bid);
            auction.notifyObservers(bid);

        } finally {
            auction.getLock().unlock();
        }
    }

    /**
     * Nạp tiền vào tài khoản.
     */
    public void deposit(User user, double amount)
            throws UserBannedException, InvalidBidException {
        if (user.isBanned()) {
            throw new UserBannedException(user.getUsername());
        }
        if (amount <= 0) {
            throw new InvalidBidException("Số tiền nạp phải lớn hơn 0 VNĐ.");
        }

        // Cộng tiền vô ví
        user.deposit(amount);

        // Bắt buộc phải lưu xuống Database nì
        userRepository.update(user);

        // Ghi lại sao kê nạp tiền
        transactionService.logDeposit(user.getUserId(), amount);
    }
}