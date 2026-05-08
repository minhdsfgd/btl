package com.code.repository;

import com.code.models.User;
import java.util.ArrayList;
import java.util.List;

public class FakeUserRepository extends UserRepository {

    // Dùng List để làm "Database tạm thời" trên RAM
    private List<User> database = new ArrayList<>();

    @Override
    public void save(User user) {
        database.add(user);
    }

    @Override
    public User findByUsername(String username) {
        for (User user : database) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null; // Không tìm thấy
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    @Override
    public void update(User user) {
        // Trong List thực ra object đã được update tham chiếu rồi,
        // nhưng viết chuẩn thì có thể code logic tìm và thay thế ở đây.
    }

    @Override
    public User findById(int id) {
        for (User user : database) {
            if (user.getUserId() == id) {
                return user;
            }
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        return database;
    }
}