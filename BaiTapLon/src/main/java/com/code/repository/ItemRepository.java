package com.code.repository;

import com.code.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRepository {
    private final Map<Integer, Item> items = new HashMap<>();

    public Item findById(int id) {
        return items.get(id);
    }

    public List<Item> findAll() {
        return new ArrayList<>(items.values());
    }

    public void save(Item item) {
        items.put(item.getItemId(), item);
    }
}
