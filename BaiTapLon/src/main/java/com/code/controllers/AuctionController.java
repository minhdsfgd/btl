package com.code.controllers;
import com.code.service.AuctionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class AuctionController {

    private AuctionService auctionService; // Sẽ được tiêm (inject) vào từ Main/Router

    @FXML
    private TextField txtBidAmount;
    @FXML
    private Label lblCurrentPrice;

    private int currentAuctionId = 1; // ID của phiên đấu giá đang mở
    private int currentUserId = 101;  // ID của người dùng đang đăng nhập

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /*
     * Hàm này được gắn vào thuộc tính onAction của nút "Đấu giá" (Bid Button)
     */
    @FXML
    public void handlePlaceBid() {
        try {
            // Lấy và ép kiểu số tiền nhập từ giao diện
            double bidAmount = Double.parseDouble(txtBidAmount.getText());

            // Gọi Service để xử lý logic
            auctionService.placeBid(currentAuctionId, currentUserId, bidAmount);

            // Nếu không có lỗi gì xảy ra (code chạy qua được dòng trên):
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá thành công!");

            // Cập nhật lại giao diện (Ví dụ: hiển thị giá mới nhất)
            lblCurrentPrice.setText(String.valueOf(bidAmount));
            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            // Lỗi khi người dùng nhập chữ cái thay vì số
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            // Bắt lỗi từ Service ném ra (Ví dụ: Không đủ tiền, giá thấp quá...)
            showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá", e.getMessage());
        }
    }

    // Hàm tiện ích tạo Popup thông báo trên JavaFX
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}