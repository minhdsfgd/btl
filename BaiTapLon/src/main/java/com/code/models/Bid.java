package com.code.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Ghi nhận một lần đặt giá. Immutable sau khi tạo.
 * Implements Serializable để truyền qua Socket.
 */
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private int bidId;
    private final int auctionId;
    private final int userId;
    private final double amount;
    private final LocalDateTime timestamp;

    public Bid(int bidId, int auctionId, int userId,
               double amount, LocalDateTime timestamp) {
        this.bidId     = bidId;
        this.auctionId = auctionId;
        this.userId    = userId;
        this.amount    = amount;
        this.timestamp = timestamp;
    }

    public int           getBidId()     { return bidId; }
    public int           getAuctionId() { return auctionId; }
    public int           getUserId()    { return userId; }
    public double        getAmount()    { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setBidId(int bidId) {
        this.bidId = bidId;
    }



    @Override
    public String toString() {
        return String.format("Bid{id=%d, auction=%d, user=%d, amount=%,.0f}",
                bidId, auctionId, userId, amount);
    }
}