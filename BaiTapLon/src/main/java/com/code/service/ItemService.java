package com.code.service;

import com.code.dao.ItemDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.Item;
import com.code.models.Role;
import com.code.repository.ItemRepository;
import com.code.models.User;

import java.sql.SQLException;
import java.util.List;

public class ItemService {

    private final ItemDAO itemDAO ;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    /**
     * Thêm một sản phẩm mới vô kho (chưa đem đi đấu giá).
     */
    public void createItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống.");
        }
        try{
            itemDAO.save(item);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Tìm sản phẩm theo ID.
     */
    public Item getItem(int itemId) {
        try{
            Item item = itemDAO.findById(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy mặt hàng mang mã số #" + itemId);
            }
            return item;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }


    }

    /**
     * Lấy toàn bộ danh sách sản phẩm.
     */
    public List<Item> getAllItems() {
        try{
            return itemDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }

    }

    /**
     * Cập nhật thông tin sản phẩm.
     */
    public void updateItem(Item item, User currentUser) throws AuthenticationException, UserBannedException {
        try{
            if (currentUser == null) {
                throw new AuthenticationException("User chưa đăng nhập");
            }

            if (currentUser.isBanned()) {
                throw new UserBannedException(currentUser.getUsername());
            }

            Item existing = itemDAO.findById(item.getItemId());
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

            itemDAO.save(existing);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB: " + e.getMessage(), e);
        }
    }
}

