package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.controllers.live.AutoBidManager;
import com.code.controllers.live.BidChartManager;
import com.code.models.Auction;
import com.code.models.AuctionEvent;
import com.code.models.Bid;
import com.code.models.User;
import com.code.network.PlaceBidData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import com.code.util.ControllerUtils;
import com.code.viewmodel.BidRow;
import com.code.viewmodel.SessionData;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import static com.code.util.ControllerUtils.navigateTo;

/**
 * Controller màn hình đấu giá trực tiếp (LiveBidding).
 *
 * Sau refactor, controller chỉ còn 3 việc:
 *  1. Nhận input từ UI → gọi đúng helper/manager
 *  2. Nhận event từ server → cập nhật UI
 *  3. Điều phối BidChartManager và AutoBidManager
 *
 * Logic chart    → BidChartManager
 * Logic auto-bid → AutoBidManager
 * ViewModel      → BidRow, SessionData (package viewmodel)
 */
public class LiveBiddingController implements Initializable {

    // ── Static handoff từ AuctionListController ───────────────────────────────
    private static SessionData pendingSessionData;

    // ── FXML: Header ──────────────────────────────────────────────────────────
    @FXML private Label currentUserLabel;
    @FXML private Label balanceLiveLabel;
    @FXML private Label balanceLiveLabel2;
    @FXML private Label sessionNameLabel;

    // ── FXML: Product info ────────────────────────────────────────────────────
    @FXML private ImageView productImageView;
    @FXML private Label     productNameLabel;
    @FXML private Label     productDescLabel;
    @FXML private Label     productCategoryLabel;

    // ── FXML: Price ───────────────────────────────────────────────────────────
    @FXML private Label currentPriceLabel;
    @FXML private Label leadingBidderLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label minStepLabel;

    // ── FXML: Countdown ───────────────────────────────────────────────────────
    @FXML private Label countdownLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Label endTimeLabel;

    // ── FXML: Bid input ───────────────────────────────────────────────────────
    @FXML private TextField bidAmountField;
    @FXML private Label     errorLabel;
    @FXML private Button    quickBid1;
    @FXML private Button    quickBid2;
    @FXML private Button    quickBid3;

    // ── FXML: History table ───────────────────────────────────────────────────
    @FXML private TableView<BidRow>           bidHistoryTable;
    @FXML private TableColumn<BidRow, String> colUsername;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colAmount;

    // ── FXML: Chart ───────────────────────────────────────────────────────────
    @FXML private LineChart<Number, Number> bidChart;
    @FXML private NumberAxis                xAxis;
    @FXML private NumberAxis                yAxis;

    // ── FXML: Auto-bid ────────────────────────────────────────────────────────
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidStepField;
    @FXML private Button    autoStep1;
    @FXML private Button    autoStep2;
    @FXML private Button    autoStep3;
    @FXML private Button    toggleAutoBidButton;
    @FXML private Label     autoBidStatusBadge;
    @FXML private Label     autoBidMaxDisplay;
    @FXML private Label     autoBidStepDisplay;
    @FXML private Label     autoBidLastBidDisplay;
    @FXML private Label     autoBidErrorLabel;

    // ── Managers (khởi tạo sau initialize) ───────────────────────────────────
    private BidChartManager  chartManager;
    private AutoBidManager   autoBidManager;

