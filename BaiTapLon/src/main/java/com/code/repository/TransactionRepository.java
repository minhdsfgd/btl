package com.code.repository;

import com.code.models.Transaction;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();

    public void save(Transaction t) { transactions.add(t); }

    public List<Transaction> findAll() { return new ArrayList<>(transactions); }

    /** Lịch sử giao dịch của một user (cả gửi lẫn nhận). */
    public List<Transaction> findByUserId(int userId) {
        return transactions.stream()
                .filter(t -> t.getFromUserId() == userId || t.getToUserId() == userId)
                .collect(Collectors.toList());
    }

    /** Giao dịch của một phiên đấu giá cụ thể. */
    public List<Transaction> findByAuctionId(int auctionId) {
        return transactions.stream()
                .filter(t -> t.getAuctionId() == auctionId)
                .collect(Collectors.toList());
    }
}