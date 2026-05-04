package com.code.repository;

import com.code.models.*;

import java.util.*;

public class UserRepository {
    private final Map<Integer, User> users = new HashMap<>();

    public User findById(int id) {
        return users.get(id);
    }

    public User findByUsername(String username) {
        return users.values()
                .stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public void save(User user) {
        users.put(user.getUserId(), user);
    }

    public void update(User user) {
        users.put(user.getUserId(), user);
    }
}
