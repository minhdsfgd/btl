package com.code.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ghi nhận giao dịch tài chính. Hoàn toàn IMMUTABLE sau khi tạo.
 * Không có setters vì giao dịch tài chính không được phép sửa.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        AUCTION_PAYMENT, // Bidder thắng trả tiền cho Seller
        REFUND,          // Hoàn tiền khi phiên bị hủy
        DEPOSIT,         // Nạp tiền vào tài khoản
        ADJUSTMENT       // Admin điều chỉnh thủ công
    }

    private final int id;
    private final int fromUserId;   // người trả tiền (-1 nếu là nạp tiền)
    private final int toUserId;     // người nhận tiền
    private final double amount;
    private final int auctionId;    // -1 nếu không liên quan đến auction
    private final Type type;
    private final LocalDateTime createdAt;

    public Transaction(int id, int fromUserId, int toUserId,
                       double amount, int auctionId, Type type) {
        if (amount <= 0) throw new IllegalArgumentException("amount phải > 0");
        this.id         = id;
        this.fromUserId = fromUserId;
        this.toUserId   = toUserId;
        this.amount     = amount;
        this.auctionId  = auctionId;
        this.type       = Objects.requireNonNullElse(type, Type.AUCTION_PAYMENT);
        this.createdAt  = LocalDateTime.now();
    }

    /** Constructor cho thanh toán đấu giá (loại mặc định). */
    public Transaction(int id, int fromUserId, int toUserId,
                       double amount, int auctionId) {
        this(id, fromUserId, toUserId, amount, auctionId, Type.AUCTION_PAYMENT);
    }

    /** Constructor cho nạp tiền (không liên quan auction). */
    public static Transaction deposit(int id, int toUserId, double amount) {
        return new Transaction(id, -1, toUserId, amount, -1, Type.DEPOSIT);
    }

    public int           getId()          { return id; }
    public int           getFromUserId()  { return fromUserId; }
    public int           getToUserId()    { return toUserId; }
    public double        getAmount()      { return amount; }
    public int           getAuctionId()   { return auctionId; }
    public Type          getType()        { return type; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, type=%s, from=%d, to=%d, amount=%,.0f}",
                id, type, fromUserId, toUserId, amount);
    }
}