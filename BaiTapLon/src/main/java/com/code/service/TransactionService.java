package com.code.service;

import com.code.models.Transaction;
import com.code.repository.TransactionRepository;
import com.code.util.IdGenerator;

import java.util.List;

public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Ghi nhận lúc user nạp tiền vô ví.
     */
    public void logDeposit(int userId, double amount) {
        // Class của bạn có sẵn hàm static deposit() xịn xò rứa thì mần luôn
        Transaction tx = Transaction.deposit(
                IdGenerator.getId(),
                userId,
                amount
        );
        transactionRepository.save(tx);
    }

    /**
     * Ghi nhận lúc user bị trừ tiền để đặt giá (Bid).
     */
    public void logBidHold(int userId, double amount, int auctionId) {
        // Tiền đi từ user sang hệ thống (-1)
        Transaction tx = new Transaction(
                IdGenerator.getId(),
                userId,  // fromUserId: người đặt giá
                -1,      // toUserId: hệ thống giữ
                amount,  // Lưu ý: amount phải > 0 theo rule trong class của bạn
                auctionId,
                Transaction.Type.ADJUSTMENT
        );
        transactionRepository.save(tx);
    }

    /**
     * Ghi nhận lúc hệ thống hoàn trả tiền do có người khác ra giá cao hơn.
     */
    public void logRefund(int userId, double amount, int auctionId) {
        Transaction tx = new Transaction(
                IdGenerator.getId(),
                -1,      // fromUserId: từ hệ thống
                userId,  // toUserId: trả lại cho user
                amount,
                auctionId,
                Transaction.Type.REFUND
        );
        transactionRepository.save(tx);
    }

    /**
     * Ghi nhận lúc thanh toán tiền cho người bán khi phiên đấu giá kết thúc.
     */
    public void logPaymentToSeller(int buyerId, int sellerId, double amount, int auctionId) {
        Transaction tx = new Transaction(
                IdGenerator.getId(),
                buyerId,  // fromUserId: người thắng cuộc trả tiền
                sellerId, // toUserId: người bán nhận tiền
                amount,
                auctionId,
                Transaction.Type.AUCTION_PAYMENT
        );
        transactionRepository.save(tx);
    }

    /**
     * Lấy sao kê lịch sử giao dịch của một user cụ thể.
     */
    public List<Transaction> getTransactionHistory(int userId) {
        // Tùy theo cách bạn viết hàm này bên TransactionRepository
        return transactionRepository.findByUserId(userId);
    }
}