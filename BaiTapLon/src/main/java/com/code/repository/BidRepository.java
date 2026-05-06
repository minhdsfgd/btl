package com.code.repository;

import com.code.models.Bid;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** FIX: CopyOnWriteArrayList thay vì ArrayList — thread-safe khi nhiều thread đọc/ghi đồng thời. */
public class BidRepository {
    private final List<Bid> bids = new CopyOnWriteArrayList<>();

    public void save(Bid bid) { bids.add(bid); }

    public List<Bid> findByAuctionId(int auctionId) {
        return bids.stream()
                .filter(b -> b.getAuctionId() == auctionId)
                .collect(Collectors.toList());
    }

    /** FIX: Thêm findByUserId — Bidder xem lịch sử bid của mình. */
    public List<Bid> findByUserId(int userId) {
        return bids.stream()
                .filter(b -> b.getUserId() == userId)
                .collect(Collectors.toList());
    }

    public Optional<Bid> findHighestBid(int auctionId) {
        return bids.stream()
                .filter(b -> b.getAuctionId() == auctionId)
                .max(Comparator.comparingDouble(Bid::getAmount));
    }
}