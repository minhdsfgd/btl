// ── ItemRepository.java ──────────────────────────────────────────────────────
package com.code.repository;

import com.code.models.Item;
import java.util.*;
import java.util.stream.Collectors;

public class ItemRepository {
    private final Map<Integer, Item> items = new HashMap<>();

    public Item findById(int id) { return items.get(id); }

    public List<Item> findAll() { return new ArrayList<>(items.values()); }

    /** Sản phẩm của một Seller — Seller dashboard. */
    public List<Item> findBySellerId(int sellerId) {
        return items.values().stream()
                .filter(i -> i.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    public void save(Item item)    { items.put(item.getItemId(), item); }

    public void delete(int itemId) { items.remove(itemId); }
}