package com.code.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {
    private int id;
    private String title;
    private String description;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isBanned = false;
    private List<Bid> bids = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    /*
    public Auction(int id, String title, String description, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

     */
    // Setters
    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}
    public void setIsBanned(boolean isBanned) {this.isBanned = isBanned;}

    // Getters
    public int getId() {return id;}
    public String getTitle() {return title;}
    public String getDescription() {return description;}
    public double getCurrentPrice() {return currentPrice;}
    public LocalDateTime getStartTime() {return startTime;}
    public LocalDateTime getEndTime() {return endTime;}
    public boolean isBanned() {return isBanned;}
    public List<Bid> getBids() {return bids;}
    public ReentrantLock getLock() {return lock;}
}