package com.code.models;

import java.util.Objects;

/**
 * Sản phẩm đưa lên đấu giá. Giá khởi điểm thuộc sản phẩm; giá hiện tại và thời gian phiên nằm ở {@link Auction}.
 */
public abstract class Item {
    private final int itemId;
    private final int sellerId;
    private String name;
    private String description;
    private double startingPrice;

    protected Item(int itemId, int sellerId, String name, String description, double startingPrice) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description != null ? description : "";
        if (startingPrice < 0) {
            throw new IllegalArgumentException("startingPrice must be >= 0");
        }
        this.startingPrice = startingPrice;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public void setStartingPrice(double startingPrice) {
        if (startingPrice < 0) {
            throw new IllegalArgumentException("startingPrice must be >= 0");
        }
        this.startingPrice = startingPrice;
    }

    public int getItemId() {
        return itemId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }
}
