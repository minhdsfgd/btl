package com.code.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import static com.code.models.AuctionStatus.*;



public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int sellerId;          // chỉ lưu id, không lưu cả User (tránh lộ password)
    private final Item item;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private boolean banned;

    /** userId người đang dẫn đầu; -1 nếu chưa có bid hợp lệ */
    private int leadingBidderId = -1;

    private final List<Bid> bids = new ArrayList<>();

    // transient vì ReentrantLock không Serializable
    private transient ReentrantLock lock;
    private transient List<BidObserver> observers;

    public Auction(int auctionId,
                   Item item,
                   int sellerId,
                   double startingPrice,
                   double bidIncrement,
                   LocalDateTime startTime,
                   LocalDateTime endTime) {
        if (bidIncrement <= 0)
            throw new IllegalArgumentException("bidIncrement phải > 0");
        if (startingPrice < 0)
            throw new IllegalArgumentException("startingPrice không được âm");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        if (!endTime.isAfter(startTime))
            throw new IllegalArgumentException("endTime phải sau startTime");

        this.auctionId    = auctionId;
        this.item         = item;
        this.sellerId     = sellerId;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.status       = AuctionStatus.OPEN;
        initTransient();
    }

    // Khởi tạo lại sau khi deserialize (vì transient bị null)
    private void initTransient() {
        this.lock      = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>();
    }

    private Object readResolve() {
        initTransient();
        return this;
    }

    // ── Observer ─────────────────────────────────────────────────────────────

    public void addObserver(BidObserver obs) {
        if (obs != null) getObservers().add(obs);
    }

    public void removeObserver(BidObserver obs) {
        getObservers().remove(obs);
    }

    public void notifyObservers(Bid bid) {
        for (BidObserver obs : getObservers()) obs.onNewBid(bid);
    }

    private List<BidObserver> getObservers() {
        if (observers == null) observers = new CopyOnWriteArrayList<>();
        return observers;
    }

    // ── Bid ──────────────────────────────────────────────────────────────────

    /**
     * Ghi nhận bid đã được BidService validate.
     * leadingBidderId chỉ cập nhật ở đây — không có setter riêng.
     */
    public void recordBid(Bid bid) {
        Objects.requireNonNull(bid, "bid");
        if (bid.getAuctionId() != auctionId)
            throw new IllegalArgumentException("bid.auctionId không khớp");
        bids.add(bid);
        leadingBidderId = bid.getUserId();
    }

    // ── State machine ────────────────────────────────────────────────────────

    public void updateStatus(AuctionStatus newStatus) {
        switch (this.status) {
            case OPEN    -> { if (newStatus == RUNNING || newStatus == CANCELED) this.status = newStatus;
            else throw new IllegalStateException("OPEN → " + newStatus + " không hợp lệ"); }
            case RUNNING -> { if (newStatus == FINISHED || newStatus == CANCELED) this.status = newStatus;
            else throw new IllegalStateException("RUNNING → " + newStatus + " không hợp lệ"); }
            case FINISHED-> { if (newStatus == PAID) this.status = newStatus;
            else throw new IllegalStateException("FINISHED → " + newStatus + " không hợp lệ"); }
            default      -> throw new IllegalStateException("Không thể đổi trạng thái từ " + this.status);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int            getAuctionId()       { return auctionId; }
    public int            getSellerId()         { return sellerId; }
    public Item           getItem()             { return item; }
    public double         getCurrentPrice()     { return currentPrice; }
    public double         getBidIncrement()     { return bidIncrement; }
    public LocalDateTime  getStartTime()        { return startTime; }
    public LocalDateTime  getEndTime()          { return endTime; }
    public AuctionStatus  getStatus()           { return status; }
    public boolean        isBanned()            { return banned; }
    public int            getLeadingBidderId()  { return leadingBidderId; }
    public List<Bid>      getBids()             { return Collections.unmodifiableList(bids); }
    public ReentrantLock  getLock() {
        if (lock == null) lock = new ReentrantLock();
        return lock;
    }

    // ── Setters (chỉ những gì được phép đổi) ─────────────────────────────────

    public void setCurrentPrice(double p)  {
        if (p < 0) throw new IllegalArgumentException("currentPrice không được âm");
        this.currentPrice = p;
    }
    public void setBidIncrement(double inc) {
        if (inc <= 0) throw new IllegalArgumentException("bidIncrement phải > 0");
        this.bidIncrement = inc;
    }
    public void setStartTime(LocalDateTime t) { this.startTime = t; }
    public void setEndTime(LocalDateTime t)   { this.endTime   = t; }
    public void setBanned(boolean banned)     { this.banned    = banned; }
    // KHÔNG có setLeadingBidderId — chỉ cập nhật qua recordBid()

    // ── Inner interface ───────────────────────────────────────────────────────

    public interface BidObserver {
        void onNewBid(Bid bid);
    }
}