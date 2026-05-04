package com.code.models;
import java.time.LocalDateTime;
public class Transaction {
    private int id;
    private int fromUserId;
    private int toUserId;
    private double amount;
    private int auctionId;
    private LocalDateTime createdAt;

    public Transaction(int id, int fromUserId, int toUserId, double amount,
                       int auctionId) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
        this.auctionId = auctionId;
        this.createdAt = LocalDateTime.now();
    }

    //Getters
    public int getTransactionId() {return id;}
    public int getFromUserId() {return fromUserId;}
    public int getToUserId() {return toUserId;}
    public double getAmount() {return amount;}
    public int getAuctionId() {return auctionId;}
    public LocalDateTime getCreatedAt() {return createdAt;}

    //Setters
    public void setId(int id) {this.id = id;}
    public void setFromUserId(int fromUserId) {this.fromUserId = fromUserId;}
    public void setToUserId(int toUserId) {this.toUserId = toUserId;}
    public void setAmount(double amount) {this.amount = amount;}
    public void setAuctionId(int auctionId) {this.auctionId = auctionId;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}
