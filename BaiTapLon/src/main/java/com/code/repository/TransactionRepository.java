package com.code.repository;

import com.code.models.Transaction;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TransactionRepository {
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

    public void save(Transaction t)   { transactions.add(t); }
    public List<Transaction> findAll(){ return new ArrayList<>(transactions); }

    public List<Transaction> findByUserId(int userId) {
        return transactions.stream()
                .filter(t -> t.getFromUserId() == userId || t.getToUserId() == userId)
                .collect(Collectors.toList());
    }

    public List<Transaction> findByAuctionId(int auctionId) {
        return transactions.stream()
                .filter(t -> t.getAuctionId() == auctionId)
                .collect(Collectors.toList());
    }
}