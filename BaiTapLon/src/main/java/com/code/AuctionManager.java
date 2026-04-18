package com.code;
public class AuctionManager{
    private static AuctionManager instance;
    private AuctionManager(){}
    public static AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
}