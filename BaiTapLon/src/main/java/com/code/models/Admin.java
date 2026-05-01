package com.code.models;
public class Admin extends User {

    public Admin(int id, String username, String password, int balance) {
        super(id, username, password, balance);
    }
    public void banUser(User user) {
        if (user instanceof Admin) {return;}
        user.setIsBanned(true);
    }
    public void unBanUser(User user) {
        if (user instanceof Admin) {return;}
        user.setIsBanned(false);
    }
    public void banAuction(Auction auction) {auction.setIsBanned(true);}
}