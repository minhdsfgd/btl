package com.code.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import static com.code.models.AuctionStatus.*;

public class Auction {
    private final int auctionId;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private User seller;
    private boolean isBanned = false;
    private List<Bid> bids = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();
    private Item item;
    private AuctionStatus status;

    private List<BidObserver> observers = new ArrayList<>();


    public Auction(int auctionId,
                   Item item,
                   double currentPrice,
                   double bidIncrement,
                   LocalDateTime startTime,
                   LocalDateTime endTime,
                   User seller
    ) {
        this.auctionId = auctionId;
        this.item= item;
        this.currentPrice = currentPrice;
        this.bidIncrement = bidIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
        this.status=AuctionStatus.OPEN; // mặc định khi tạo là open
    }

    // Observer
    public void addObserver(BidObserver obs) {
        observers.add(obs);
    }
    public void removeObserver(BidObserver obs) {
        observers.remove(obs);
    }
    public void notifyObservers(Bid bid) {
        for (BidObserver obs : observers) {
            obs.onNewBid(bid);
        }
    }

    public void updateStatus(AuctionStatus newStatus) {
        switch (this.status) {
            case OPEN:
                if (newStatus == RUNNING || newStatus == CANCELED) {
                    this.status = newStatus;
                } else throw new IllegalStateException("Invalid transition");
                break;
            case RUNNING:
                if (newStatus == FINISHED || newStatus == CANCELED) {
                    this.status = newStatus;
                } else throw new IllegalStateException("Invalid transition");
                break;

            case FINISHED:
                if (newStatus == PAID) {
                    this.status = newStatus;
                } else throw new IllegalStateException("Invalid transition");
                break;

            default:
                throw new IllegalStateException("Cannot change state from " + this.status);
        }
    }

    // Setters
    public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}
    public void setBidIncrement(double bidIncrement) {this.bidIncrement = bidIncrement;}
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}
    public void setEndTime(LocalDateTime endTime) {this.endTime = endTime;}
    public void setIsBanned(boolean isBanned) {this.isBanned = isBanned;}

    // Getters
    public double getBidIncrement() {return bidIncrement;}
    public int getAuctionId() {return auctionId;}
    public double getCurrentPrice() {return currentPrice;}
    public LocalDateTime getStartTime() {return startTime;}
    public LocalDateTime getEndTime() {return endTime;}
    public boolean isBanned() {return isBanned;}
    public List<Bid> getBids() {return bids;}
    public ReentrantLock getLock() {return lock;}
    public AuctionStatus getStatus() {return status;}

    interface BidObserver {
        void onNewBid(Bid bid);
    }
}