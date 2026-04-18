package com.code;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction{
    private String auctionId;
    private Item item;
    private AuctionStatus status;
    private double currnetPrice;
    private Bidder currentWinner;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Bid> bids;
    private ReentrantLock lock;
    private List<AuctionObserver> observers;

    public void start(){}
    public void finish(){}
    public boolean placeBid(Bidder bidder, double amount){}
    public void addObservers(AuctionObserver observer){}
    public void notifyObserers(Bid bid){}
    public AuctionStatus getStatus(){}
    public void transitionState(AuctionStatus newStatus){}
}