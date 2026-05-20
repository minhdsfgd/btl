package com.code.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    private Bid bid;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        // Cố định một mốc thời gian để so sánh
        timestamp = LocalDateTime.now();

        // Khởi tạo một đối tượng Bid mẫu dùng chung cho các test case
        bid = new Bid(1, 100, 50, 250000.0, timestamp);
    }

    @Test
    @DisplayName("TC01: Khởi tạo Bid thành công và kiểm tra các hàm Getters")
    void testConstructorAndGetters() {
        assertEquals(1, bid.getBidId(), "Bid ID phải là 1");
        assertEquals(100, bid.getAuctionId(), "Auction ID phải là 100");
        assertEquals(50, bid.getUserId(), "User ID phải là 50");
        assertEquals(250000.0, bid.getAmount(), 0.01, "Amount phải là 250000.0");
        assertEquals(timestamp, bid.getTimestamp(), "Timestamp phải khớp với thời gian khởi tạo");
    }

    @Test
    @DisplayName("TC02: Kiểm tra hàm setBidId cập nhật đúng ID")
    void testSetBidId() {
        bid.setBidId(999);
        assertEquals(999, bid.getBidId(), "Bid ID phải được cập nhật thành 999");
    }

    @Test
    @DisplayName("TC03: Kiểm tra hàm toString trả về đúng format")
    void testToString() {
        // Hàm toString() trong class Bid sử dụng String.format() với %,.0f
        // Định dạng dấu phẩy/chấm phụ thuộc vào Locale của máy tính chạy test.
        // Để test chính xác, ta so sánh nội dung xuất ra với chính cách String.format hoạt động.
        String expectedString = String.format("Bid{id=1, auction=100, user=50, amount=%,.0f}", 250000.0);

        assertEquals(expectedString, bid.toString(), "Chuỗi toString không đúng định dạng mong đợi");
    }
}