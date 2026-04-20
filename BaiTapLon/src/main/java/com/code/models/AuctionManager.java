package com.code.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AuctionManager{
    private static AuctionManager instance;
    private Map<String, Auction> auctionMap;
    private ReentrantLock managerLock;

    private AuctionManager(){
        this.auctionMap = new HashMap<>();
        this.managerLock = new ReentrantLock();
    }

    public static AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
    public Auction createAuction(Item item, Seller seller){
        managerLock.lock();
        try{
            Auction auction = new Auction(item, seller);
            auctionMap.put(auction.getAuctionId(), auction);
            return auction;
        }
        finally {
            managerLock.unlock();
        }
        return new Auction();
    }
    public Auction getAuction(String id){
        Auction auction = auctionMap.get(id);
        if (auction == null){
            throw new IllegalArgumentException("Auction not found");
        }
        return auction;
    }
    public List<Auction> getAllActive(){
        return auctionMap.values().stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN ||
                                    a.getStatus() == AuctionStatus.RUNNING
                ).collect(Collectors.toList());
    }
    public void closeExpiredAuctions() {
        managerLock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            auctionMap.values().stream()
                    .filter(a ->
                            a.getStatus() == AuctionStatus.RUNNING &&
                                    a.getEndTime().isBefore(now)
                    )
                    .forEach(Auction::finish);
        } finally {
            managerLock.unlock();
        }
    }
}