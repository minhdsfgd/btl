package com.code.models;
public abstract class User {
    private int userId;
    private String username;
    private String password;
    private double balance;
    private boolean isActive;
    private boolean isBanned = false;

    public User(int userId, String username, String password, double balance) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.balance = balance;
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