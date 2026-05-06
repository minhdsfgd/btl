package com.code.service;

import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.Item;
import com.code.models.Role;
import com.code.repository.ItemRepository;
import com.code.models.User;

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
    public void updateItem(Item item, User currentUser) throws AuthenticationException, UserBannedException {
        if (currentUser == null) {
            throw new AuthenticationException("User chưa đăng nhập");
        }

        if (currentUser.isBanned()) {
            throw new UserBannedException(currentUser.getUsername());
        }

        Item existing = itemRepository.findById(item.getItemId());
        if (existing == null) {
            throw new IllegalArgumentException("Item không tồn tại");
        }

        // Không phải owner và cũng không phải admin
        if (existing.getSellerId() != currentUser.getUserId()
                && !currentUser.hasRole(Role.ADMIN)) {
            throw new AuthenticationException("Không có quyền sửa item");
        }

        // Update field
        existing.setName(item.getName());
        existing.setDescription(item.getDescription());
        existing.setStartingPrice(item.getStartingPrice());

        itemRepository.save(existing);
    }
}