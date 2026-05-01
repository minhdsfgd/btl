package com.code.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bid {
    private int bidId;
    private int auctionId;
    private int userId;
    private double amount;
    private LocalDateTime timestamp;
    public Bid(int bidId, int auctionId, int userId, double amount, LocalDateTime timestamp) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.timestamp = timestamp;
    }


    // Getters
    public int getUserId() { return userId; }
    public double getAmount(){
        return amount;
    }
    LocalDateTime getTimestamp(){
        return timestamp;
    }
}