package com.code.repository;

import com.code.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionRepository {
    // Fake repo
    private final Map<Integer, Auction> auctions = new HashMap<>();

    public Auction findAuctionById(int id) {
        return auctions.get(id);
    }

    public List<Auction> findActiveAuctions() {
        return auctions.values()
                .stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .toList();
    }

    public void save(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
    }

    public void updateStatus(int auctionId, AuctionStatus status) {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.updateStatus(status);
        }
    }
}
