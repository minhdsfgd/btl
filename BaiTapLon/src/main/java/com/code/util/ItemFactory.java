package com.code.util;

import com.code.models.Electronics;
import com.code.models.Art;
import com.code.models.Vehicle;
import com.code.models.Item;

public class ItemFactory {
    public static Item createItem(int itemId, ItemType itemType, String name, String description, double price) {
        return switch (itemType) {
            case VEHICLE -> new Vehicle(itemId, name, description);
            case ELECTRONICS -> new Electronics(itemId, name, description);
            case ART -> new Art(itemId, name, description);
            default -> throw new IllegalArgumentException();
        };
    }
}

