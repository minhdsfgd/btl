package com.code.models;

public class BidValidator {


    public static boolean validate(Bid bid, Auction auction) {
        return bid.isValid()
                && isAmountValid(bid.getAmount(), auction)
                && isAuctionOpen(auction)
                && isBidderEligible(bid.getBidder());
    }

    public static boolean isAmountValid(double amount, Auction auction) {
        return amount > auction.getCurrentPrice();
    }


    public static boolean isAuctionOpen(Auction auction) {
        return auction.getStatus() == AuctionStatus.RUNNING;
    }


    public static boolean isBidderEligible(Bidder bidder) {
        return bidder != null && bidder.getBalance() > 0;
    }
}