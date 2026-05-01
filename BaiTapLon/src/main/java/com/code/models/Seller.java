package com.code.models;

import java.time.LocalDateTime;

public class Seller extends User {

    public Seller(int id, String username, String password, int balance) {
        super(id, username, password, balance);
    }

    public void createAuction() {
        Auction auction = new Auction();
    }
}
