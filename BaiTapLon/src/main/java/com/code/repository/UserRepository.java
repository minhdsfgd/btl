// ── UserRepository.java ───────────────────────────────────────────────────────
package com.code.repository;

import com.code.models.User;
import java.util.*;

public class UserRepository {
    private final Map<Integer, User> byId       = new HashMap<>();
    private final Map<String, User>  byUsername = new HashMap<>();

    public User findById(int id) { return byId.get(id); }

    public User findByUsername(String username) { return byUsername.get(username); }

    public boolean existsByUsername(String username) { return byUsername.containsKey(username); }

    public List<User> findAll() { return new ArrayList<>(byId.values()); }

    public void save(User user) {
        byId.put(user.getUserId(), user);
        byUsername.put(user.getUsername(), user);
    }

    public void update(User user) { save(user); }

    public void delete(int userId) {
        User u = byId.remove(userId);
        if (u != null) byUsername.remove(u.getUsername());
    }
}