package com.code.util;

import com.code.models.Art;
import com.code.models.Electronics;
import com.code.models.Item;
import com.code.models.Vehicle;

public final class ItemFactory {

    private ItemFactory() {
    }

    /**
     * @param sellerId id người bán (Seller)
     * @param startingPrice giá khởi điểm sản phẩm
     */
    public static Item createItem(int itemId, ItemType itemType, int sellerId, String name, String description,
                                    double startingPrice) {
        return switch (itemType) {
            case VEHICLE -> new Vehicle(itemId, sellerId, name, description, startingPrice);
            case ELECTRONICS -> new Electronics(itemId, sellerId, name, description, startingPrice);
            case ART -> new Art(itemId, sellerId, name, description, startingPrice);
        };
    }

    /** Tương thích code cũ: sellerId = 0, startingPrice = 0 */
    public static Item createItem(int itemId, ItemType itemType, String name, String description) {
        return createItem(itemId, itemType, 0, name, description, 0.0);
    }
}
