package com.code.service;

import com.code.dao.TransactionDAO;
import com.code.models.Transaction;

import java.sql.SQLException;
import java.util.List;

public class TransactionService {

    private final TransactionDAO transactionDAO;

    public TransactionService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
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
}