package com.code.controllers.live;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.network.AutoBidData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 *  - Validate input khi bật auto-bid
 *  - Gửi AUTOBID_SET / AUTOBID_CANCEL lên server
 *  - Cập nhật trạng thái UI (badge, nút, input)
 *  - Callback ra ngoài khi bật/tắt thành công
**/
public class AutoBidManager {

    // ── UI fields (inject từ controller) ─────────────────────────────────────
    private final TextField tfMax;
    private final TextField tfStep;
    private final Button    btnToggle;
    private final Button    btnStep1;
    private final Button    btnStep2;
    private final Button    btnStep3;
    private final Label     lblBadge;
    private final Label     lblMaxDisplay;
    private final Label     lblStepDisplay;
    private final Label     lblLastBid;
    private final Label     lblError;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isActive      = false;
    private double  currentMax    = 0;
    private double  currentStep   = 0;

    // ── External dependencies ─────────────────────────────────────────────────
    private final int              auctionId;
    private final Consumer<String> onError;      // callback hiện lỗi lên controller
    private final Consumer<String> onAlert;      // callback hiện Alert lên controller
    private       double           currentPrice; // cập nhật realtime từ controller

    // ── Style constants ───────────────────────────────────────────────────────
    private static final String BTN_ON_STYLE  =
            "-fx-background-color:#555555; -fx-text-fill:white; -fx-font-weight:bold;";
    private static final String BTN_OFF_STYLE =
            "-fx-background-color:#8B0000; -fx-text-fill:white; -fx-font-weight:bold;";

    public AutoBidManager(int auctionId, double currentPrice,
                          TextField tfMax, TextField tfStep,
                          Button btnToggle,
                          Button btnStep1, Button btnStep2, Button btnStep3,
                          Label lblBadge, Label lblMaxDisplay,
                          Label lblStepDisplay, Label lblLastBid, Label lblError,
                          Consumer<String> onError, Consumer<String> onAlert) {
        this.auctionId     = auctionId;
        this.currentPrice  = currentPrice;
        this.tfMax         = tfMax;
        this.tfStep        = tfStep;
        this.btnToggle     = btnToggle;
        this.btnStep1      = btnStep1;
        this.btnStep2      = btnStep2;
        this.btnStep3      = btnStep3;
        this.lblBadge      = lblBadge;
        this.lblMaxDisplay = lblMaxDisplay;
        this.lblStepDisplay= lblStepDisplay;
        this.lblLastBid    = lblLastBid;
        this.lblError      = lblError;
        this.onError       = onError;
        this.onAlert       = onAlert;
        refreshUI();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Gọi khi giá hiện tại thay đổi để validate đúng */
    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }

    /** Gọi khi nhấn nút "BẬT / TẮT ĐẤU GIÁ TỰ ĐỘNG" */
    public void toggle(double minimumStep) {
        clearError();
        if (!isActive) {
            activate(minimumStep);
        } else {
            deactivate();
        }
    }

    /** Điền bước tối thiểu vào ô bước tăng */
    public void fillMinStep(double minimumStep) {
        tfStep.setText(String.format("%.0f", minimumStep));
    }

    /** Điền giá trị cố định vào ô bước tăng */
    public void fillStep(double value) {
        tfStep.setText(String.format("%.0f", value));
    }

    /** Vô hiệu hóa toàn bộ UI auto-bid (khi phiên kết thúc) */
    public void disable() {
        btnToggle.setDisable(true);
        tfMax.setDisable(true);
        tfStep.setDisable(true);
        btnStep1.setDisable(true);
        btnStep2.setDisable(true);
        btnStep3.setDisable(true);
    }

