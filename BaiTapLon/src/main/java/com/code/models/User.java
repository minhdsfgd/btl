package com.code.models;

import java.util.HashSet;
import java.util.Set;

public abstract class User {
    private final int userId;
    private String username;
    private String password;
    private double balance;
    private boolean isActive;
    private boolean isBanned = false;
    Set<Role> roles;

    public User(int userId, String username, String password, double balance, Set<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.balance = balance;
        this.roles = roles;
    }

    boolean hasRole(Role role) {
        return roles.contains(role);
    }
    void addRole(Role role) {
        roles.add(role);
    }
    void removeRole(Role role) {
        roles.remove(role);
    }
    // Setters
    public void setUsername(String username) {this.username = username;}
    public void setPassword(String password) {this.password = password;}
    public void setBalance(double balance) {this.balance = balance;}
    public void setIsBanned(boolean isBanned) {this.isBanned = isBanned;}

    // Getters
    public int getUserId() {return userId;}
    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public double getBalance() {return balance;}
    public boolean isIsBanned() {return isBanned;}
}

enum Role{
    ADMIN, SELLER, BIDDER
}