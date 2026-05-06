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

    /** userId người dẫn đầu; -1 nếu chưa có bid */
    private int leadingBidderId = -1;
    //danh sách trống để lưu lịch sử các lượt đặt giá
    private final List<Bid> bids = new ArrayList<>();

    // transient — không Serializable
    private transient ReentrantLock lock;
    private transient List<AuctionObserver> observers;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Auction(int auctionId, Item item, int sellerId,
                   double startingPrice, double bidIncrement,
                   LocalDateTime startTime, LocalDateTime endTime) {
        if (bidIncrement <= 0)
            throw new IllegalArgumentException("bidIncrement phải > 0");
        if (startingPrice < 0)
            throw new IllegalArgumentException("startingPrice không được âm");

        // ném lỗi ngay lập tức nếu truyền null
        Objects.requireNonNull(item,      "item");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime,   "endTime");

        // kiểm tra logic tgian
        if (!endTime.isAfter(startTime))
            throw new IllegalArgumentException("endTime phải sau startTime");

        this.auctionId    = auctionId;
        this.item         = item;
        this.sellerId     = sellerId;
        this.currentPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.status       = OPEN;
        // gọi hàm phụ để khởi tạo khoá lock vào ds observer
        initTransient();
    }

    // Khởi tạo lại sau khi deserialize (vì transient bị null)
    private void initTransient() {
        this.lock      = new ReentrantLock();
        this.observers = new CopyOnWriteArrayList<>();
    }

    // readObject thay vì readResolve — đảm bảo transient được khởi tạo đúng
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransient();
    }

    // ── Observer (dùng AuctionEvent thay vì Bid) ─────────────────────────────

    public void addObserver(AuctionObserver obs) {
        if (obs != null) observers().add(obs);
    }

    public void removeObserver(AuctionObserver obs) {
        observers().remove(obs);
    }

    /** Gửi sự kiện tới tất cả observer. */
    public void notifyObservers(AuctionEvent event) {
        // lặp qua ds những aoi đang theo dõi
        for (AuctionObserver obs : observers()) {
            obs.onAuctionEvent(event);
        }
    }

    private List<AuctionObserver> observers() {
        if (observers == null) observers = new CopyOnWriteArrayList<>();
        return observers;
    }

    // ── Bid ───────────────────────────────────────────────────────────────────

    /**
     * Ghi nhận bid đã được BidService validate.
     * Chỉ cho phép khi status == RUNNING.
     * leadingBidderId chỉ cập nhật ở đây — không có setter riêng.
     */
    public void recordBid(Bid bid) {
        Objects.requireNonNull(bid, "bid");
        if (this.status != RUNNING)
            throw new IllegalStateException(
                    "Không thể ghi bid khi phiên ở trạng thái " + this.status);
        if (bid.getAuctionId() != auctionId)
            throw new IllegalArgumentException("bid.auctionId không khớp");
        bids.add(bid);
        leadingBidderId = bid.getUserId();
    }

    // ── State machine ─────────────────────────────────────────────────────────

    public void updateStatus(AuctionStatus newStatus) {
        switch (this.status) {
            case OPEN     -> { if (newStatus == RUNNING || newStatus == CANCELED)
                this.status = newStatus;
            else throw new IllegalStateException("OPEN → " + newStatus + " không hợp lệ"); }
            case RUNNING  -> { if (newStatus == FINISHED || newStatus == CANCELED)
                this.status = newStatus;
            else throw new IllegalStateException("RUNNING → " + newStatus + " không hợp lệ"); }
            case FINISHED -> { if (newStatus == PAID)
                this.status = newStatus;
            else throw new IllegalStateException("FINISHED → " + newStatus + " không hợp lệ"); }
            default       -> throw new IllegalStateException("Không thể đổi từ " + this.status);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int            getAuctionId()      { return auctionId; }
    public int            getSellerId()        { return sellerId; }
    public Item           getItem()            { return item; }
    public double         getCurrentPrice()    { return currentPrice; }
    public double         getBidIncrement()    { return bidIncrement; }
    public LocalDateTime  getStartTime()       { return startTime; }
    public LocalDateTime  getEndTime()         { return endTime; }
    public AuctionStatus  getStatus()          { return status; }
    public boolean        isBanned()           { return banned; }
    public int            getLeadingBidderId() { return leadingBidderId; }
    public List<Bid>      getBids()            { return Collections.unmodifiableList(bids); }
    public ReentrantLock  getLock() {
        if (lock == null) lock = new ReentrantLock();
        return lock;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setCurrentPrice(double p) {
        if (p < currentPrice)
            throw new IllegalArgumentException(
                    "Giá mới (" + p + ") phải >= giá hiện tại (" + currentPrice + ")");
        this.currentPrice = p;
    }

    public void setBidIncrement(double inc) {
        if (inc <= 0) throw new IllegalArgumentException("bidIncrement phải > 0");
        this.bidIncrement = inc;
    }

    public void setStartTime(LocalDateTime t) {
        Objects.requireNonNull(t, "startTime");
        if (endTime != null && !endTime.isAfter(t))
            throw new IllegalArgumentException("startTime phải trước endTime");
        this.startTime = t;
    }

    public void setEndTime(LocalDateTime t) {
        Objects.requireNonNull(t, "endTime");
        if (!t.isAfter(startTime))
            throw new IllegalArgumentException("endTime phải sau startTime (" + startTime + ")");
        this.endTime = t;
    }

    public void setBanned(boolean banned) { this.banned = banned; }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Auction)) return false;
        return auctionId == ((Auction) o).auctionId;
    }

    @Override
    public int hashCode() { return Integer.hashCode(auctionId); }

    // ── Observer interface (tách ra khỏi Auction) ─────────────────────────────

    public interface AuctionObserver {
        void onAuctionEvent(AuctionEvent event);
    }
}