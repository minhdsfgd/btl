package com.code.models;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
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

    // ── Balance ──────────────────────────────────────────────────────────────

    /**
     * Nạp tiền vào tài khoản. amount phải > 0.
     */
    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền nạp phải > 0");
        this.balance += amount;
    }

    /**
     * Trừ tiền khi thắng đấu giá hoặc Admin điều chỉnh.
     * Ném InsufficientBalanceException nếu không đủ số dư.
     */
    public synchronized void deductBalance(double amount)
            throws com.code.exception.InsufficientBalanceException {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền trừ phải > 0");
        if (this.balance < amount) {
            throw new com.code.exception.InsufficientBalanceException(
                    "Số dư không đủ: cần " + amount + " VNĐ, hiện có " + this.balance + " VNĐ");
        }
        this.balance -= amount;
    }

    /**
     * Chỉ dùng nội bộ (DAO load từ DB). Không expose ra ngoài để tránh
     * bypass logic deposit/deduct.
     */
    public void setBalance(double balance) {
        if (balance < 0) throw new IllegalArgumentException("balance không được âm");
        this.balance = balance;
    }

    // ── Role ─────────────────────────────────────────────────────────────────

    public boolean hasRole(Role role) { return roles.contains(role); }

    public void addRole(Role role) {
        if (role != null) roles.add(role);
    }

    public void removeRole(Role role) {
        if (role != null) roles.remove(role);
    }

    public Set<Role> getRoles() { return Collections.unmodifiableSet(roles); }

    /** Dùng chung bởi subclass để merge role chính + role phụ. */
    protected static Set<Role> mergeRoles(Role primary, Set<Role> extra) {
        EnumSet<Role> set = EnumSet.of(primary);
        if (extra != null) set.addAll(extra);
        return set;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setUsername(String username) {
        this.username = Objects.requireNonNull(username, "username");
    }
    public void setPassword(String password) {
        this.password = Objects.requireNonNull(password, "password");
    }
    public void setActive(boolean active)    { this.active   = active; }
    public void setBanned(boolean banned)    { this.banned   = banned; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int    getUserId()   { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public double getBalance()  { return balance; }
    public boolean isActive()   { return active; }
    public boolean isBanned()   { return banned; }
}