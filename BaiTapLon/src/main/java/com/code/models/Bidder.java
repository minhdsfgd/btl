package com.code.models;
public class Bidder extends User {
    private double balance;
    public Bidder(String name, String email,String id, String password) {
        super(name, email, id, password);
        this.balance = 0;
    }
    public void bid() {}
    public double getBalance() {
        return balance;
    }
    public void addBalance(double amount) {
        this.balance += amount;
    }
}