    // ── Internal state ────────────────────────────────────────────────────────
    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private Timeline countdownTimeline;
    private long     remainingSeconds = 0;
    private int      currentAuctionId = -1;
    private int      auctionOwnerId   = -1;
    private String   currentUsername  = "";
    private double   currentPrice     = 0;
    private double   minimumStep      = 10_000;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================================
    //  Initializable
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupHistoryTable();
        clearError();
        // Chart manager khởi tạo trực tiếp — FXML inject đảm bảo bidChart != null
        chartManager = new BidChartManager(bidChart, yAxis);
        applyPendingSessionData();
    }

    // =========================================================================
    //  Static factory — gọi từ AuctionListController trước khi navigate
    // =========================================================================

    public static void prepareSession(int auctionId, String username,
                                      String sessionName, String productName,
                                      String description, String imageUrl,
                                      double startPrice, double currentPrice,
                                      double minStep, long countdownSeconds,
                                      String leadingBidder, int ownerId) {
        pendingSessionData = new SessionData(
                auctionId, username, sessionName, productName, description,
                imageUrl, startPrice, currentPrice, minStep,
                countdownSeconds, leadingBidder, ownerId
        );
    }

    // =========================================================================
    //  FXML Handlers — Bid
    // =========================================================================

    @FXML
    private void handlePlaceBid() {
        clearError();
        if (SessionManager.getUserId() == auctionOwnerId) {
            showError("Bạn không thể đặt giá cho sản phẩm của chính mình.");
            return;
        }

        String raw = bidAmountField.getText().trim().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) { showError("Vui lòng nhập số tiền đặt giá."); return; }

        double amount;
        try { amount = Double.parseDouble(raw); }
        catch (NumberFormatException ex) { showError("Số tiền không hợp lệ."); return; }

        if (amount <= currentPrice) {
            showError("Giá phải cao hơn giá hiện tại (" + formatPrice(currentPrice) + ")."); return;
        }
        if (amount < currentPrice + minimumStep) {
            showError("Bước giá tối thiểu là " + formatPrice(minimumStep) + "."); return;
        }

        final double finalAmount = amount;
        new Thread(() -> {
            try {
                SocketClient.getInstance().sendAsync(
                        Request.of(RequestType.PLACE_BID,
                                new PlaceBidData(currentAuctionId, finalAmount)));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Gửi bid thất bại: " + ex.getMessage()));
            }
        }, "place-bid-thread").start();

        bidAmountField.clear();
    }

    @FXML private void handleQuickBid1() { applyQuickBid(100_000); }
    @FXML private void handleQuickBid2() { applyQuickBid(500_000); }
    @FXML private void handleQuickBid3() { applyQuickBid(1_000_000); }

    // =========================================================================
    //  FXML Handlers — Auto-bid
    // =========================================================================

    @FXML private void handleAutoStep1() { if (autoBidManager != null) autoBidManager.fillMinStep(minimumStep); }
    @FXML private void handleAutoStep2() { if (autoBidManager != null) autoBidManager.fillStep(500_000); }
    @FXML private void handleAutoStep3() { if (autoBidManager != null) autoBidManager.fillStep(1_000_000); }

    @FXML
    private void handleToggleAutoBid() {
        if (autoBidManager != null) autoBidManager.toggle(minimumStep);
    }

    // =========================================================================
    //  FXML Handlers — Navigation
    // =========================================================================

    @FXML private void handleExitRoom()   { leave(); }
    @FXML private void handleBackToList() { leave(); }

    // =========================================================================
    //  Server integration
    // =========================================================================

    private void startWatchingAndListening() {
        if (currentAuctionId < 0) return;

        new Thread(() -> {
            try {
                // Bước 1: WATCH_AUCTION — đăng ký nhận event realtime
                Response watchRes = SocketClient.getInstance().sendRequest(
                        Request.of(RequestType.WATCH_AUCTION, currentAuctionId));

                // Bước 2: lấy lịch sử bid
                // Ưu tiên lấy từ Auction trả về. Nếu getBids() null/rỗng
                // thì gọi thêm GET_BID_HISTORY để đảm bảo không bao giờ trắng.
                List<Bid> bids = null;

                if (watchRes.isSuccess() && watchRes.getData() instanceof Auction auction) {
                    bids = auction.getBids();
                }

                // Fallback: getBids() null hoặc rỗng → gọi riêng
                if (bids == null || bids.isEmpty()) {
                    Response histRes = SocketClient.getInstance().sendRequest(
                            Request.of(RequestType.GET_BIDS_BY_AUCTION, currentAuctionId));
                    if (histRes.isSuccess()) {
                        bids = com.code.util.ControllerUtils.getResponseList(histRes);
                    }
                }

                final List<Bid> finalBids = bids;
                Platform.runLater(() -> {
                    if (finalBids != null && !finalBids.isEmpty()) {
                        populateBidHistory(finalBids);
                        System.out.println("[DEBUG] Loaded " + finalBids.size() + " bids");
                    } else {
                        System.out.println("[DEBUG] Bids null hoặc rỗng!");
                    }
                    initAutoBidManager();
                });

                // Bước 3: bắt đầu lắng nghe event realtime
                SocketClient.getInstance().startListening(obj -> Platform.runLater(() -> {
                    if      (obj instanceof AuctionEvent event)  handleServerEvent(event);
                    else if (obj instanceof Response    response) handleBidResponse(response);
                    else if (obj == null) { showError("Mất kết nối với server."); stopCountdown(); }
                }));

            } catch (Exception ex) {
                Platform.runLater(() -> showError("Không thể kết nối phiên: " + ex.getMessage()));
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
                addBidToHistory(who, amount);
            }
            case AUCTION_FINISHED -> {
                stopCountdown();
                countdownLabel.setText("KẾT THÚC");
                countdownLabel.setStyle("-fx-text-fill:#ef5350; -fx-font-size:22; -fx-font-weight:bold;");
                sessionStatusLabel.setText("Đã kết thúc");
                setAllBidInputsDisabled(true);
                refreshBalanceAsync();
                int winner = event.getWinnerBidderId();
                String msg = (winner == SessionManager.getUserId())
                        ? "Bạn đã thắng phiên đấu giá này!"
                        : "Phiên kết thúc. Người thắng: " + (winner == -1 ? "Không có" : "Người khác #" + winner);
                showInfoAlert(msg);
            }
            case AUCTION_CANCELED -> {
                stopCountdown();
                sessionStatusLabel.setText("Đã hủy");
                setAllBidInputsDisabled(true);
                showInfoAlert("⚠ Phiên đấu giá đã bị hủy.");
            }
            case STATUS_CHANGED -> sessionStatusLabel.setText(event.getNewStatus().name());
            case TIME_EXTENDED  -> {
                remainingSeconds += 60;
                if (endTimeLabel != null)
                    endTimeLabel.setText(
                            LocalDateTime.now().plusSeconds(remainingSeconds).format(TIME_FMT));
                countdownLabel.setStyle("-fx-text-fill:#ff9800; -fx-font-weight:bold;");
            }
        }
    }

    private void handleBidResponse(Response response) {
        if (!response.isSuccess()) {
            showError("Lỗi: " + response.getMessage());
            return;
        }
        clearError();
        if (response.getData() instanceof User updatedUser) {
            SessionManager.setUser(updatedUser);
            updateBalanceLabel();
        } else if (response.getData() instanceof Auction updatedAuction
                && autoBidManager != null) {
            autoBidManager.handleServerResponse(response, updatedAuction);
        }
    }

    // =========================================================================
    //  Public setters (gọi bởi applyPendingSessionData)
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
        loadProductImage(imageUrl);
    }

    public void setCurrentPrice(double price, String leadingBidder) {
        this.currentPrice = price;
        if (autoBidManager != null) autoBidManager.setCurrentPrice(price);
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
        if (endTimeLabel != null)
            endTimeLabel.setText(LocalDateTime.now().plusSeconds(totalSeconds).format(TIME_FMT));

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateCountdownLabel();
            if (remainingSeconds <= 0) { stopCountdown(); onAuctionEnded(); }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void stopCountdown() {
        if (countdownTimeline != null) { countdownTimeline.stop(); countdownTimeline = null; }
    }

    public void addBidToHistory(String username, double amount) {
        String time = LocalDateTime.now().format(TIME_FMT);
        bidHistory.add(0, new BidRow(username, time, formatPrice(amount)));
        bidHistoryTable.setItems(bidHistory);
        chartManager.addPoint(amount);
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    private void applyPendingSessionData() {
        if (pendingSessionData == null) return;
        SessionData sd = pendingSessionData;
        pendingSessionData = null; // clear ngay để tránh leak

        this.currentAuctionId = sd.auctionId;
        this.auctionOwnerId   = sd.ownerId;
        setCurrentUser(sd.username);
        setSessionName(sd.sessionName);
        setProduct(sd.productName, sd.description, sd.imageUrl);
        setStartPrice(sd.startPrice);
        setCurrentPrice(sd.currentPrice, sd.leadingBidder);
        setMinimumStep(sd.minStep);
        startCountdown(sd.countdownSeconds);
        updateBalanceLabel();
        startWatchingAndListening(); // lịch sử bid load bên trong
    }

    /** Khởi tạo AutoBidManager sau khi đã có currentPrice đúng */
    private void initAutoBidManager() {
        autoBidManager = new AutoBidManager(
                currentAuctionId, currentPrice,
                autoBidMaxField, autoBidStepField,
                toggleAutoBidButton,
                autoStep1, autoStep2, autoStep3,
                autoBidStatusBadge, autoBidMaxDisplay,
                autoBidStepDisplay, autoBidLastBidDisplay, autoBidErrorLabel,
                this::showError,
                this::showInfoAlert
        );
    }

    /**
     * Load lịch sử bid vào cả bảng lẫn chart.
     * Sort theo timestamp để chart luôn đồng biến.
     */
    private void populateBidHistory(List<Bid> bids) {
        if (bids == null) return;
        bidHistory.clear();

        List<Bid> sorted = new ArrayList<>(bids);
        sorted.sort(Comparator.comparing(Bid::getTimestamp)); // cũ → mới

        // Chart: cũ → mới (đồng biến)
        chartManager.loadHistory(sorted);

        // Bảng: mới → cũ (mới nhất lên đầu)
        for (int i = sorted.size() - 1; i >= 0; i--) {
            Bid bid = sorted.get(i);
            String who  = bid.getUserId() == SessionManager.getUserId()
                    ? currentUsername : "Người khác #" + bid.getUserId();
            String time = bid.getTimestamp().format(TIME_FMT);
            bidHistory.add(new BidRow(who, time, formatPrice(bid.getAmount())));
        }
    }

    private void applyQuickBid(double delta) {
        if (bidAmountField.isDisabled()) return;
        double suggested = Math.max(currentPrice + delta, currentPrice + minimumStep);
        bidAmountField.setText(String.format("%.0f", suggested));
        clearError();
    }

    private void leave() {
        stopCountdown();
        SocketClient.getInstance().stopListening();
        try { SocketClient.getInstance().sendAsync(Request.of(RequestType.UNWATCH_AUCTION)); }
        catch (Exception ignored) {}
        navigateTo("/com/code/views/AuctionList.fxml");
    }

    private void loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        new Thread(() -> {
            try {
                java.io.File imgFile = new java.io.File(imageUrl);
                if (!imgFile.exists()) return;
                Image img = new Image(imgFile.toURI().toString(), 400, 180, false, true, true);
                Platform.runLater(() -> { if (!img.isError()) productImageView.setImage(img); });
            } catch (Exception ignored) {}
        }, "load-product-image").start();
    }

    private void refreshBalanceAsync() {
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_MY_INFO, null));
                if (res.isSuccess() && res.getData() instanceof User freshUser) {
                    Platform.runLater(() -> {
                        SessionManager.setUser(freshUser);
                        updateBalanceLabel();
                    });
                }
            } catch (Exception ignored) {}
        }, "balance-refresh-thread").start();
    }

    private void updateBalanceLabel() {
        User u = SessionManager.getUser();
        if (u == null) return;
        java.text.NumberFormat nf =
                java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        String text = nf.format((long) u.getBalance()) + " ₫";
        balanceLiveLabel.setText(text);
        if (balanceLiveLabel2 != null) balanceLiveLabel2.setText(text);
    }

    private void updateCountdownLabel() {
        long h = remainingSeconds / 3600;
        long m = (remainingSeconds % 3600) / 60;
        long s = remainingSeconds % 60;
        String text  = String.format("%02d:%02d:%02d", h, m, s);
        String color = remainingSeconds <= 60 ? "#ef5350" : "#4dd0e1";
        countdownLabel.setText(text);
        countdownLabel.setStyle("-fx-text-fill:" + color
                + "; -fx-font-size:22; -fx-font-weight:bold;");
    }

    private void onAuctionEnded() {
        countdownLabel.setText("KẾT THÚC");
        countdownLabel.setStyle("-fx-text-fill:#ef5350; -fx-font-size:22; -fx-font-weight:bold;");
        sessionStatusLabel.setText("Đã kết thúc");
        setAllBidInputsDisabled(true);
        if (autoBidManager != null) autoBidManager.disable();
    }

    /** Vô hiệu hóa toàn bộ input đặt giá */
    private void setAllBidInputsDisabled(boolean disabled) {
        bidAmountField.setDisable(disabled);
        if (quickBid1 != null) quickBid1.setDisable(disabled);
        if (quickBid2 != null) quickBid2.setDisable(disabled);
        if (quickBid3 != null) quickBid3.setDisable(disabled);
    }

    private void setupHistoryTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        colAmount.setStyle("-fx-alignment:CENTER_RIGHT;");
        colTime.setStyle("-fx-alignment:CENTER;");

        colUsername.setCellFactory(col -> styledCell("#1a1a1a", null));
        colTime.setCellFactory(col     -> styledCell("#065f3b", "-fx-alignment:CENTER;"));
        colAmount.setCellFactory(col   -> styledCell("#065f3b", "-fx-alignment:CENTER_RIGHT;"));

        bidHistoryTable.setItems(bidHistory);
        bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bidHistoryTable.setPlaceholder(new Label("Chưa có lịch sử đặt giá") {{
            setStyle("-fx-text-fill:#4a7a5e; -fx-font-size:11;");
        }});
        bidHistoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BidRow row, boolean empty) {
                super.updateItem(row, empty);
                setStyle(empty || row == null
                        ? "-fx-background-color:#003333;"
                        : "-fx-background-color:white;");
            }
        });
    }

    /** Tạo TableCell với màu chữ và style tùy chọn */
    private TableCell<BidRow, String> styledCell(String color, String extra) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    String style = "-fx-text-fill:" + color + ";";
                    if (extra != null) style += extra;
                    setStyle(style);
                }
            }
        };
    }

    private void showError(String msg)   { if (errorLabel != null) errorLabel.setText(msg); }
    private void clearError()            { if (errorLabel != null) errorLabel.setText(""); }
    private void showInfoAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo phiên đấu giá");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    private String formatPrice(double p) { return String.format("%,.0f VND", p); }
}