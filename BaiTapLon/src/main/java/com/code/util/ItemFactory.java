package com.code.util;

import com.code.models.*;

public final class ItemFactory {

    private ItemFactory() {}

    /**
     * Tạo Item theo loại — Factory Method Pattern.
     *
     * @param itemId       ID duy nhất (lấy từ IdGenerator)
     * @param itemType     loại sản phẩm
     * @param sellerId     ID người bán (phải là user hợp lệ có role SELLER)
     * @param name         tên sản phẩm
     * @param description  mô tả
     * @param startingPrice giá khởi điểm
     */
    public static Item createItem(int itemId, ItemType itemType, int sellerId,
                                  String name, String description, double startingPrice) {
        return switch (itemType) {
            case VEHICLE     -> new Vehicle(itemId, sellerId, name, description, startingPrice);
            case ELECTRONICS -> new Electronics(itemId, sellerId, name, description, startingPrice);
            case ART         -> new Art(itemId, sellerId, name, description, startingPrice);
        };
    }

    /**
     * @deprecated Dùng {@link #createItem(int, ItemType, int, String, String, double)} thay thế.
     *             sellerId=0 là không hợp lệ — mọi Item phải có seller thật.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static Item createItem(int itemId, ItemType itemType,
                                  String name, String description) {
        throw new UnsupportedOperationException(
                "Phải truyền sellerId và startingPrice. " +
                        "Dùng createItem(itemId, itemType, sellerId, name, description, startingPrice).");
    }
}