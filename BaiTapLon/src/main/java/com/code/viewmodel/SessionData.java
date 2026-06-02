package com.code.viewmodel;

/**
 * Data transfer object chứa toàn bộ thông tin cần thiết
 * để khởi tạo màn hình LiveBidding.
 *
 * Trước đây là inner class private trong LiveBiddingController.
 * Tách ra để dễ test và tái sử dụng.
 */
public class SessionData {
    public final int    auctionId;
    public final String username;
    public final String sessionName;
    public final String productName;
    public final String description;
    public final String imageUrl;
    public final double startPrice;
    public final double currentPrice;
    public final double minStep;
    public final long   countdownSeconds;
    public final String leadingBidder;
    public final int    ownerId;

    public SessionData(int auctionId, String username, String sessionName,
                       String productName, String description, String imageUrl,
                       double startPrice, double currentPrice,
                       double minStep, long countdownSeconds,
                       String leadingBidder, int ownerId) {
        this.auctionId        = auctionId;
        this.username         = username;
        this.sessionName      = sessionName;
        this.productName      = productName;
        this.description      = description;
        this.imageUrl         = imageUrl;
        this.startPrice       = startPrice;
        this.currentPrice     = currentPrice;
        this.minStep          = minStep;
        this.countdownSeconds = countdownSeconds;
        this.leadingBidder    = leadingBidder;
        this.ownerId          = ownerId;
    }
}