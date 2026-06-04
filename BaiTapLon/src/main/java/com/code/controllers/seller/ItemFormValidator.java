package com.code.controllers.seller;

import com.code.models.Art;
import com.code.models.Electronics;
import com.code.models.Item;
import com.code.models.Vehicle;

import java.util.Optional;


public class ItemFormValidator {
    public static final class ValidationResult {
        private final boolean valid;
        private final String  errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid        = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok()              { return new ValidationResult(true,  null); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg);  }

        public boolean isValid()           { return valid; }
        public Optional<String> getError() { return Optional.ofNullable(errorMessage); }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    //valid liệu đầu vào của form tạo sản phẩm.


    public ValidationResult validate(String name, String startPriceStr,
                                     String type, String extra2) {
        //  Tên không được rỗng
        if (name == null || name.isBlank()) {
            return ValidationResult.error("Vui lòng nhập tên sản phẩm.");
        }

        // Giá khởi điểm phải là số dương
        String cleanPrice = startPriceStr == null ? "" : startPriceStr.trim().replaceAll("[^\\d.]", "");
        try {
            double price = Double.parseDouble(cleanPrice);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            return ValidationResult.error("Giá khởi điểm không hợp lệ (phải > 0).");
        }

        // Field số (warranty/yearMade) phải là số nguyên nếu được điền
        if (("ELECTRONICS".equals(type) || "VEHICLE".equals(type))
                && extra2 != null && !extra2.isBlank()) {
            String cleanNum = extra2.replaceAll("[^\\d]", "");
            if (cleanNum.isEmpty()) {
                String label = "ELECTRONICS".equals(type) ? "Bảo hành" : "Năm sản xuất";
                return ValidationResult.error(label + " phải là số nguyên.");
            }
        }

        return ValidationResult.ok();
    }

    //Tạo đối tượng {@link Item} từ dữ liệu form đã được validate hợp lệ./
    public Item buildItem(String type, int sellerId,
                          String name, String desc, String startPriceStr,
                          String extra1, String extra2, String imageBase64) {

        double startPrice = Double.parseDouble(
                startPriceStr.trim().replaceAll("[^\\d.]", ""));

        Item item = switch (type) {
            case "ELECTRONICS" -> {
                int warranty = parseIntSafe(extra2);
                String brand = extra1.isBlank() ? "Unknown" : extra1;
                yield new Electronics(0, sellerId, name, desc, startPrice, brand, warranty);
            }
            case "ART" -> {
                String artist = extra1.isBlank() ? "Khuyết danh" : extra1;
                yield new Art(0, sellerId, name, desc, startPrice, artist, extra2);
            }
            case "VEHICLE" -> {
                int year = parseIntSafe(extra2);
                yield new Vehicle(0, sellerId, name, desc, startPrice, extra1, year);
            }
            default -> throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        };

        item.setImageUrl(imageBase64);
        return item;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        String digits = s.replaceAll("[^\\d]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}