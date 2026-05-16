package com.code.controllers;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.AuctionEvent;
import com.code.models.Bid;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// THÊM MỚI: import chart
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import javafx.util.Duration;

public class LiveBiddingController implements Initializable {

    // ── Static session data (truyền từ AuctionListController) ─────────────────
    private static SessionData pendingSessionData;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label currentUserLabel;
    @FXML private Label balanceLiveLabel;         // Số dư ở navbar
    @FXML private Label balanceLiveLabel2;        // THÊM MỚI: Số dư ở card đặt giá
    @FXML private Label sessionNameLabel;

    // ── Product info ───────────────────────────────────────────────────────────
    @FXML private ImageView productImageView;
    @FXML private Label     productNameLabel;
    @FXML private Label     productDescLabel;
    @FXML private Label     productCategoryLabel;

    // ── Price & bidder ─────────────────────────────────────────────────────────
    @FXML private Label currentPriceLabel;
    @FXML private Label leadingBidderLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label minStepLabel;

    // ── Countdown ──────────────────────────────────────────────────────────────
    @FXML private Label countdownLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Label endTimeLabel;

    // ── Bid input ──────────────────────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Label     errorLabel;

    // THÊM MỚI: 3 nút đặt giá nhanh (+100k, +500k, +1M)
    @FXML private Button quickBid1;
    @FXML private Button quickBid2;
    @FXML private Button quickBid3;

    // ── History table ──────────────────────────────────────────────────────────
    @FXML private TableView<BidRow>           bidHistoryTable;
    @FXML private TableColumn<BidRow, String> colUsername;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colAmount;

    // THÊM MỚI: LineChart real-time và các trục
    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis                xAxis;
    @FXML private NumberAxis                yAxis;

    // ── Internal state ─────────────────────────────────────────────────────────
    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private Timeline countdownTimeline;
    private long     remainingSeconds = 0;

    private int    currentAuctionId = -1;
    private String currentUsername  = "";
    private double currentPrice     = 0;
    private double minimumStep      = 10_000;

