package com.code.models;

import com.code.util.ItemType;
import java.io.Serializable;

/** Tác phẩm nghệ thuật. Field riêng: artistName (tác giả), medium (chất liệu). */
public class Art extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artistName;
    private String medium; // vd: "Sơn dầu", "Màu nước", "Điêu khắc"

    public Art(int itemId, int sellerId, String name,
               String description, double startingPrice,
               String artistName, String medium) {
        super(itemId, sellerId, name, description, startingPrice);
        this.artistName = artistName != null ? artistName : "Khuyết danh";
        this.medium     = medium     != null ? medium     : "";
    }

    /** Constructor tương thích cũ. */
    public Art(int itemId, int sellerId, String name,
               String description, double startingPrice) {
        this(itemId, sellerId, name, description, startingPrice, "Khuyết danh", "");
    }

    @Override public ItemType getType() { return ItemType.ART; }

    public String getArtistName()          { return artistName; }
    public String getMedium()              { return medium; }
    public void   setArtistName(String a)  { this.artistName = a != null ? a : "Khuyết danh"; }
    public void   setMedium(String m)      { this.medium = m != null ? m : ""; }

    @Override
    public String toString() {
        return String.format("Art{name='%s', artist='%s', medium='%s', price=%,.0f}",
                getName(), artistName, medium, getStartingPrice());
    }
}