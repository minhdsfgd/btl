package com.code.models;

import java.time.LocalDateTime;

public class Seller extends User {


    public Seller(int userId, String username, String password, int balance) {
        super(userId, username, password, balance);
    }


    public Item createItem(String name, String description, double price){
        return Item(itemId, name, description, price);
    }

    public void createAuction(int auctionId, String title, String description, double price) {
        Auction auction = new Auction(
                auctionId,
                title,
                description,
                currentPrice,
                bidIncrement,
                startTime,
                endTime,
                seller);
    }

    // bid increment
    // end price
    // stop auction
    // cancel auction
}
