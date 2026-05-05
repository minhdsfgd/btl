package com.code.service;

import com.code.models.Item;
import com.code.repository.ItemRepository;

import java.util.List;

public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Thêm một sản phẩm mới vô kho (chưa đem đi đấu giá).
     */
    public void createItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống.");
        }
        itemRepository.save(item);
    }

    /**
     * Tìm sản phẩm theo ID.
     */
    public Item getItem(int itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy mặt hàng mang mã số #" + itemId);
        }
        return item;
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm.
     */
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /**
     * Cập nhật thông tin sản phẩm.
     */
    public void updateItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ.");
        }
        itemRepository.update(item);
    }
}