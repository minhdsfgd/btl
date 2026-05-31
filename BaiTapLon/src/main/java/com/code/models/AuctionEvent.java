package com.code.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sự kiện được gửi tới Observer khi có thay đổi trong phiên đấu giá.
 *
 * <p>Thay thế cách dùng Bid giả (bidId=-1) làm signal kết thúc phiên.
 * Observer phân biệt sự kiện qua {@link EventType}:</p>
 * <ul>
 *   <li>{@code BID_PLACED}        — có bid mới, {@link #getBid()} trả về bid thật</li>
 *   <li>{@code AUCTION_FINISHED}  — phiên kết thúc, {@link #getWinnerBidderId()} là người thắng</li>
 *   <li>{@code AUCTION_CANCELED}  — phiên bị hủy, không có người thắng</li>
 *   <li>{@code STATUS_CHANGED}    — scheduler chuyển OPEN→RUNNING</li>
 *   <li>{@code USER_BANNED}       — user bị ban, client cần logout về Login</li>
 * </ul>
 *
 * <pre>
 * // Trong Observer (client):
 * public void onAuctionEvent(AuctionEvent event) {
 *     switch (event.getType()) {
 *         case BID_PLACED       -> updatePriceLabel(event.getBid().getAmount());
 *         case AUCTION_FINISHED -> showWinner(event.getWinnerBidderId());
 *         case AUCTION_CANCELED -> showCanceledMessage();
 *         case STATUS_CHANGED   -> refreshStatus(event.getNewStatus());
 *         case USER_BANNED      -> handleBanned();
 *     }
 * }
 * </pre>
 */
public class AuctionEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        BID_PLACED,
        AUCTION_FINISHED,
        AUCTION_CANCELED,
        STATUS_CHANGED,
        TIME_EXTENDED,
        /** Push xuống client đang xem LiveBidding khi tài khoản của họ bị ban. */
        USER_BANNED
    }

    private final EventType      type;
    private final int            auctionId;
    private final Bid            bid;            // null nếu không phải BID_PLACED
    private final int            winnerBidderId; // -1 nếu không có người thắng
    private final AuctionStatus  newStatus;      // trạng thái mới sau khi đổi
    private final LocalDateTime  timestamp;
    private final int            bannedUserId;   // userId bị ban (-1 nếu không áp dụng)
    private List<Bid> bidHistory;

    // ── Constructor cho BID_PLACED ────────────────────────────────────────────
    public static AuctionEvent bidPlaced(int auctionId, Bid bid) {
        return new AuctionEvent(EventType.BID_PLACED, auctionId, bid, -1, null, -1);
    }

    // ── Constructor cho AUCTION_FINISHED ─────────────────────────────────────
    public static AuctionEvent auctionFinished(int auctionId, int winnerBidderId) {
        return new AuctionEvent(EventType.AUCTION_FINISHED, auctionId, null, winnerBidderId, AuctionStatus.FINISHED, -1);
    }

    // ── Constructor cho AUCTION_CANCELED ─────────────────────────────────────
    public static AuctionEvent auctionCanceled(int auctionId) {
        return new AuctionEvent(EventType.AUCTION_CANCELED, auctionId, null, -1, AuctionStatus.CANCELED, -1);
    }

    // ── Constructor cho STATUS_CHANGED (OPEN→RUNNING) ────────────────────────
    public static AuctionEvent statusChanged(int auctionId, AuctionStatus newStatus) {
        return new AuctionEvent(EventType.STATUS_CHANGED, auctionId, null, -1, newStatus, -1);
    }

    public static AuctionEvent timeExtended(int auctionId) {
        return new AuctionEvent(EventType.TIME_EXTENDED, auctionId, null, -1, null, -1);
    }

    // ── Constructor cho USER_BANNED ───────────────────────────────────────────
    public static AuctionEvent userBanned(int auctionId, int bannedUserId) {
        return new AuctionEvent(EventType.USER_BANNED, auctionId, null, -1, null, bannedUserId);
    }

    private AuctionEvent(EventType type, int auctionId, Bid bid,
                         int winnerBidderId, AuctionStatus newStatus, int bannedUserId) {
        this.type           = type;
        this.auctionId      = auctionId;
        this.bid            = bid;
        this.winnerBidderId = winnerBidderId;
        this.newStatus      = newStatus;
        this.timestamp      = LocalDateTime.now();
        this.bannedUserId   = bannedUserId;
    }

    public EventType     getType()            { return type; }
    public int           getAuctionId()       { return auctionId; }
    public Bid           getBid()             { return bid; }
    public int           getWinnerBidderId()  { return winnerBidderId; }
    public AuctionStatus getNewStatus()       { return newStatus; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public int           getBannedUserId()    { return bannedUserId; }

    @Override
    public String toString() {
        return String.format("AuctionEvent{type=%s, auction=%d, time=%s}",
                type, auctionId, timestamp);
    }
}