package com.code.viewmodel;

/**
 * ViewModel cho một dòng trong bảng lịch sử đặt giá.
 * Tách từ inner class LiveBiddingController.BidRow.
 */
public class BidRow {
    private final String username;
    private final String time;
    private final String amount;

    public BidRow(String username, String time, String amount) {
        this.username = username;
        this.time     = time;
        this.amount   = amount;
    }

    public String getUsername() { return username; }
    public String getTime()     { return time; }
    public String getAmount()   { return amount; }
}