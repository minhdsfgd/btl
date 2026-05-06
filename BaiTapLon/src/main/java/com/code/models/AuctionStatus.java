package com.code.models;

public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;

    /**
     * Trạng thái kết thúc — không thể đổi sang trạng thái khác (trừ FINISHED→PAID).
     * Dùng để kiểm tra nhanh thay vì so sánh thủ công nhiều nơi.
     *
     * <pre>
     * if (auction.getStatus().isTerminal()) {
     *     // không nhận bid mới
     * }
     * </pre>
     */
    public boolean isTerminal() {
        return this == FINISHED || this == PAID || this == CANCELED;
    }

    /** Phiên đang hoạt động và nhận bid. */
    public boolean isActive() {
        return this == OPEN || this == RUNNING;
    }
}