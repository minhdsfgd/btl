package com.code.models;

import com.code.util.ItemType;

import java.io.Serializable;
import java.util.Objects;

/**
 * Sản phẩm đưa lên đấu giá.
 * Implements Serializable vì Item nằm trong Auction được gửi qua Socket.
 */
public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int itemId;
    private final int sellerId;
    private String name;
    private String description;
    private double startingPrice;

    protected Item(int itemId, int sellerId, String name,
                   String description, double startingPrice) {
        if (startingPrice < 0)
            throw new IllegalArgumentException("startingPrice phải >= 0");
        this.itemId        = itemId;
        this.sellerId      = sellerId;
        this.name          = Objects.requireNonNull(name, "name");
        this.description   = description != null ? description : "";
        this.startingPrice = startingPrice;
    }

    /** Subclass override để trả về loại sản phẩm — thể hiện polymorphism. */
    public abstract ItemType getType();

    /** Subclass override để in thông tin đặc trưng. */
    @Override
    public abstract String toString();

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }
    public void setDescription(String desc) {
        this.description = desc != null ? desc : "";
    }
    public void setStartingPrice(double p) {
        if (p < 0) throw new IllegalArgumentException("startingPrice phải >= 0");
        this.startingPrice = p;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int    getItemId()        { return itemId; }
    public int    getSellerId()      { return sellerId; }
    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public double getStartingPrice() { return startingPrice; }
}