package com.code.repository;

import com.code.models.*;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionRepository {
    private final Map<Integer, Auction> auctions = new HashMap<>();

    public Auction findAuctionById(int id) { return auctions.get(id); }

    /** OPEN + RUNNING — hiển thị cho Bidder. */
    public List<Auction> findActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }

    /** Tất cả — Admin quản lý và scheduler dùng. */
    public List<Auction> findAll() { return new ArrayList<>(auctions.values()); }

    /** Phiên của một Seller — Seller dashboard. */
    public List<Auction> findBySellerId(int sellerId) {
        return auctions.values().stream()
                .filter(a -> a.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    public void save(Auction auction) { auctions.put(auction.getAuctionId(), auction); }

    public void updateStatus(int auctionId, AuctionStatus status) {
        Auction a = auctions.get(auctionId);
        if (a != null) a.updateStatus(status);
    }
}