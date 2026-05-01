package com.code.models;

import com.code.util.IdGenerator;
import com.code.util.ItemFactory;
import com.code.util.ItemType;

import java.time.LocalDateTime;

public class Seller extends User {


    public Seller(int userId, String username, String password, int balance) {
        super(userId, username, password, balance);
    }


    public Item createItem(ItemType type, String name, String description, double price){
        int itemId = IdGenerator.getId();
        return ItemFactory.createItem(itemId, type, name, description, price);
    }

    public void createAuction() {
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
