package com.code.models;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String password;
    private double balance;
    private boolean active = true;
    private boolean banned;
    private final Set<Role> roles;

    protected User(int userId, String username,
                   String password, double balance, Set<Role> roles) {
        if (balance < 0) throw new IllegalArgumentException("balance không được âm");
        this.userId   = userId;
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
        this.balance  = balance;
        this.roles    = (roles == null || roles.isEmpty())
                ? EnumSet.noneOf(Role.class)
                : EnumSet.copyOf(roles);
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền nạp phải > 0");
        this.balance += amount;
    }

    public synchronized void deductBalance(double amount)
            throws com.code.exception.InsufficientBalanceException {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền trừ phải > 0");
        if (this.balance < amount)
            throw new com.code.exception.InsufficientBalanceException(
                    String.format("Số dư không đủ: cần %,.0f VNĐ, hiện có %,.0f VNĐ.", amount, balance));
        this.balance -= amount;
    }

    /**
     * CHỈ DAO dùng khi load từ DB.
     * protected + synchronized để đồng bộ với deposit/deductBalance.
     */
    public synchronized void setBalance(double balance) {
        if (balance < 0) throw new IllegalArgumentException("balance không được âm");
        this.balance = balance;
    }

    // ── Role ──────────────────────────────────────────────────────────────────

    public synchronized boolean hasRole(Role role)  { return roles.contains(role); }
    public synchronized void addRole(Role role)     { if (role != null) roles.add(role); }
    public synchronized void removeRole(Role role)  { if (role != null) roles.remove(role); }
    public Set<Role> getRoles()                     { return Collections.unmodifiableSet(roles); }

    protected static Set<Role> mergeRoles(Role primary, Set<Role> extra) {
        EnumSet<Role> set = EnumSet.of(primary);
        if (extra != null) set.addAll(extra);
        return set;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUserId(int id)       { this.userId = id; }
    public void setUsername(String u)  { this.username = Objects.requireNonNull(u); }
    public void setPassword(String p)  { this.password = Objects.requireNonNull(p); }
    public void setActive(boolean a)   { this.active = a; }
    public void setBanned(boolean b)   { this.banned = b; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getUserId()   { return userId; }
    public String  getUsername() { return username; }
    public String  getPassword() { return password; }
    public double  getBalance()  { return balance; }
    public boolean isActive()    { return active; }
    public boolean isBanned()    { return banned; }

    // ── equals / hashCode (dựa trên userId) ──────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return userId == ((User) o).userId;
    }

    @Override
    public int hashCode() { return Integer.hashCode(userId); }
}