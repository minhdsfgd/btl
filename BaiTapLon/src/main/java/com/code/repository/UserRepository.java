// ── UserRepository.java ───────────────────────────────────────────────────────
package com.code.repository;

import com.code.models.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<Integer, User> byId       = new ConcurrentHashMap<>();
    private final Map<String, User>  byUsername = new ConcurrentHashMap<>();

    public User findById(int id)             { return byId.get(id); }
    public User findByUsername(String u)     { return byUsername.get(u); }
    public boolean existsByUsername(String u){ return byUsername.containsKey(u); }
    public List<User> findAll()              { return new ArrayList<>(byId.values()); }

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