package com.code.service;

import com.code.dao.TransactionDAO;
import com.code.dao.UserDAO;
import com.code.exception.InsufficientBalanceException;
import com.code.exception.UserBannedException;
import com.code.models.Transaction;
import com.code.models.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionService {

    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;

    public TransactionService(TransactionDAO transactionDAO, UserDAO userDAO) {
        this.transactionDAO = transactionDAO;
        this.userDAO = userDAO;
    }

    /**
     * Ghi nhận lúc user nạp tiền vô ví.
     */
    public void logDeposit(int userId, double amount) {
        Transaction tx = Transaction.deposit(
                0,
                userId,
                amount
        );
        try{
            transactionDAO.save(tx);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Ghi nhận lúc user bị trừ tiền để đặt giá (Bid).
     */
    public void logBidHold(int userId, double amount, int auctionId) {
        // Tiền đi từ user sang hệ thống (-1)
        Transaction tx = new Transaction(
                0,
                userId,  // fromUserId: người đặt giá
                -1,      // toUserId: hệ thống giữ
                amount,  // Lưu ý: amount phải > 0 theo rule trong class của bạn
                auctionId,
                Transaction.Type.ADJUSTMENT
        );
        try{
            transactionDAO.save(tx);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Ghi nhận lúc hệ thống hoàn trả tiền do có người khác ra giá cao hơn.
     */
    public void logRefund(int userId, double amount, int auctionId) {
        Transaction tx = new Transaction(
                0,
                -1,      // fromUserId: từ hệ thống
                userId,  // toUserId: trả lại cho user
                amount,
                auctionId,
                Transaction.Type.REFUND
        );
        try{
            transactionDAO.save(tx);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Ghi nhận lúc thanh toán tiền cho người bán khi phiên đấu giá kết thúc.
     */
    public void logPaymentToSeller(int buyerId, int sellerId, double amount, int auctionId) {
        Transaction tx = new Transaction(
                0,
                buyerId,  // fromUserId: người thắng cuộc trả tiền
                sellerId, // toUserId: người bán nhận tiền
                amount,
                auctionId,
                Transaction.Type.AUCTION_PAYMENT
        );
        try{
            transactionDAO.save(tx);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Lấy sao kê lịch sử giao dịch của một user cụ thể.
     */
    public List<Transaction> getTransactionHistory(int userId) {
        // Tùy theo cách bạn viết hàm này bên TransactionDAO
        try{
            return transactionDAO.findByUserId(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }


    }
    public void save(Transaction tx) {
        try {
            transactionDAO.save(tx);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi lưu transaction: " + e.getMessage(), e);
        }
    }
    // TransactionService.java
    public Transaction deposit(User user, double amount)
            throws UserBannedException, SQLException, InsufficientBalanceException {

        // Validate
        if (!user.isActive())
            throw new UserBannedException(user.getUsername());

        if (amount <= 0)
            throw new IllegalArgumentException("Số tiền phải > 0");

        // Update user balance in memory
        user.deposit(amount);

        try {
            // Lưu user xuống DB
            userDAO.update(user);

            Transaction tx = Transaction.deposit(
                    0,                      // id = 0 (auto-increment từ DB)
                    user.getUserId(),       // toUserId
                    amount
            );

            // Save transaction (id sẽ được set bởi DAO)
            transactionDAO.save(tx);

            return tx;

        } catch (SQLException e) {
            // Rollback balance in memory
            user.deductBalance(amount);

            throw new RuntimeException(
                    "Lỗi lưu deposit transaction: " + e.getMessage(), e);
        }
    }
}