package com.code.service;

import com.code.models.Auction;
import com.code.models.Bid;
import com.code.models.User;
import java.time.LocalDateTime;
import java.util.Map;

public class AuctionService {
    // Mô phỏng Database
    private final Map<Integer, Auction> auctionDatabase;
    private final Map<Integer, User> userDatabase;
    private int bidCounter = 1;

    public AuctionService(Map<Integer, Auction> auctionDatabase, Map<Integer, User> userDatabase) {
        this.auctionDatabase = auctionDatabase;
        this.userDatabase = userDatabase;
    }

    /**
     * Hàm đặt giá (Bid) - Trả về void nhưng sẽ ném Exception nếu có lỗi
     */
    public void placeBid(int auctionId, int userId, double bidAmount) throws Exception {
        Auction auction = auctionDatabase.get(auctionId);
        User bidder = userDatabase.get(userId);

        // 1. Kiểm tra tính hợp lệ cơ bản
        if (auction == null || bidder == null) {
            throw new Exception("Lỗi hệ thống: Phiên đấu giá hoặc tài khoản không tồn tại.");
        }
        if (auction.isBanned() || bidder.isIsBanned()) {
            throw new Exception("Thao tác bị từ chối: Tài khoản hoặc phiên đấu giá đã bị khóa.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
            throw new Exception("Phiên đấu giá chưa mở hoặc đã kết thúc.");
        }

        // 2. KHÓA AUCTION (Thread-safe) để xử lý logic trừ tiền/cộng tiền
        auction.getLock().lock();
        try {
            double currentPrice = auction.getCurrentPrice();
            double minValidBid = currentPrice + auction.getBidIncrement();

            if (bidAmount < minValidBid) {
                throw new Exception("Số tiền đấu giá phải lớn hơn hoặc bằng " + minValidBid);
            }

            if (bidder.getBalance() < bidAmount) {
                throw new Exception("Số dư trong ví không đủ. Vui lòng nạp thêm tiền.");
            }

            // Hoàn tiền cho người đang giữ giá cao nhất trước đó (nếu có)
            if (!auction.getBids().isEmpty()) {
                Bid highestPreviousBid = auction.getBids().get(auction.getBids().size() - 1);
                // Lưu ý: Cần bổ sung getUserId() vào class Bid của bạn
                User previousBidder = userDatabase.get(highestPreviousBid.getUserId());
                if (previousBidder != null) {
                    previousBidder.setBalance(previousBidder.getBalance() + highestPreviousBid.getAmount());
                }
            }

            // Trừ tiền người bid mới
            bidder.setBalance(bidder.getBalance() - bidAmount);

            // Cập nhật giá mới cho Auction
            auction.setCurrentPrice(bidAmount);

            // Lưu lịch sử Bid (Lưu ý: Cần tạo thêm Constructor cho class Bid)
            Bid newBid = new Bid(bidCounter++, auctionId, userId, bidAmount, now);
            auction.getBids().add(newBid);

        } finally {
            // Luôn unlock trong finally để tránh Deadlock
            auction.getLock().unlock();
        }
    }
}