package com.code.util;

import com.code.models.Electronics;
import com.code.models.Art;
import com.code.models.Vehicle;
import com.code.models.Item;
import com.code.models.Seller;

import java.util.Map;

public class ItemFactory {
    public static Item createItem(int itemId, ItemType itemType, String name, String description, double price) {
        return switch (itemType) {
            case VEHICLE -> new Vehicle(itemId, name, description, price);
            case ELECTRONICS -> new Electronics(itemId, name, description, price);
            case ART -> new Art(itemId, name, description, price);
            default -> throw new IllegalArgumentException();
        };
    }
}

