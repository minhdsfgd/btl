package com.code;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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
    public Auction getAuction(String id){}
    public List<Auction> getAllActive(){}
    public void closeExpiredAuctions(){};
}