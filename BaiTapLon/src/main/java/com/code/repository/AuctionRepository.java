// ── AuctionRepository.java ───────────────────────────────────────────────────
package com.code.repository;

import com.code.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** FIX: Dùng ConcurrentHashMap thay vì HashMap — thread-safe cho multi-client server. */
public class AuctionRepository {
    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    public Auction findAuctionById(int id) { return auctions.get(id); }

    /** OPEN + RUNNING — Bidder xem danh sách. */
    public List<Auction> findActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> a.getStatus().isActive())   // dùng isActive() mới
                .collect(Collectors.toList());
    }

    public List<Auction> findAll() { return new ArrayList<>(auctions.values()); }

    public List<Auction> findBySellerId(int sellerId) {
        return auctions.values().stream()
                .filter(a -> a.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    public void save(Auction auction) { auctions.put(auction.getAuctionId(), auction); }

    // FIX: Xóa updateStatus() — việc đổi trạng thái phải qua AuctionService (có lock)
    // Nếu repository gọi trực tiếp sẽ bypass managerLock → race condition
}