    /**
     * Xử lý Response trả về từ server sau AUTOBID_SET / AUTOBID_CANCEL.
     * Gọi từ LiveBiddingController.handleBidResponse().
     */
    public void handleServerResponse(Response response, Auction updatedAuction) {
        String msg = response.getMessage().toLowerCase();
        if (msg.contains("bật auto bid")) {
            isActive = true;
            int myId = SessionManager.getUserId();
            if (updatedAuction.getAutoBidders() != null
                    && updatedAuction.getAutoBidders().containsKey(myId)) {
                AutoBidData data = updatedAuction.getAutoBidders().get(myId);
                currentMax  = data.maxAmount;
                currentStep = data.step;
            }
            refreshUI();
            onAlert.accept("Đã BẬT đấu giá tự động thành công!");
        } else if (msg.contains("tắt auto bid")) {
            isActive    = false;
            currentMax  = 0;
            currentStep = 0;
            refreshUI();
            onAlert.accept("Đã TẮT đấu giá tự động.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void activate(double minimumStep) {
        String maxRaw  = tfMax.getText().trim().replaceAll("[^\\d]", "");
        String stepRaw = tfStep.getText().trim().replaceAll("[^\\d]", "");

        if (maxRaw.isEmpty() || stepRaw.isEmpty()) {
            setError("Vui lòng nhập giá trần và bước tăng.");
            return;
        }

        double maxBid, step;
        try {
            maxBid = Double.parseDouble(maxRaw);
            step   = Double.parseDouble(stepRaw);
        } catch (NumberFormatException e) {
            setError("Vui lòng nhập số hợp lệ.");
            return;
        }

        if (maxBid <= currentPrice) { setError("Giá trần phải cao hơn giá hiện tại."); return; }
        if (step < minimumStep)     { setError("Bước tăng không được nhỏ hơn bước tối thiểu."); return; }
        if (step <= 0)              { setError("Bước tăng phải lớn hơn 0."); return; }

        new Thread(() -> {
            try {
                SocketClient.getInstance().sendAsync(
                        Request.of(RequestType.AUTOBID_SET,
                                new AutoBidData(auctionId, maxBid, step)));
            } catch (Exception e) {
                Platform.runLater(() -> setError("Lỗi kết nối: " + e.getMessage()));
            }
        }, "autobid-set-thread").start();
    }

    private void deactivate() {
        new Thread(() -> {
            try {
                SocketClient.getInstance().sendAsync(
                        Request.of(RequestType.AUTOBID_CANCEL, auctionId));
            } catch (Exception e) {
                Platform.runLater(() -> setError("Lỗi kết nối: " + e.getMessage()));
            }
        }, "autobid-cancel-thread").start();
    }

    private void refreshUI() {
        if (isActive) {
            btnToggle.setText("⚡ TẮT ĐẤU GIÁ TỰ ĐỘNG");
            btnToggle.setStyle(BTN_ON_STYLE);
            if (lblBadge != null) {
                lblBadge.setText("ĐANG BẬT");
                lblBadge.setStyle("-fx-text-fill:#4caf50; -fx-font-weight:bold;");
            }
            if (lblMaxDisplay  != null) lblMaxDisplay.setText(formatPrice(currentMax));
            if (lblStepDisplay != null) lblStepDisplay.setText(formatPrice(currentStep));
            tfMax.setDisable(true);
            tfStep.setDisable(true);
            btnStep1.setDisable(true);
            btnStep2.setDisable(true);
            btnStep3.setDisable(true);
        } else {
            btnToggle.setText("⚡ BẬT ĐẤU GIÁ TỰ ĐỘNG");
            btnToggle.setStyle(BTN_OFF_STYLE);
            if (lblBadge != null) {
                lblBadge.setText("TẮT");
                lblBadge.setStyle("-fx-text-fill:#ef5350;");
            }
            if (lblMaxDisplay  != null) lblMaxDisplay.setText("Chưa đặt");
            if (lblStepDisplay != null) lblStepDisplay.setText("Chưa đặt");
            tfMax.setDisable(false);
            tfStep.setDisable(false);
            btnStep1.setDisable(false);
            btnStep2.setDisable(false);
            btnStep3.setDisable(false);
        }
    }

    private void setError(String msg) {
        if (lblError != null) lblError.setText(msg);
        onError.accept(msg);
    }

    private void clearError() {
        if (lblError != null) lblError.setText("");
    }

    private String formatPrice(double p) {
        return String.format("%,.0f VND", p);
    }
}