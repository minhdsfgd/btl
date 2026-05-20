package com.code.util;

import com.code.models.Art;
import com.code.models.Electronics;
import com.code.models.Item;
import com.code.models.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    // =========================================================================
    // 1. TEST TẠO ĐÚNG ĐỐI TƯỢNG TỪNG LOẠI (POLYMORPHISM)
    // =========================================================================

    @Test
    @DisplayName("TC01: createItem sinh ra đúng class Vehicle khi chọn ItemType.VEHICLE")
    void testCreateItem_Vehicle() {
        Item item = ItemFactory.createItem(1, ItemType.VEHICLE, 10, "VinFast VF8", "Xe điện", 80000000);

        // Xác nhận Java sinh ra đúng class con (Vehicle)
        assertInstanceOf(Vehicle.class, item);

        // Xác nhận hàm getType (override) trả về đúng enum
        assertEquals(ItemType.VEHICLE, item.getType());

        // Kiểm tra xem dữ liệu truyền vào có được gán đúng vào Object không
        assertEquals(1, item.getItemId());
        assertEquals(10, item.getSellerId());
        assertEquals("VinFast VF8", item.getName());
        assertEquals("Xe điện", item.getDescription());
        assertEquals(80000000, item.getStartingPrice());
    }

    @Test
    @DisplayName("TC02: createItem sinh ra đúng class Electronics khi chọn ItemType.ELECTRONICS")
    void testCreateItem_Electronics() {
        Item item = ItemFactory.createItem(2, ItemType.ELECTRONICS, 20, "MacBook Pro", "M3 Max", 60000000);

        assertInstanceOf(Electronics.class, item);
        assertEquals(ItemType.ELECTRONICS, item.getType());
    }

    @Test
    @DisplayName("TC03: createItem sinh ra đúng class Art khi chọn ItemType.ART")
    void testCreateItem_Art() {
        Item item = ItemFactory.createItem(3, ItemType.ART, 30, "Đêm đầy sao", "Tranh Van Gogh", 1000000);

        assertInstanceOf(Art.class, item);
        assertEquals(ItemType.ART, item.getType());
    }

    // =========================================================================
    // 2. TEST PHƯƠNG THỨC BỊ LOẠI BỎ (DEPRECATED)
    // =========================================================================

    @Test
    @DisplayName("TC04: Gọi hàm createItem cũ (deprecated) phải ném UnsupportedOperationException")
    @SuppressWarnings("deprecation") // Giữ lại để IDE không cảnh báo gạch ngang khi gọi hàm cũ
    void testCreateItem_Deprecated_ThrowsException() {

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            // 🟢 ĐÃ SỬA: Gọi đúng hàm cũ (4 tham số) để nó ném ra ngoại lệ
            ItemFactory.createItem(1, ItemType.ELECTRONICS, "iPhone", "Mô tả");
        });

        // Kiểm tra xem thông điệp lỗi trả về có đúng như trong ItemFactory đã định nghĩa không
        assertTrue(exception.getMessage().contains("Phải truyền sellerId và startingPrice"));
    }
}