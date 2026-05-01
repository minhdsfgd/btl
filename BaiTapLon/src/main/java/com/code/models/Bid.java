package com.code.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bid {
    private int bidId;
    private int auctionId;
    private int userId;
    private double amount;
    private LocalDateTime timestamp;

    // Getters
    public double getAmount(){
        return amount;
    }
    LocalDateTime getTimestamp(){
        return timestamp;
    }
}