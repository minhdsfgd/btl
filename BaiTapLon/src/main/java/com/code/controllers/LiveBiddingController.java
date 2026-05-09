package com.code.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class LiveBiddingController implements Initializable {
    private static SessionData pendingSessionData;

    // ── Header ───────────────────────────────────────────────────────────────
    @FXML private Label currentUserLabel;
    @FXML private Label sessionNameLabel;

    // ── Product info ─────────────────────────────────────────────────────────
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
    @FXML private TableView<BidRow>       bidHistoryTable;
    @FXML private TableColumn<BidRow, String> colUsername;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colAmount;

    // ── Internal state ────────────────────────────────────────────────────────
    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private Timeline countdownTimeline;
    private long remainingSeconds = 0;

    private String  currentUsername = "";
    private double  currentPrice    = 0;
    private double  minimumStep     = 10_000;   // minimum bid increment

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ========================================================================
    //  Initializable
    // ========================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupHistoryTable();
        clearError();
        applyPendingSessionData();
    }

    // ========================================================================
    //  Public API – inject data from outside
    // ========================================================================

    /** Set the name shown in the header for the logged-in user. */
    public void setCurrentUser(String username) {
        this.currentUsername = username;
        currentUserLabel.setText("👤  " + username);
    }

    public void setSessionName(String sessionName) {
        sessionNameLabel.setText(sessionName);
    }

    /**
     * Load product information into the left panel.
     *
     * @param name        product name
     * @param description short description
     * @param imageUrl    URL / file path for the product image (nullable)
     */
    public void setProduct(String name, String description, String imageUrl) {
        productNameLabel.setText(name);
        productDescLabel.setText(description);
        productCategoryLabel.setText("Đồ sưu tầm");
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                productImageView.setImage(new Image(imageUrl, true));
            } catch (Exception ignored) { /* keep blank if load fails */ }
        }
    }

    /**
     * Set the current highest bid and the leading bidder's name.
     * Also persists currentPrice for validation.
     */
    public void setCurrentPrice(double price, String leadingBidder) {
        this.currentPrice = price;
        currentPriceLabel.setText(formatPrice(price));
        leadingBidderLabel.setText(leadingBidder == null || leadingBidder.isBlank()
                ? "Chưa có người đặt giá"
                : "🏆  " + leadingBidder);
    }

    /**
     * Set minimum bid step (default 10 000 VND).
     */
    public void setMinimumStep(double step) {
        this.minimumStep = step;
        minStepLabel.setText(formatPrice(step));
    }

    public void setStartPrice(double price) {
        startPriceLabel.setText(formatPrice(price));
    }

    /**
     * Start (or restart) the countdown timer.
     *
     * @param totalSeconds remaining seconds in the auction
     */
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

    /** Stop the countdown without triggering the "ended" callback. */
    public void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    /**
     * Append a bid record to the history table (call after a successful bid
     * from any source – local or server push).
     */
    public void addBidToHistory(String username, double amount) {
        String time = LocalDateTime.now().format(TIME_FMT);
        BidRow row  = new BidRow(username, time, formatPrice(amount));
        // Insert at top so newest bid appears first
        bidHistory.add(0, row);
        bidHistoryTable.setItems(bidHistory);
    }

    // ========================================================================
    //  FXML handlers
    // ========================================================================

    @FXML
    private void handlePlaceBid() {
        clearError();
        String raw = bidAmountField.getText().trim().replaceAll("[^\\d]", "");

        if (raw.isEmpty()) {
            showError("Vui lòng nhập số tiền đặt giá.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showError("Số tiền không hợp lệ.");
            return;
        }

        if (amount <= currentPrice) {
            showError("Giá phải cao hơn giá hiện tại (" + formatPrice(currentPrice) + ").");
            return;
        }

        if (amount < currentPrice + minimumStep) {
            showError("Bước giá tối thiểu là " + formatPrice(minimumStep) + ".");
            return;
        }

        // ── Accepted ──
        setCurrentPrice(amount, currentUsername);
        addBidToHistory(currentUsername, amount);
        bidAmountField.clear();

        // TODO: send bid to server / WebSocket here
        System.out.printf("[BID] %s đặt %s%n", currentUsername, formatPrice(amount));
    }

    @FXML
    private void handleExitRoom() {
        stopCountdown();
        com.code.util.ControllerUtils.navigateTo("/com/code/views/AuctionList.fxml");
    }

    @FXML
    private void handleBackToList() {
        stopCountdown();
        com.code.util.ControllerUtils.navigateTo("/com/code/views/AuctionList.fxml");
    }

    public static void prepareSession(String username, String sessionName, String productName,
                                      String description, double startPrice, double currentPrice,
                                      double minStep, long countdownSeconds) {
        pendingSessionData = new SessionData(
                username, sessionName, productName, description,
                startPrice, currentPrice, minStep, countdownSeconds
        );
    }

    // ========================================================================
    //  Private helpers
    // ========================================================================

    private void setupHistoryTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Style columns
        colAmount.setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: #4caf50;");
        colTime.setStyle("-fx-alignment: CENTER;");

        bidHistoryTable.setItems(bidHistory);
        bidHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bidHistoryTable.setPlaceholder(
                new Label("Chưa có lịch sử đặt giá") {{
                    setStyle("-fx-text-fill: #4a7a5e; -fx-font-size: 11;");
                }});

        // Row factory: highlight the current user's bids
        bidHistoryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(BidRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setStyle("");
                } else if (row.getUsername().equals(currentUsername)) {
                    setStyle("-fx-background-color: #1b4d30;");
                } else {
                    setStyle("-fx-background-color: #0f3d2a;");
                }
            }
        });
    }

    private void updateCountdownLabel() {
        long h = remainingSeconds / 3600;
        long m = (remainingSeconds % 3600) / 60;
        long s = remainingSeconds % 60;
        String text = String.format("%02d:%02d:%02d", h, m, s);

        // Turn red when under 60 seconds
        String color = remainingSeconds <= 60 ? "#ef5350" : "#4dd0e1";
        Platform.runLater(() -> {
            countdownLabel.setText(text);
            countdownLabel.setStyle("-fx-text-fill: " + color
                    + "; -fx-font-size: 26; -fx-font-weight: bold;");
        });
    }

    /** Called automatically when the countdown reaches zero. */
    private void onAuctionEnded() {
        Platform.runLater(() -> {
            countdownLabel.setText("KẾT THÚC");
            countdownLabel.setStyle("-fx-text-fill: #ef5350; -fx-font-size: 22; -fx-font-weight: bold;");
            sessionStatusLabel.setText("Đã kết thúc");
            bidAmountField.setDisable(true);
            // TODO: show winner dialog / notify server
            System.out.println("[AUCTION] Phiên đấu giá đã kết thúc.");
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private String formatPrice(double price) {
        return String.format("%,.0f VND", price);
    }

    private void applyPendingSessionData() {
        if (pendingSessionData == null) {
            return;
        }
        setCurrentUser(pendingSessionData.username);
        setSessionName(pendingSessionData.sessionName);
        setProduct(pendingSessionData.productName, pendingSessionData.description, null);
        setStartPrice(pendingSessionData.startPrice);
        setCurrentPrice(pendingSessionData.currentPrice, pendingSessionData.username);
        setMinimumStep(pendingSessionData.minStep);
        startCountdown(pendingSessionData.countdownSeconds);
        pendingSessionData = null;
    }

    // ========================================================================
    //  Inner model class
    // ========================================================================

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
        private final String username;
        private final String sessionName;
        private final String productName;
        private final String description;
        private final double startPrice;
        private final double currentPrice;
        private final double minStep;
        private final long countdownSeconds;

        private SessionData(String username, String sessionName, String productName,
                            String description, double startPrice, double currentPrice,
                            double minStep, long countdownSeconds) {
            this.username = username;
            this.sessionName = sessionName;
            this.productName = productName;
            this.description = description;
            this.startPrice = startPrice;
            this.currentPrice = currentPrice;
            this.minStep = minStep;
            this.countdownSeconds = countdownSeconds;
        }
    }
}