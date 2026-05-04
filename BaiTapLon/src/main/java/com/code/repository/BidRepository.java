package com.code.repository;

import com.code.models.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BidRepository {
    private final List<Bid> bids = new ArrayList<>();

    public List<Bid> findByAuctionId(int auctionId) {
        return bids.stream()
                .filter(b -> b.getAuctionId() == auctionId)
                .toList();
    }

    public Bid findHighestBid(int auctionId) {
        return bids.stream()
                .filter(b -> b.getAuctionId() == auctionId)
                .max(Comparator.comparingDouble(Bid::getAmount))
                .orElse(null);
    }

    public void save(Bid bid) {
        bids.add(bid);
    }
}
