package com.code.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {
    private int id;
    private int fromUserId;
    private int toUserId;
    private double amount;
    private int auctionId;
    private TransactionType type;
    private LocalDateTime createdAt;

    public Transaction(int id, int fromUserId, int toUserId, double amount, int auctionId, TransactionType type) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
        this.auctionId = auctionId;
        this.type = Objects.requireNonNullElse(type, TransactionType.AUCTION_PAYMENT);
        this.createdAt = LocalDateTime.now();
    }

    /** Tương thích constructor cũ: mặc định {@link TransactionType#AUCTION_PAYMENT}. */
    public Transaction(int id, int fromUserId, int toUserId, double amount, int auctionId) {
        this(id, fromUserId, toUserId, amount, auctionId, TransactionType.AUCTION_PAYMENT);
    }

    public int getTransactionId() {
        return id;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public int getToUserId() {
        return toUserId;
    }

    public double getAmount() {
        return amount;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public void setType(TransactionType type) {
        this.type = Objects.requireNonNullElse(type, TransactionType.AUCTION_PAYMENT);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
