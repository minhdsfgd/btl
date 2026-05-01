package com.code.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bid {

    private String bidId;
    private Bidder bidder;
    private Auction auction;
    private double amount;
    private LocalDateTime timestamp;
    private boolean isAutoBid;

    public Bid(Bidder bidder, Auction auction, double amount, boolean isAutoBid) {
        this.bidId = UUID.randomUUID().toString();
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.isAutoBid = isAutoBid;
    }

    public boolean isValid() {
        return amount > 0 && bidder != null && auction != null;
    }

    // Getters
    public double getAmount() { return amount; }
    public String getBidId() { return bidId; }
    public Bidder getBidder() { return bidder; }
    public Auction getAuction() { return auction; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isAutoBid() { return isAutoBid; }

}