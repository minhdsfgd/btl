package com.code.models;

import java.time.LocalDateTime;

public class Bid {
    private final int bidId;
    private final int auctionId;
    private final int userId;
    private final double amount;
    private final LocalDateTime timestamp;

    public Bid(int bidId, int auctionId, int userId, double amount, LocalDateTime timestamp) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBidId() {
        return bidId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
