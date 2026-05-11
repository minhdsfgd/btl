package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.AuctionEvent;
import com.code.network.PlaceBidData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LiveBiddingController implements Initializable {

    // ── Static session data (truyền từ AuctionListController) ────────────────
    private static SessionData pendingSessionData;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label currentUserLabel;
    @FXML private Label sessionNameLabel;

    // ── Product info ──────────────────────────────────────────────────────────
    @FXML private ImageView productImageView;
    @FXML private Label     productNameLabel;
    @FXML private Label     productDescLabel;
    @FXML private Label     productCategoryLabel;

    // ── Price & bidder ────────────────────────────────────────────────────────
    @FXML private Label currentPriceLabel;
    @FXML private Label leadingBidderLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label minStepLabel;

    // ── Countdown ─────────────────────────────────────────────────────────────
    @FXML private Label countdownLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Label endTimeLabel;

    // ── Bid input ─────────────────────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Label     errorLabel;

    // ── History table ─────────────────────────────────────────────────────────
    @FXML private TableView<BidRow>           bidHistoryTable;
    @FXML private TableColumn<BidRow, String> colUsername;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colAmount;

    // ── Internal state ────────────────────────────────────────────────────────
    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private Timeline countdownTimeline;
    private long     remainingSeconds = 0;

    private int    currentAuctionId = -1;
    private String currentUsername  = "";
    private double currentPrice     = 0;
    private double minimumStep      = 10_000;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================================
    //  Initializable
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupHistoryTable();
        clearError();
        applyPendingSessionData();
        startWatchingAndListening();
    }

    // =========================================================================
    //  Static factory — gọi trước khi navigate tới màn hình này
    // =========================================================================

    /**
     * Đặt dữ liệu phiên trước khi navigate.
     * @param auctionId   ID thực của phiên trên server
     */
    public static void prepareSession(int auctionId, String username,
                                      String sessionName, String productName,
                                      String description, double startPrice,
                                      double currentPrice, double minStep,
                                      long countdownSeconds) {
        pendingSessionData = new SessionData(
                auctionId, username, sessionName, productName, description,
                startPrice, currentPrice, minStep, countdownSeconds
        );
    }

    // =========================================================================
    //  Server integration
    // =========================================================================

    /**
     * Gửi WATCH_AUCTION để đăng ký nhận sự kiện realtime,
     * sau đó bật listener đọc mọi object từ server.
     */
    private void startWatchingAndListening() {
        if (currentAuctionId < 0) return;

        new Thread(() -> {
            try {
                // 1. Đăng ký xem phiên — đồng bộ (nhận Response)
                Response watchRes = SocketClient.getInstance().sendRequest(
                        Request.of(RequestType.WATCH_AUCTION, currentAuctionId));
                System.out.println("[Live] WATCH_AUCTION: " + watchRes.getMessage());

                // 2. Bắt đầu lắng nghe — từ đây in stream có thể có:
                //    AuctionEvent (push) hoặc Response (phản hồi bid của mình)
                SocketClient.getInstance().startListening(obj -> {
                    Platform.runLater(() -> {
                        if (obj instanceof AuctionEvent event) {
                            handleServerEvent(event);
                        } else if (obj instanceof Response response) {
                            handleBidResponse(response);
                        } else if (obj == null) {
                            showError("Mất kết nối với server.");
                            stopCountdown();
                        }
                    });
                });

            } catch (Exception ex) {
                Platform.runLater(() ->
                        showError("Không thể kết nối phiên: " + ex.getMessage()));
            }
        }, "watch-auction-thread").start();
    }

    /** Xử lý AuctionEvent do server đẩy xuống. */
    private void handleServerEvent(AuctionEvent event) {
        switch (event.getType()) {
            case BID_PLACED -> {
                double amount = event.getBid().getAmount();
                int    bidder = event.getBid().getUserId();
                String who    = (bidder == SessionManager.getUserId())
                        ? currentUsername : "Người khác #" + bidder;
                setCurrentPrice(amount, who);
                addBidToHistory(who, amount);
            }
            case AUCTION_FINISHED -> {
                stopCountdown();
                countdownLabel.setText("KẾT THÚC");
                countdownLabel.setStyle("-fx-text-fill:#ef5350; -fx-font-size:22;"
                        + " -fx-font-weight:bold;");
                sessionStatusLabel.setText("Đã kết thúc");
                bidAmountField.setDisable(true);
                int winner = event.getWinnerBidderId();
                String winMsg = winner == SessionManager.getUserId()
                        ? "🏆 Bạn đã thắng phiên đấu giá này!"
                        : "Phiên kết thúc. Người thắng: #" + winner;
                showAlert(winMsg);
            }
            case AUCTION_CANCELED -> {
                stopCountdown();
                sessionStatusLabel.setText("Đã hủy");
                bidAmountField.setDisable(true);
                showAlert("⚠ Phiên đấu giá đã bị hủy.");
            }
            case STATUS_CHANGED -> {
                sessionStatusLabel.setText(event.getNewStatus().name());
            }
        }
    }

    /** Xử lý Response trả về từ PLACE_BID (đến qua listener). */
    private void handleBidResponse(Response response) {
        if (!response.isSuccess()) {
            showError("Đặt giá thất bại: " + response.getMessage());
        } else {
            clearError();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo phiên đấu giá");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // =========================================================================
    //  FXML handlers
    // =========================================================================

    @FXML
    private void handlePlaceBid() {
        clearError();

        // ← Thêm kiểm tra: button phải được enable (phiên đang RUNNING)
        if (bidAmountField.isDisabled()) {
            showError("Phiên đấu giá đã kết thúc hoặc bị hủy. Không thể đặt giá.");
            return;
        }

        String raw = bidAmountField.getText().trim().replaceAll("[^\\d]", "");

        if (raw.isEmpty()) { showError("Vui lòng nhập số tiền đặt giá."); return; }

        double amount;
        try { amount = Double.parseDouble(raw); }
        catch (NumberFormatException ex) { showError("Số tiền không hợp lệ."); return; }

        if (amount <= currentPrice) {
            showError("Giá phải cao hơn giá hiện tại (" + formatPrice(currentPrice) + ").");
            return;
        }
        if (amount < currentPrice + minimumStep) {
            showError("Bước giá tối thiểu là " + formatPrice(minimumStep) + ".");
            return;
        }

        // Gửi lên server bất đồng bộ — Response sẽ đến qua listener
        new Thread(() -> {
            try {
                SocketClient.getInstance().sendAsync(
                        Request.of(RequestType.PLACE_BID,
                                new PlaceBidData(currentAuctionId, amount)));
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showError("Gửi bid thất bại: " + ex.getMessage()));
            }
        }, "place-bid-thread").start();

        bidAmountField.clear();
    }

    @FXML
    private void handleExitRoom() {
        leave();
    }

    @FXML
    private void handleBackToList() {
        leave();
    }

    private void leave() {
        stopCountdown();
        sendUnwatch();
        com.code.util.ControllerUtils.navigateTo("/com/code/views/AuctionList.fxml");
    }

    private void sendUnwatch() {
        try {
            // Không thể dùng sendRequest() vì listener đang đọc in — gửi async
            SocketClient.getInstance().sendAsync(Request.of(RequestType.UNWATCH_AUCTION));
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Public setters (dùng trong applyPendingSessionData)
    // =========================================================================

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        currentUserLabel.setText("👤  " + username);
    }

    public void setSessionName(String name) {
        sessionNameLabel.setText(name);
    }

    public void setProduct(String name, String description, String imageUrl) {
        productNameLabel.setText(name);
        productDescLabel.setText(description);
        productCategoryLabel.setText("Đang đấu giá");
    }

    public void setCurrentPrice(double price, String leadingBidder) {
        this.currentPrice = price;
        currentPriceLabel.setText(formatPrice(price));
        leadingBidderLabel.setText(leadingBidder == null || leadingBidder.isBlank()
                ? "Chưa có người đặt giá" : "🏆  " + leadingBidder);
    }

    public void setMinimumStep(double step) {
        this.minimumStep = step;
        minStepLabel.setText(formatPrice(step));
    }

    public void setStartPrice(double price) {
        startPriceLabel.setText(formatPrice(price));
    }

    public void startCountdown(long totalSeconds) {
        stopCountdown();
        remainingSeconds = totalSeconds;
        updateCountdownLabel();
        sessionStatusLabel.setText("Đang diễn ra");
        endTimeLabel.setText(LocalDateTime.now().plusSeconds(totalSeconds).format(TIME_FMT));

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateCountdownLabel();
            if (remainingSeconds <= 0) {
                stopCountdown();
                onAuctionEnded();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    public void addBidToHistory(String username, double amount) {
        String time = LocalDateTime.now().format(TIME_FMT);
        bidHistory.add(0, new BidRow(username, time, formatPrice(amount)));
        bidHistoryTable.setItems(bidHistory);
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    private void applyPendingSessionData() {
        if (pendingSessionData == null) return;
        this.currentAuctionId = pendingSessionData.auctionId;
        setCurrentUser(pendingSessionData.username);
        setSessionName(pendingSessionData.sessionName);
        setProduct(pendingSessionData.productName, pendingSessionData.description, null);
        setStartPrice(pendingSessionData.startPrice);
        setCurrentPrice(pendingSessionData.currentPrice, pendingSessionData.username);
        setMinimumStep(pendingSessionData.minStep);
        startCountdown(pendingSessionData.countdownSeconds);
        pendingSessionData = null;
    }

    private void setupHistoryTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        colAmount.setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: #4caf50;");
        colTime.setStyle("-fx-alignment: CENTER;");

        bidHistoryTable.setItems(bidHistory);
        bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bidHistoryTable.setPlaceholder(new Label("Chưa có lịch sử đặt giá") {{
            setStyle("-fx-text-fill: #4a7a5e; -fx-font-size: 11;");
        }});

        bidHistoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BidRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) setStyle("");
                else if (row.getUsername().equals(currentUsername))
                    setStyle("-fx-background-color: #1b4d30;");
                else
                    setStyle("-fx-background-color: #0f3d2a;");
            }
        });
    }

    private void updateCountdownLabel() {
        long h = remainingSeconds / 3600;
        long m = (remainingSeconds % 3600) / 60;
        long s = remainingSeconds % 60;
        String text  = String.format("%02d:%02d:%02d", h, m, s);
        String color = remainingSeconds <= 60 ? "#ef5350" : "#4dd0e1";
        Platform.runLater(() -> {
            countdownLabel.setText(text);
            countdownLabel.setStyle("-fx-text-fill: " + color
                    + "; -fx-font-size: 26; -fx-font-weight: bold;");
        });
    }

    private void onAuctionEnded() {
        Platform.runLater(() -> {
            countdownLabel.setText("KẾT THÚC");
            countdownLabel.setStyle("-fx-text-fill: #ef5350; -fx-font-size: 22;"
                    + " -fx-font-weight: bold;");
            sessionStatusLabel.setText("Đã kết thúc");
            bidAmountField.setDisable(true);
        });
    }

    private void showError(String msg) { errorLabel.setText(msg); }
    private void clearError()          { errorLabel.setText(""); }
    private String formatPrice(double p) { return String.format("%,.0f VND", p); }

    // =========================================================================
    //  Inner classes
    // =========================================================================

    public static class BidRow {
        private final String username;
        private final String time;
        private final String amount;

        public BidRow(String username, String time, String amount) {
            this.username = username;
            this.time     = time;
            this.amount   = amount;
        }

        public String getUsername() { return username; }
        public String getTime()     { return time; }
        public String getAmount()   { return amount; }
    }

    private static class SessionData {
        final int    auctionId;
        final String username;
        final String sessionName;
        final String productName;
        final String description;
        final double startPrice;
        final double currentPrice;
        final double minStep;
        final long   countdownSeconds;

        SessionData(int auctionId, String username, String sessionName,
                    String productName, String description, double startPrice,
                    double currentPrice, double minStep, long countdownSeconds) {
            this.auctionId       = auctionId;
            this.username        = username;
            this.sessionName     = sessionName;
            this.productName     = productName;
            this.description     = description;
            this.startPrice      = startPrice;
            this.currentPrice    = currentPrice;
            this.minStep         = minStep;
            this.countdownSeconds = countdownSeconds;
        }
    }
}