    // THÊM MỚI: biến chart
    private XYChart.Series<Number, Number> bidSeries;
    private int bidPointCount = 0; // đếm số điểm đã vẽ

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================================
    //  Initializable
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupHistoryTable();
        setupBidChart();           // THÊM MỚI: khởi tạo chart
        clearError();
        applyPendingSessionData();
        startWatchingAndListening();
    }

    // =========================================================================
    //  THÊM MỚI: Khởi tạo LineChart
    // =========================================================================

    private void setupBidChart() {
        // Tạo series dữ liệu
        bidSeries = new XYChart.Series<>();
        bidSeries.setName("Giá đặt");
        bidChart.getData().add(bidSeries);
        bidChart.setLegendVisible(false);
        bidChart.setCreateSymbols(true);   // hiện chấm tròn tại mỗi điểm

        // Định dạng trục Y: rút gọn số lớn (12.500.000 → 12.5M)
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                double v = n.doubleValue();
                if (v >= 1_000_000_000) return String.format("%.1fB", v / 1_000_000_000);
                if (v >= 1_000_000)     return String.format("%.1fM", v / 1_000_000);
                if (v >= 1_000)         return String.format("%.0fk", v / 1_000);
                return String.format("%.0f", v);
            }
            @Override public Number fromString(String s) { return 0; }
        });

        // Style chart sau khi layout xong (dùng Platform.runLater để chắc chắn node đã tồn tại)
        Platform.runLater(() -> {
            // Màu nền vùng plot
            if (bidChart.lookup(".chart-plot-background") != null)
                bidChart.lookup(".chart-plot-background")
                        .setStyle("-fx-background-color:#002222;");

            // Màu đường và chấm series
            if (bidSeries.getNode() != null)
                bidSeries.getNode().setStyle("-fx-stroke:#6ee7b7; -fx-stroke-width:2px;");

            // Màu grid ngang
            bidChart.lookupAll(".chart-horizontal-grid-lines")
                    .forEach(n -> n.setStyle("-fx-stroke:rgba(110,231,183,0.2);"));

            // Màu label trục
            bidChart.lookupAll(".axis-label")
                    .forEach(n -> n.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:10px;"));
            bidChart.lookupAll(".tick-mark")
                    .forEach(n -> n.setStyle("-fx-stroke:#4a9a7a;"));
        });
    }

    // =========================================================================
    //  Static factory — gọi trước khi navigate tới màn hình này
    // =========================================================================

    public static void prepareSession(int auctionId, String username,
                                      String sessionName, String productName,
                                      String description,
                                      String imageUrl,          // THÊM MỚI tham số này
                                      double startPrice, double currentPrice,
                                      double minStep, long countdownSeconds,
                                      String leadingBidder) {
        pendingSessionData = new SessionData(
                auctionId, username, sessionName, productName, description,
                imageUrl,                                        // THÊM MỚI
                startPrice, currentPrice, minStep, countdownSeconds, leadingBidder
        );
    }

    // =========================================================================
    //  Server integration
    // =========================================================================

    private void startWatchingAndListening() {
        if (currentAuctionId < 0) return;

        new Thread(() -> {
            try {
                Response watchRes = SocketClient.getInstance().sendRequest(
                        Request.of(RequestType.WATCH_AUCTION, currentAuctionId));
                System.out.println("[Live] WATCH_AUCTION: " + watchRes.getMessage());

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

    private void handleServerEvent(AuctionEvent event) {
        switch (event.getType()) {
            case BID_PLACED -> {
                double amount = event.getBid().getAmount();
                int    bidder = event.getBid().getUserId();
                String who    = (bidder == SessionManager.getUserId())
                        ? currentUsername : "Người khác #" + bidder;
                setCurrentPrice(amount, who);
                addBidToHistory(who, amount); // THAY ĐỔI: hàm này giờ cũng cập nhật chart
            }
            case AUCTION_FINISHED -> {
                stopCountdown();
                countdownLabel.setText("KẾT THÚC");
                countdownLabel.setStyle("-fx-text-fill:#ef5350; -fx-font-size:22; -fx-font-weight:bold;");
                sessionStatusLabel.setText("Đã kết thúc");
                setQuickBidDisabled(true); // THÊM MỚI: disable nút nhanh khi kết thúc
                bidAmountField.setDisable(true);
                int winner = event.getWinnerBidderId();
                String winnerName;
                if (winner == -1) {
                    winnerName = "Không có người đặt giá";
                } else if (winner == SessionManager.getUserId()) {
                    winnerName = currentUsername;
                } else {
                    winnerName = "Người khác #" + winner;
                }
                String winMsg = winner == SessionManager.getUserId()
                        ? "🏆 Bạn đã thắng phiên đấu giá này!"
                        : "Phiên kết thúc. Người thắng: " + winnerName;
                showAlert(winMsg);
            }
            case AUCTION_CANCELED -> {
                stopCountdown();
                sessionStatusLabel.setText("Đã hủy");
                bidAmountField.setDisable(true);
                setQuickBidDisabled(true); // THÊM MỚI
                showAlert("⚠ Phiên đấu giá đã bị hủy.");
            }
            case STATUS_CHANGED -> {
                sessionStatusLabel.setText(event.getNewStatus().name());
            }
        }
    }

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

        if (bidAmountField.isDisabled()) {
            showError("Phiên đấu giá đã kết thúc hoặc bị hủy.");
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

    // THÊM MỚI: 3 handler đặt giá nhanh
    // Mỗi nút tính = currentPrice + delta rồi điền vào TextField
    @FXML private void handleQuickBid1() { applyQuickBid(100_000); }
    @FXML private void handleQuickBid2() { applyQuickBid(500_000); }
    @FXML private void handleQuickBid3() { applyQuickBid(1_000_000); }

    /**
     * Tính giá gợi ý = currentPrice + delta và điền vào bidAmountField.
     * Người dùng vẫn phải nhấn "ĐẶT GIÁ NGAY" để xác nhận.
     */
    private void applyQuickBid(double delta) {
        if (bidAmountField.isDisabled()) return;
        double suggested = currentPrice + delta;
        // Đảm bảo thoả mãn minimumStep
        if (delta < minimumStep) suggested = currentPrice + minimumStep;
        bidAmountField.setText(String.format("%.0f", suggested));
        clearError();
    }

    @FXML
    private void handleExitRoom() { leave(); }

    @FXML
    private void handleBackToList() { leave(); }

    private void leave() {
        stopCountdown();
        sendUnwatch();
        SocketClient.getInstance().stopListening();
        com.code.util.ControllerUtils.navigateTo("/com/code/views/AuctionList.fxml");
    }

    private void sendUnwatch() {
        try {
            SocketClient.getInstance().sendAsync(Request.of(RequestType.UNWATCH_AUCTION));
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Public setters
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

        // load ảnh thật từ server
        if (imageUrl != null && !imageUrl.isBlank()) {
            new Thread(() -> {
                try {
                    // true = background loading (không block UI thread)
                    Image img = new Image(imageUrl, 151, 120, true, true, true);
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            productImageView.setImage(img);
                        }
                        // Nếu lỗi load ảnh thì giữ nguyên placeholder (không làm gì)
                    });
                } catch (Exception ignored) { }
            }, "load-product-image").start();
        }
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

    /*ngoài thêm vào bảng, giờ còn thêm điểm vào biểu đồ.
     */
    public void addBidToHistory(String username, double amount) {
        String time = LocalDateTime.now().format(TIME_FMT);
        bidHistory.add(0, new BidRow(username, time, formatPrice(amount)));
        bidHistoryTable.setItems(bidHistory);

        // vẽ điểm mới lên chart
        int pointIndex = ++bidPointCount;
        XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(pointIndex, amount);
        bidSeries.getData().add(dataPoint);

        // Giữ tối đa 30 điểm trên chart để không quá rối
        if (bidSeries.getData().size() > 30) {
            bidSeries.getData().remove(0);
        }

        // Style điểm mới nhất màu đỏ nổi bật, các điểm cũ màu xanh
        Platform.runLater(() -> {
            // Reset màu điểm áp chót về xanh
            if (bidSeries.getData().size() >= 2) {
                XYChart.Data<Number, Number> prev =
                        bidSeries.getData().get(bidSeries.getData().size() - 2);
                if (prev.getNode() != null)
                    prev.getNode().setStyle(
                            "-fx-background-color:#6ee7b7, #002222;" +
                                    "-fx-background-radius:4px;");
            }
            // Màu điểm mới nhất — đỏ
            if (dataPoint.getNode() != null)
                dataPoint.getNode().setStyle(
                        "-fx-background-color:#ef5350, #002222;" +
                                "-fx-background-radius:5px;");

            // Đảm bảo màu đường series luôn xanh
            if (bidSeries.getNode() != null)
                bidSeries.getNode().setStyle(
                        "-fx-stroke:#6ee7b7; -fx-stroke-width:2px;");
        });
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    private void applyPendingSessionData() {
        if (pendingSessionData == null) return;
        this.currentAuctionId = pendingSessionData.auctionId;
        setCurrentUser(pendingSessionData.username);
        setSessionName(pendingSessionData.sessionName);

        //  truyền imageUrl vào setProduct
        setProduct(pendingSessionData.productName,
                pendingSessionData.description,
                pendingSessionData.imageUrl);

        setStartPrice(pendingSessionData.startPrice);
        setCurrentPrice(pendingSessionData.currentPrice, pendingSessionData.leadingBidder);
        setMinimumStep(pendingSessionData.minStep);
        startCountdown(pendingSessionData.countdownSeconds);

        //  cả balanceLiveLabel2 ở card đặt giá
        if (SessionManager.getUser() != null) {
            java.text.NumberFormat nf =
                    java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            String balanceText = nf.format((long) SessionManager.getUser().getBalance()) + " ₫";
            balanceLiveLabel.setText(balanceText);
            if (balanceLiveLabel2 != null) balanceLiveLabel2.setText(balanceText);
        }

        loadBidHistory();
        pendingSessionData = null;
    }

    private void loadBidHistory() {
        if (currentAuctionId < 0) return;

        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance().sendRequest(
                        Request.of(RequestType.GET_BIDS_BY_AUCTION, currentAuctionId));
                if (!res.isSuccess()) {
                    Platform.runLater(() -> showError("Không tải được lịch sử: " + res.getMessage()));
                    return;
                }

                Object data = res.getData();
                if (!(data instanceof List<?> bids)) return;

                Platform.runLater(() -> {
                    bidHistory.clear();
                    bidSeries.getData().clear(); //  xóa chart cũ
                    bidPointCount = 0;           // reset bộ đếm

                    // Duyệt từ cũ→mới để thêm vào chart theo thứ tự thời gian
                    for (int i = bids.size() - 1; i >= 0; i--) {
                        Object obj = bids.get(i);
                        if (!(obj instanceof Bid bid)) continue;

                        String username = bid.getUserId() == SessionManager.getUserId()
                                ? currentUsername : "Người khác #" + bid.getUserId();
                        String time = bid.getTimestamp().format(TIME_FMT);

                        // Thêm vào bảng (mới nhất lên đầu)
                        bidHistory.add(new BidRow(username, time, formatPrice(bid.getAmount())));

                        // THÊM MỚI: thêm vào chart theo thứ tự lịch sử
                        bidPointCount++;
                        bidSeries.getData().add(
                                new XYChart.Data<>(bidPointCount, bid.getAmount()));
                    }

                    bidHistory.sort(null); // nếu BidRow implements Comparable, hoặc xóa dòng này
                    // Style đường chart sau khi có dữ liệu
                    if (bidSeries.getNode() != null)
                        bidSeries.getNode().setStyle("-fx-stroke:#6ee7b7; -fx-stroke-width:2px;");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Lỗi tải lịch sử: " + ex.getMessage()));
            }
        }, "load-bid-history").start();
    }

    private void setupHistoryTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        colAmount.setStyle("-fx-alignment:CENTER_RIGHT; -fx-text-fill:#4caf50;");
        colTime.setStyle("-fx-alignment:CENTER;");

        bidHistoryTable.setItems(bidHistory);
        bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bidHistoryTable.setPlaceholder(new Label("Chưa có lịch sử đặt giá") {{
            setStyle("-fx-text-fill:#4a7a5e; -fx-font-size:11;");
        }});

        bidHistoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BidRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) setStyle("");
                else if (row.getUsername().equals(currentUsername))
                    setStyle("-fx-background-color:#1b4d30;");
                else
                    setStyle("-fx-background-color:#0f3d2a;");
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
            countdownLabel.setStyle("-fx-text-fill:" + color
                    + "; -fx-font-size:22; -fx-font-weight:bold;");
        });
    }

    private void onAuctionEnded() {
        Platform.runLater(() -> {
            countdownLabel.setText("KẾT THÚC");
            countdownLabel.setStyle("-fx-text-fill:#ef5350; -fx-font-size:22; -fx-font-weight:bold;");
            sessionStatusLabel.setText("Đã kết thúc");
            bidAmountField.setDisable(true);
            setQuickBidDisabled(true); // THÊM MỚI
        });
    }

    // THÊM MỚI: disable/enable 3 nút quick-bid cùng lúc
    private void setQuickBidDisabled(boolean disabled) {
        if (quickBid1 != null) quickBid1.setDisable(disabled);
        if (quickBid2 != null) quickBid2.setDisable(disabled);
        if (quickBid3 != null) quickBid3.setDisable(disabled);
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


    private/**
     * THAY ĐỔI: thêm field imageUrl
     */ static class SessionData {
        final int    auctionId;
        final String username;
        final String sessionName;
        final String productName;
        final String description;
        final String imageUrl;           // THÊM MỚI
        final double startPrice;
        final double currentPrice;
        final double minStep;
        final long   countdownSeconds;
        final String leadingBidder;

        SessionData(int auctionId, String username, String sessionName,
                    String productName, String description,
                    String imageUrl,   // THÊM MỚI
                    double startPrice, double currentPrice,
                    double minStep, long countdownSeconds, String leadingBidder) {
            this.auctionId        = auctionId;
            this.username         = username;
            this.sessionName      = sessionName;
            this.productName      = productName;
            this.description      = description;
            this.imageUrl         = imageUrl;   // THÊM MỚI
            this.startPrice       = startPrice;
            this.currentPrice     = currentPrice;
            this.minStep          = minStep;
            this.countdownSeconds = countdownSeconds;
            this.leadingBidder    = leadingBidder;
        }
    }
}