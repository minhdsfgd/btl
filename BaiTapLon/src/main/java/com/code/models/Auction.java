package com.code.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {
    private final int auctionId;
    private String title;
    private String description;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private User seller;
    private boolean isBanned = false;
    private List<Bid> bids = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public Auction(int auctionId,
                   Item item,
                   double bidIncrement,
                   LocalDateTime startTime,
                   LocalDateTime endTime,
                   User seller
                   ) {
        this.auctionId = auctionId;
        this.title = title;
        this.description = description;
        this.currentPrice = currentPrice;
        this.bidIncrement = bidIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
    }


    // Setters
    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}
    public void setBidIncrement(double bidIncrement) {this.bidIncrement = bidIncrement;}
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}
    public void setEndTime(LocalDateTime endTime) {this.endTime = endTime;}
    public void setIsBanned(boolean isBanned) {this.isBanned = isBanned;}

    // Getters
    public int getAuctionId() {return auctionId;}
    public String getTitle() {return title;}
    public String getDescription() {return description;}
    public double getCurrentPrice() {return currentPrice;}
    public LocalDateTime getStartTime() {return startTime;}
    public LocalDateTime getEndTime() {return endTime;}
    public boolean isBanned() {return isBanned;}
    public List<Bid> getBids() {return bids;}
    public ReentrantLock getLock() {return lock;}
}