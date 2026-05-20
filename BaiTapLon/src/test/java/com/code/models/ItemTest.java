package com.code.models;

import com.code.util.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    // =========================================================================
    // 1. TEST CÁC LOGIC CHUNG CỦA ABSTRACT CLASS (ITEM)
    // =========================================================================
    @Nested
    @DisplayName("Tests cho logic của class cha (Item)")
    class AbstractItemLogicTests {

        @Test
        @DisplayName("TC01: Khởi tạo với startingPrice < 0 phải ném IllegalArgumentException")
        void testConstructor_NegativePrice_ThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                // Mượn class Electronics để test logic constructor của Item
                new Electronics(1, 10, "iPhone", "Mô tả", -5000);
            });
            assertTrue(exception.getMessage().contains("startingPrice phải >= 0"));
        }

        @Test
        @DisplayName("TC02: Khởi tạo với name = null phải ném NullPointerException")
        void testConstructor_NullName_ThrowsException() {
            assertThrows(NullPointerException.class, () -> {
                new Electronics(1, 10, null, "Mô tả", 1000);
            });
        }

        @Test
        @DisplayName("TC03: Truyền description = null thì phải tự động chuyển thành chuỗi rỗng")
        void testConstructor_NullDescription_BecomesEmptyString() {
            Item item = new Electronics(1, 10, "iPhone", null, 1000);
            assertEquals("", item.getDescription(), "Description null phải được gán thành chuỗi rỗng");
        }

        @Test
        @DisplayName("TC04: Setter setStartingPrice ném lỗi khi set giá âm")
        void testSetStartingPrice_Negative() {
            Item item = new Electronics(1, 10, "iPhone", "Mô tả", 1000);
            assertThrows(IllegalArgumentException.class, () -> item.setStartingPrice(-100));
        }

        @Test
        @DisplayName("TC05: Setter setDescription với giá trị null chuyển thành rỗng")
        void testSetDescription_Null() {
            Item item = new Electronics(1, 10, "iPhone", "Mô tả", 1000);
            item.setDescription(null);
            assertEquals("", item.getDescription());
        }
    }

    // =========================================================================
    // 2. TEST CHO CLASS ELECTRONICS
    // =========================================================================
    @Nested
    @DisplayName("Tests cho class Electronics")
    class ElectronicsTests {

        @Test
        @DisplayName("TC06: Khởi tạo thành công và trả về đúng getType()")
        void testElectronicsTypeAndFields() {
            Electronics elec = new Electronics(1, 20, "Laptop", "Dell", 15000.0, "Dell", 12);
            assertEquals(ItemType.ELECTRONICS, elec.getType());
            assertEquals("Dell", elec.getBrand());
            assertEquals(12, elec.getWarrantyMonths());
        }

        @Test
        @DisplayName("TC07: warrantyMonths truyền vào số âm thì tự động gán bằng 0")
        void testNegativeWarranty_BecomesZero() {
            Electronics elec = new Electronics(1, 20, "Laptop", "Dell", 15000.0, "Dell", -5);
            assertEquals(0, elec.getWarrantyMonths(), "Bảo hành âm phải được đưa về 0");
        }

        @Test
        @DisplayName("TC08: Hàm toString() xuất ra đúng format")
        void testToString() {
            Electronics elec = new Electronics(1, 20, "Laptop", "Mô tả", 15000.0, "Dell", 12);
            String expected = String.format("Electronics{name='Laptop', brand='Dell', warranty=12 tháng, price=%,.0f}", 15000.0);
            assertEquals(expected, elec.toString());
        }
    }

    // =========================================================================
    // 3. TEST CHO CLASS ART
    // =========================================================================
    @Nested
    @DisplayName("Tests cho class Art")
    class ArtTests {

        @Test
        @DisplayName("TC09: Khởi tạo thành công và trả về đúng getType()")
        void testArtTypeAndFields() {
            Art art = new Art(2, 30, "Bức tranh", "Đẹp", 50000.0, "Picasso", "Sơn dầu");
            assertEquals(ItemType.ART, art.getType());
            assertEquals("Picasso", art.getArtistName());
            assertEquals("Sơn dầu", art.getMedium());
        }

        @Test
        @DisplayName("TC10: artistName là null thì tự động gán thành 'Khuyết danh'")
        void testNullArtistName_BecomesKhuyetDanh() {
            Art art = new Art(2, 30, "Bức tranh", "Đẹp", 50000.0, null, "Sơn dầu");
            assertEquals("Khuyết danh", art.getArtistName());
        }

        @Test
        @DisplayName("TC11: Hàm toString() xuất ra đúng format")
        void testToString() {
            Art art = new Art(2, 30, "Bức tranh", "Mô tả", 50000.0, "Picasso", "Sơn dầu");
            String expected = String.format("Art{name='Bức tranh', artist='Picasso', medium='Sơn dầu', price=%,.0f}", 50000.0);
            assertEquals(expected, art.toString());
        }
    }

    // =========================================================================
    // 4. TEST CHO CLASS VEHICLE
    // =========================================================================
    @Nested
    @DisplayName("Tests cho class Vehicle")
    class VehicleTests {

        @Test
        @DisplayName("TC12: Khởi tạo thành công và trả về đúng getType()")
        void testVehicleTypeAndFields() {
            Vehicle vehicle = new Vehicle(3, 40, "Xe hơi", "Bền", 300000.0, "29A-12345", 2020);
            assertEquals(ItemType.VEHICLE, vehicle.getType());
            assertEquals("29A-12345", vehicle.getLicensePlate());
            assertEquals(2020, vehicle.getYearMade());
        }

        @Test
        @DisplayName("TC13: licensePlate là null thì tự động gán thành chuỗi rỗng")
        void testNullLicensePlate_BecomesEmptyString() {
            Vehicle vehicle = new Vehicle(3, 40, "Xe hơi", "Bền", 300000.0, null, 2020);
            assertEquals("", vehicle.getLicensePlate());
        }

        @Test
        @DisplayName("TC14: Hàm toString() xuất ra đúng format")
        void testToString() {
            Vehicle vehicle = new Vehicle(3, 40, "Xe hơi", "Mô tả", 300000.0, "29A-12345", 2020);
            String expected = String.format("Vehicle{name='Xe hơi', plate='29A-12345', year=2020, price=%,.0f}", 300000.0);
            assertEquals(expected, vehicle.toString());
        }
    }
}