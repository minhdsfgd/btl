package com.code.models;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {
    private String auctionId;
    private Item item;
    private AuctionStatus status;
    private double currentPrice;
    private Bidder currentWinner;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Bid> bids;
    private ReentrantLock lock;
    private List<AuctionObserver> observers;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this.auctionId = UUID.randomUUID().toString();
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.observers = new ArrayList<>();
    }

    public void start() {
        transitionState(AuctionStatus.RUNNING);
        notifyObservers(bid -> {
            for (AuctionObserver o : observers) o.onAuctionStarted(this);
        });
    }

    public void finish() {
        transitionState(AuctionStatus.FINISHED);
        notifyObservers(bid -> {
            for (AuctionObserver o : observers) o.onAuctionFinished(this);
        });
    }

    public boolean placeBid(Bidder bidder, double amount) {
        lock.lock();
        try {
            Bid bid = new Bid(bidder, this, amount, false);
            if (!BidValidator.validate(bid, this)) {
                System.out.println("Bid rejected for bidder: " + bidder.getUsername());
                return false;
            }
            bids.add(bid);
            currentPrice = amount;
            currentWinner = bidder;
            notifyObservers(b -> {
                for (AuctionObserver o : observers) o.onBidPlaced(this, bid);
            });
            return true;
        } finally {
            lock.unlock();
        }
    }


    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }


    private void notifyObservers(java.util.function.Consumer<Void> action) {
        action.accept(null);
    }

    public void notifyObservers(Bid bid) {
        for (AuctionObserver o : observers) {
            o.onBidPlaced(this, bid);
        }
    }


    public void transitionState(AuctionStatus newStatus) {
        if (this.status == AuctionStatus.CANCELED || this.status == AuctionStatus.PAID) {
            throw new IllegalStateException("Cannot transition from terminal state: " + this.status);
        }
        this.status = newStatus;
        System.out.println("Auction [" + auctionId + "] transitioned to: " + newStatus);
    }


    public AuctionStatus getStatus() { return status; }
    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public double getCurrentPrice() { return currentPrice; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<Bid> getBids() { return bids; }
}