package com.code.models;

import com.code.util.ItemType;
import java.io.Serializable;

/** Thiết bị điện tử. Field riêng: brand (hãng), warrantyMonths (bảo hành). */
public class Electronics extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String brand;
    private int warrantyMonths;

    public Electronics(int itemId, int sellerId, String name,
                       String description, double startingPrice,
                       String brand, int warrantyMonths) {
        super(itemId, sellerId, name, description, startingPrice);
        this.brand          = brand != null ? brand : "";
        this.warrantyMonths = Math.max(0, warrantyMonths);
    }

    /** Constructor tương thích cũ — brand và warranty để trống. */
    public Electronics(int itemId, int sellerId, String name,
                       String description, double startingPrice) {
        this(itemId, sellerId, name, description, startingPrice, "", 0);
    }

    @Override public ItemType getType() { return ItemType.ELECTRONICS; }

    public String getBrand()          { return brand; }
    public int    getWarrantyMonths() { return warrantyMonths; }
    public void   setBrand(String b)  { this.brand = b != null ? b : ""; }

    @Override
    public String toString() {
        return String.format("Electronics{name='%s', brand='%s', warranty=%d tháng, price=%,.0f}",
                getName(), brand, warrantyMonths, getStartingPrice());
    }
}