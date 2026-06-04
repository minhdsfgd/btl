package com.code.controllers.seller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

//Kiểm tra tính hợp lệ của form tạo phiên đấu
// chỉ chịu trách nhiệm validate dữ liệu đầu vào của form auction,

public class AuctionFormValidator {

    public static final class ValidationResult {
        private final boolean valid;
        private final String  errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid        = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok()              { return new ValidationResult(true,  null); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg);  }

        public boolean isValid()              { return valid; }
        public Optional<String> getError()    { return Optional.ofNullable(errorMessage); }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    //valid toàn bộ phiên
    public ValidationResult validate(String selectedItem,
                                     String bidIncrementStr,
                                     LocalDate startDate, int startHour, int startMin,
                                     LocalDate endDate,   int endHour,   int endMin) {

        //  chọn sản phẩm
        if (selectedItem == null || selectedItem.isBlank()) {
            return ValidationResult.error("Vui lòng chọn sản phẩm.");
        }

        //Bước tăng giá phải là số dương
        double bidIncrement;
        try {
            bidIncrement = Double.parseDouble(bidIncrementStr.trim());
            if (bidIncrement <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            return ValidationResult.error("Bậc tăng giá phải là số lớn hơn 0.");
        }

        // Thời gian bắt đầu phải trong tương lai
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMin));
        if (startTime.isBefore(LocalDateTime.now())) {
            return ValidationResult.error("Thời gian bắt đầu phải là thời điểm trong tương lai.");
        }

        // Thời gian kết thúc phải sau bắt đầu
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMin));
        if (!endTime.isAfter(startTime)) {
            return ValidationResult.error("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        return ValidationResult.ok();
    }

    public double parseBidIncrement(String bidIncrementStr) {
        return Double.parseDouble(bidIncrementStr.trim());
    }

    public LocalDateTime toDateTime(LocalDate date, int hour, int min) {
        return LocalDateTime.of(date, LocalTime.of(hour, min));
    }
}