package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.models.AuctionStatus;
import com.code.models.Role;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.code.models.AuctionStatus.*;
import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class AuctionListController {

    // ── FXML fields ───────────────────────────────────────────────────────────

    @FXML private Label      usernameLabel;
    @FXML private Label      balanceLabel;
    @FXML private Button     depositButton;
    @FXML private Button     modeBuyerButton;
    @FXML private Button     modeSellerButton;
    @FXML private Button     logoutButton;
    @FXML private TextField  searchField;
    @FXML private Button     searchButton;
    @FXML private Button     refreshButton;
    @FXML private Button     filterAllButton;
    @FXML private Button     filterActiveButton;
    @FXML private Button     filterUpcomingButton;
    @FXML private Button     filterEndedButton;
    @FXML private Button     filterPendingPaymentButton;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label      resultCountLabel;
    @FXML private FlowPane   auctionListContainer;
    @FXML private AnchorPane rootPane;
    @FXML private ImageView  backgroundImage;
    @FXML private Label      navTitleLabel;

    // ── State ─────────────────────────────────────────────────────────────────

    private String currentFilter   = "ALL";
    private String currentCategory = "Tất cả";
    private final List<Auction> allAuctions = new ArrayList<>();

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Kích thước cố định của card — ảnh 4/10, info 6/10
     *  4 card/hàng: (1280 - 24px padding*2 - 12px gap*3) / 4 ≈ 295px */
    private static final double CARD_WIDTH  = 295;
    private static final double CARD_HEIGHT = 460;
    private static final double IMG_HEIGHT  = CARD_HEIGHT * 0.4;   // 184px
    private static final double INFO_HEIGHT = CARD_HEIGHT * 0.6;   // 276px

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private static final NumberFormat NF =
            NumberFormat.getInstance(Locale.of("vi", "VN"));

    // =========================================================================
    //  Initialize
    // =========================================================================

    @FXML
    public void initialize() {
        // Bind background
        if (backgroundImage != null && rootPane != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        usernameLabel.setText(SessionManager.getUsername());
        refreshBalance();

        // Handlers
        logoutButton .setOnAction(e -> { sendLogout(); SessionManager.clear(); navigateTo("/com/code/views/Login.fxml"); });
        depositButton.setOnAction(e -> handleDeposit());
        modeBuyerButton .setOnAction(e -> setBuyerMode());
        modeSellerButton.setOnAction(e -> setSellerMode());

        searchButton .setOnAction(e -> handleSearch());
        searchField  .setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleSearch(); });
        refreshButton.setOnAction(e -> loadAuctionsFromServer());

        filterAllButton          .setOnAction(e -> handleFilter("ALL",            filterAllButton));
        filterActiveButton       .setOnAction(e -> handleFilter("ACTIVE",         filterActiveButton));
        filterUpcomingButton     .setOnAction(e -> handleFilter("UPCOMING",       filterUpcomingButton));
        filterEndedButton        .setOnAction(e -> handleFilter("ENDED",          filterEndedButton));
        filterPendingPaymentButton.setOnAction(e -> handleFilter("PENDING",       filterPendingPaymentButton));

        categoryComboBox.getItems().addAll("Tất cả", "ELECTRONICS", "ART", "VEHICLE");
        categoryComboBox.setValue("Tất cả");
        categoryComboBox.setOnAction(e -> { currentCategory = categoryComboBox.getValue(); handleSearch(); });

        setActiveFilter(filterAllButton);
        setBuyerMode();
        loadAuctionsFromServer();
    }

    // =========================================================================
    //  Load từ server
    // =========================================================================

    private void loadAuctionsFromServer() {
        showLoading("Đang tải danh sách phiên đấu giá...");
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_ACTIVE_AUCTIONS));
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Auction> list = res.getDataAs(List.class);
                        allAuctions.clear();
                        if (list != null) allAuctions.addAll(list);
                        handleSearch();
                    } else {
                        showLoading("Lỗi tải dữ liệu: " + res.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showLoading("Mất kết nối server: " + ex.getMessage()));
            }
        }, "load-auctions").start();
    }

    private void refreshBalance() {
        if (SessionManager.getUser() != null)
            balanceLabel.setText(formatPrice(SessionManager.getUser().getBalance()));
    }

    private void sendLogout() {
        try { SocketClient.getInstance().sendRequest(Request.of(RequestType.LOGOUT)); }
        catch (Exception ignored) {}
    }

    // =========================================================================
    //  Deposit
    // =========================================================================

    private void handleDeposit() {
        TextInputDialog dialog = new TextInputDialog("100000");
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nạp tiền vào tài khoản");
        dialog.setContentText("Số tiền cần nạp (VNĐ):");
        dialog.showAndWait().ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.replace(",", "").replace(".", ""));
                if (amount <= 0) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền phải > 0"); return; }

                new Thread(() -> {
                    try {
                        Response res = SocketClient.getInstance()
                                .sendRequest(Request.of(RequestType.DEPOSIT, amount));
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                try {
                                    com.code.models.User u = res.getDataAs(com.code.models.User.class);
                                    SessionManager.setUser(u);
                                } catch (Exception ignored) {}
                                refreshBalance();
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", res.getMessage());
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", res.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", ex.getMessage()));
                    }
                }, "deposit-thread").start();

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền không hợp lệ.");
            }
        });
    }

    // =========================================================================
    //  Search / Filter
    // =========================================================================

    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        List<Auction> result = new ArrayList<>();
        for (Auction a : allAuctions) {
            boolean matchKeyword  = keyword.isEmpty()
                    || a.getItem().getName().toLowerCase().contains(keyword);
            boolean matchFilter   = matchesFilter(a);
            boolean matchCategory = "Tất cả".equals(currentCategory)
                    || a.getItem().getType().name().equalsIgnoreCase(currentCategory);
            if (matchKeyword && matchFilter && matchCategory) result.add(a);
        }
        resultCountLabel.setText("Tìm thấy " + result.size() + " phiên");
        renderList(result);
    }

    private void handleFilter(String filter, Button activeBtn) {
        currentFilter = filter;
        setActiveFilter(activeBtn);
        handleSearch();
    }

    private boolean matchesFilter(Auction a) {
        return switch (currentFilter) {
            case "ACTIVE"   -> a.getStatus() == AuctionStatus.RUNNING;
            case "UPCOMING" -> a.getStatus() == AuctionStatus.OPEN;
            case "ENDED"    -> a.getStatus().isTerminal();
            case "PENDING"  -> a.getStatus() == AuctionStatus.FINISHED
                    && a.getLeadingBidderId() == SessionManager.getUserId();
            default         -> true;
        };
    }

    // =========================================================================
    //  Render
    // =========================================================================

    private void renderList(List<Auction> items) {
        auctionListContainer.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("Không có phiên đấu giá nào phù hợp.");
            empty.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:13;");
            auctionListContainer.getChildren().add(empty);
            return;
        }
        for (Auction a : items)
            auctionListContainer.getChildren().add(buildCard(a));
    }

    private void showLoading(String msg) {
        auctionListContainer.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:13;");
        auctionListContainer.getChildren().add(lbl);
    }

    // =========================================================================
    //  buildCard — card cố định kích thước, nửa trên ảnh / nửa dưới thông tin
    // =========================================================================

    private VBox buildCard(Auction a) {
        int    bidCount = a.getBids().size();
        String catIcon  = getCategoryIcon(a.getItem().getType().name());

        // ── NỬAA TRÊN: vùng chứa ảnh 4/10 card, nền tối ─────────────────────
        // Kích thước khung ảnh vuông = IMG_HEIGHT - padding 2 bên
        double frameSize = IMG_HEIGHT - 20;  // 20px padding trên+dưới

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(CARD_WIDTH, IMG_HEIGHT);
        imageBox.setMinSize(CARD_WIDTH, IMG_HEIGHT);
        imageBox.setMaxSize(CARD_WIDTH, IMG_HEIGHT);
        imageBox.setStyle("-fx-background-color:#002b2b;");

        // Khung vuông chứa ảnh — có viền xanh, nền tối, bo góc nhẹ
        StackPane imageFrame = new StackPane();
        imageFrame.setPrefSize(frameSize, frameSize);
        imageFrame.setMinSize(frameSize, frameSize);
        imageFrame.setMaxSize(frameSize, frameSize);
        imageFrame.setStyle(
                "-fx-background-color:#001a1a;" +
                        "-fx-border-color:#6ee7b7;" +
                        "-fx-border-width:1.5;" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;");

        ImageView cardImage = new ImageView();
        cardImage.setFitWidth(frameSize - 8);   // 4px padding mỗi bên trong khung
        cardImage.setFitHeight(frameSize - 8);
        cardImage.setPreserveRatio(true);        // giữ tỉ lệ, không méo
        imageFrame.getChildren().add(cardImage);

        imageBox.getChildren().add(imageFrame);
        StackPane.setAlignment(imageFrame, Pos.CENTER);

        // Load ảnh async
        String imageUrl = a.getItem().getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            final double fs = frameSize - 8;
            new Thread(() -> {
                try {
                    java.io.File f = new java.io.File(imageUrl);
                    if (!f.exists()) return;
                    Image img = new Image(f.toURI().toString(),
                            fs, fs, true, true, true);   // preserveRatio=true
                    Platform.runLater(() -> { if (!img.isError()) cardImage.setImage(img); });
                } catch (Exception ignored) {}
            }, "card-img-" + a.getAuctionId()).start();
        }

        // ── NỬA DƯỚI: thông tin chiếm 6/10 card (252px) ──────────────────────
        // Giữ nguyên thiết kế gốc của bạn, chỉ khóa kích thước
        Label catLabel = new Label(catIcon + " " + a.getItem().getType().name());
        catLabel.setStyle(
                "-fx-background-color:#99D1D3; -fx-text-fill:#000022;" +
                        "-fx-font-size:10; -fx-background-radius:6; -fx-padding:2 8 2 8;");

        Label idLabel = new Label("Phiên " + a.getAuctionId());
        idLabel.setStyle("-fx-text-fill:#f0f2f1; -fx-font-size:10;");

        Label nameLabel = new Label(a.getItem().getName());
        nameLabel.setStyle(
                "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(CARD_WIDTH - 24);

        Label priceLabel = new Label("💰 " + formatPrice(a.getCurrentPrice()));
        priceLabel.setStyle(
                "-fx-text-fill:#6ee7b7; -fx-font-size:13; -fx-font-weight:bold;");

        Label startPriceLabel = new Label("Khởi điểm: " + formatPrice(a.getItem().getStartingPrice()));
        startPriceLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:10;");

        Label bidCountLabel = new Label("📊 " + bidCount + " lượt đặt");
        bidCountLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:10;");

        Label timeLabel = new Label("⏱ " + formatTimeInfo(a));
        timeLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:11;");

        Label startTimeLabel = new Label("Bắt đầu: " + a.getStartTime().format(FMT));
        startTimeLabel.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:9;");

        Label endTimeLabel = new Label("Kết thúc: " + a.getEndTime().format(FMT));
        endTimeLabel.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:9;");

        Label statusBadge = new Label(mapStatus(a.getStatus()));
        statusBadge.setStyle(getStatusStyle(a.getStatus()));

        Button actionBtn = new Button(getActionText(a));
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setStyle(
                "-fx-background-color:#16a34a; -fx-text-fill:white;" +
                        "-fx-background-radius:8; -fx-padding:6 12; -fx-cursor:hand;");
        actionBtn.setDisable(a.getStatus() == CANCELED);
        actionBtn.setOnAction(e -> handleAction(a, actionBtn, statusBadge));

        // Spacer đẩy nút xuống đáy infoBox
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox infoBox = new VBox(5,
                catLabel, idLabel, nameLabel,
                priceLabel, startPriceLabel, bidCountLabel,
                timeLabel, startTimeLabel, endTimeLabel,
                statusBadge,
                spacer,
                actionBtn
        );
        infoBox.setPadding(new Insets(10));
        // Khóa cứng 6/10 chiều cao — nút luôn nằm đáy, không bị tràn
        infoBox.setPrefSize(CARD_WIDTH, INFO_HEIGHT);
        infoBox.setMinSize(CARD_WIDTH, INFO_HEIGHT);
        infoBox.setMaxSize(CARD_WIDTH, INFO_HEIGHT);

        // ── Ghép card: imageBox trên, infoBox dưới, khớp nhau ────────────────
        VBox card = new VBox(0, imageBox, infoBox);
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setStyle(
                "-fx-background-color:#003333; -fx-padding:0;" +
                        "-fx-background-radius:12;" +
                        "-fx-border-color:#6ee7b7; -fx-border-radius:12; -fx-border-width:1;");
        return card;
    }

    // =========================================================================
    //  Action handler
    // =========================================================================

    private void handleAction(Auction a, Button actionBtn, Label statusBadge) {
        if (a.getStatus() == RUNNING) {
            long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), a.getEndTime());

            String leaderName;
            if (a.getLeadingBidderId() == -1)
                leaderName = "";
            else if (a.getLeadingBidderId() == SessionManager.getUserId())
                leaderName = SessionManager.getUsername();
            else
                leaderName = "Người khác #" + a.getLeadingBidderId();

            LiveBiddingController.prepareSession(
                    a.getAuctionId(),
                    SessionManager.getUsername(),
                    "Phiên #" + a.getAuctionId(),
                    a.getItem().getName(),
                    a.getItem().getDescription(),
                    a.getItem().getImageUrl(),
                    a.getItem().getStartingPrice(),
                    a.getCurrentPrice(),
                    a.getBidIncrement(),
                    Math.max(remaining, 0),
                    leaderName,
                    a.getItem().getSellerId()
            );
            navigateTo("/com/code/views/LiveBidding.fxml");

        } else if (a.getStatus() == OPEN) {
            String info = "Sản phẩm: "    + a.getItem().getName()
                    + "\nMô tả: "         + (a.getItem().getDescription() != null ? a.getItem().getDescription() : "Không có")
                    + "\nGiá khởi điểm: " + formatPrice(a.getItem().getStartingPrice())
                    + "\nBước giá: "      + formatPrice(a.getBidIncrement())
                    + "\nBắt đầu lúc: "  + a.getStartTime().format(FMT)
                    + "\nKết thúc lúc: " + a.getEndTime().format(FMT)
                    + "\nDanh mục: "      + a.getItem().getType().name();
            showAlert(Alert.AlertType.INFORMATION, "Chi tiết phiên #" + a.getAuctionId(), info);

        } else if (a.getStatus() == FINISHED
                && a.getLeadingBidderId() == SessionManager.getUserId()) {
            new Thread(() -> {
                try {
                    Response res = SocketClient.getInstance()
                            .sendRequest(Request.of(RequestType.MARK_AS_PAID, a.getAuctionId()));
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            actionBtn.setText("Xem chi tiết");
                            a.updateStatus(PAID);
                            statusBadge.setText(mapStatus(PAID));
                            statusBadge.setStyle(getStatusStyle(PAID));
                            showAlert(Alert.AlertType.INFORMATION,
                                    "Thanh toán thành công",
                                    "Bạn đã thanh toán cho phiên #" + a.getAuctionId());
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Lỗi: " + res.getMessage()).showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR, "Lỗi kết nối: " + ex.getMessage()).showAndWait());
                }
            }, "mark-as-paid").start();

        } else {
            int bidCount = a.getBids().size();
            String winnerInfo = "";
            if (a.getLeadingBidderId() != -1) {
                winnerInfo = "\nNgười thắng: "
                        + (a.getLeadingBidderId() == SessionManager.getUserId()
                        ? SessionManager.getUsername()
                        : "Người khác #" + a.getLeadingBidderId());
            }
            String info = "Sản phẩm: "     + a.getItem().getName()
                    + "\nTrạng thái: "     + mapStatus(a.getStatus())
                    + "\nGiá cuối: "       + formatPrice(a.getCurrentPrice())
                    + "\nSố lượt đặt: "   + bidCount
                    + winnerInfo
                    + "\nKết thúc lúc: "  + a.getEndTime().format(FMT);
            showAlert(Alert.AlertType.INFORMATION, "Kết quả phiên #" + a.getAuctionId(), info);
        }
    }

    // =========================================================================
    //  Style / format helpers
    // =========================================================================

    private String mapStatus(AuctionStatus s) {
        return switch (s) {
            case RUNNING  -> "🟢 Đang diễn ra";
            case OPEN     -> "🟡 Sắp mở";
            case FINISHED -> "🔴 Kết thúc";
            case PAID     -> "✅ Đã thanh toán";
            case CANCELED -> "❌ Đã hủy";
        };
    }

    private String formatTimeInfo(Auction a) {
        if (a.getStatus() == AuctionStatus.RUNNING) {
            long mins = ChronoUnit.MINUTES.between(LocalDateTime.now(), a.getEndTime());
            return mins > 0 ? mins + " phút còn lại" : "Sắp kết thúc";
        }
        if (a.getStatus() == AuctionStatus.OPEN)
            return "Bắt đầu lúc " + a.getStartTime().format(FMT);
        return "Kết thúc lúc " + a.getEndTime().format(FMT);
    }

    private String formatPrice(double price) {
        return NF.format((long) price) + " ₫";
    }

    private String getCategoryIcon(String type) {
        return switch (type.toUpperCase()) {
            case "ELECTRONICS" -> "📱";
            case "ART"         -> "🎨";
            case "VEHICLE"     -> "🚗";
            default            -> "📦";
        };
    }

    private String getStatusStyle(AuctionStatus s) {
        return switch (s) {
            case RUNNING -> "-fx-background-color:#d1fae5; -fx-text-fill:#065f3b;"
                    + "-fx-padding:3 10; -fx-background-radius:20; -fx-font-size:11;";
            case OPEN    -> "-fx-background-color:#fef9c3; -fx-text-fill:#854d0e;"
                    + "-fx-padding:3 10; -fx-background-radius:20; -fx-font-size:11;";
            default      -> "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;"
                    + "-fx-padding:3 10; -fx-background-radius:20; -fx-font-size:11;";
        };
    }

    private String getActionText(Auction a) {
        return switch (a.getStatus()) {
            case RUNNING -> "Vào phòng đấu giá →";
            case OPEN    -> "Xem chi tiết";
            case FINISHED -> {
                if (a.getLeadingBidderId() == SessionManager.getUserId()
                        && LocalDateTime.now().isBefore(a.getEndTime().plusHours(24)))
                    yield "Thanh toán";
                yield "Xem kết quả";
            }
            default -> "Xem kết quả";
        };
    }

    private void setBuyerMode() {
        modeBuyerButton.setStyle(
                "-fx-background-color:white; -fx-text-fill:#065f3b;" +
                        "-fx-font-weight:bold; -fx-font-size:12; -fx-background-radius:16;" +
                        "-fx-padding:4 12; -fx-cursor:hand;");
        modeSellerButton.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:rgba(255,255,255,0.8); -fx-font-size:12;" +
                        "-fx-background-radius:16; -fx-padding:4 12; -fx-cursor:hand;");
    }

    private void setSellerMode() {
        if (!SessionManager.hasRole(Role.SELLER) && !SessionManager.hasRole(Role.ADMIN)) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                    "Tài khoản của bạn chưa có quyền người bán.");
            return;
        }
        navigateTo("/com/code/views/SellerDashboard.fxml");
    }

    private void setActiveFilter(Button activeBtn) {
        String inactive = "-fx-background-color:#065f3b; -fx-text-fill:#9fe6c8;" +
                "-fx-background-radius:8; -fx-padding:5 12;";
        String active   = "-fx-background-color:#16a34a; -fx-text-fill:white;" +
                "-fx-background-radius:8; -fx-padding:5 12; -fx-font-weight:bold;";
        for (Button btn : new Button[]{ filterAllButton, filterActiveButton,
                filterUpcomingButton, filterEndedButton,
                filterPendingPaymentButton }) {
            if (btn != null) btn.setStyle(btn == activeBtn ? active : inactive);
        }
    }
}