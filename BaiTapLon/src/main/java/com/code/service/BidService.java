package com.code.service;

import com.code.models.*;
import com.code.util.IdGenerator;
import java.time.LocalDateTime;

public class BidService {
    private final LocalDateTime timestamp =  LocalDateTime.now();
    
    public void placeBid(User user, Auction auction, double amount) {
        // validate
        // update giá

        Bid bid = new Bid(IdGenerator.getId(),
                auction.getAuctionId(),
                user.getUserId(),
                amount,
                timestamp);

        auction.notifyObservers(bid);
    }
}