package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.models.AuctionStatus;
import com.code.models.Role;
import com.code.models.User;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.code.models.AuctionStatus.*;
import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;
import static com.code.util.ControllerUtils.handleBanResponse;

public class AuctionListController {

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
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label      resultCountLabel;
    @FXML private FlowPane   auctionListContainer;
    @FXML private AnchorPane rootPane;
    @FXML private ImageView  backgroundImage;
    @FXML private Label      navTitleLabel;
    @FXML private Button     filterPendingPaymentButton;

    private String currentFilter   = "ALL";
    private String currentCategory = "Tất cả";
    private final List<Auction> allAuctions = new ArrayList<>();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private static final NumberFormat NF = NumberFormat.getInstance(Locale.of("vi", "VN"));

    // =========================================================================
    //  Initialize
    // =========================================================================

    @FXML
    public void initialize() {
        if (backgroundImage != null && rootPane != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        usernameLabel.setText(SessionManager.getUsername());
        getBalanceFromServer();

        logoutButton.setOnAction(e -> {
            sendLogout();
            SessionManager.clear();
            navigateTo("/com/code/views/Login.fxml");
        });

        depositButton.setOnAction(e -> handleDeposit());
        modeBuyerButton.setOnAction(e -> setBuyerMode());
        modeSellerButton.setOnAction(e -> setSellerMode());

        searchButton.setOnAction(e -> handleSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleSearch();
        });
        refreshButton.setOnAction(e -> {
            loadAuctionsFromServer();
            getBalanceFromServer();
        });

        filterAllButton     .setOnAction(e -> handleFilter("ALL",             filterAllButton));
        filterActiveButton  .setOnAction(e -> handleFilter("ACTIVE",          filterActiveButton));
        filterUpcomingButton.setOnAction(e -> handleFilter("UPCOMING",        filterUpcomingButton));
        filterEndedButton   .setOnAction(e -> handleFilter("ENDED",           filterEndedButton));
        filterPendingPaymentButton.setOnAction(e -> handleFilter("PENDING_PAYMENT", filterPendingPaymentButton));

        categoryComboBox.getItems().addAll("Tất cả", "ELECTRONICS", "ART", "VEHICLE");
        categoryComboBox.setValue("Tất cả");
        categoryComboBox.setOnAction(e -> {
            currentCategory = categoryComboBox.getValue();
            handleSearch();
        });

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
                    if (handleBanResponse(res)) return;
                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Auction> list = res.getDataAs(List.class);
                        allAuctions.clear();
                        if (list != null) allAuctions.addAll(list);
                        Collections.reverse(allAuctions);
                        handleSearch();
                    } else {
                        showLoading("Lỗi tải dữ liệu: " + res.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showLoading("Mất kết nối server: " + ex.getMessage()));
            }
        }, "load-auctions").start();
    }

    private void refreshBalance() {
        if (SessionManager.getUser() != null) {
            balanceLabel.setText(formatPrice(SessionManager.getUser().getBalance()));
        }
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
                double amount = Double.parseDouble(
                        input.replace(",", "").replace(".", ""));
                if (amount <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền phải > 0");
                    return;
                }

                new Thread(() -> {
                    try {
                        Response res = SocketClient.getInstance()
                                .sendRequest(Request.of(RequestType.DEPOSIT, amount));
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                if (res.getData() != null) {
                                    try {
                                        com.code.models.User u =
                                                res.getDataAs(com.code.models.User.class);
                                        SessionManager.setUser(u);
                                    } catch (Exception ignored) {}
                                }
                                refreshBalance();
                                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                        res.getMessage());
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", res.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() ->
                                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                                        ex.getMessage()));
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
            case "ACTIVE"          -> a.getStatus() == RUNNING;
            case "UPCOMING"        -> a.getStatus() == OPEN;
            case "ENDED"           -> a.getStatus().isTerminal();
            case "PENDING_PAYMENT" -> a.getStatus() == FINISHED
                    && a.getLeadingBidderId() == SessionManager.getUserId();
            default -> true;
        };
    }

    // =========================================================================
    //  Render
    // =========================================================================

    private void renderList(List<Auction> items) {
        auctionListContainer.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("Không có phiên đấu giá nào phù hợp.");
            empty.setStyle("-fx-text-fill:#1a1a1a; -fx-font-size:13;");
            auctionListContainer.getChildren().add(empty);
            return;
        }

        for (Auction a : items) {
            auctionListContainer.getChildren().add(buildCard(a));
        }
    }

    private void showLoading(String msg) {
        auctionListContainer.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:13;");
        auctionListContainer.getChildren().add(lbl);
    }

    private VBox buildCard(Auction a) {
        String statusStr     = mapStatus(a.getStatus());
        String timeInfo      = formatTimeInfo(a);
        String priceStr      = formatPrice(a.getCurrentPrice());
        String startPriceStr = formatPrice(a.getItem().getStartingPrice());
        String catIcon       = getCategoryIcon(a.getItem().getType().name());
        int    bidCount      = a.getBids().size();

        // ── Ảnh sản phẩm: có padding xung quanh, bo góc, không sát mép card ────
        // Kích thước ảnh = card width (250) - padding 2 bên (12+12) = 226
        // Chiều cao cố định 130px
        ImageView cardImage = new ImageView();
        cardImage.setFitWidth(226);
        cardImage.setFitHeight(130);
        cardImage.setPreserveRatio(false); // fill full ô

        // Bo góc ảnh 8px
        javafx.scene.shape.Rectangle baseClip =
                new javafx.scene.shape.Rectangle(226, 130);
        baseClip.setArcWidth(8);
        baseClip.setArcHeight(8);
        cardImage.setClip(baseClip);

        // Nền placeholder khi chưa có ảnh
        javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane(cardImage);
        imageContainer.setPrefSize(226, 130);
        imageContainer.setMaxSize(226, 130);
        imageContainer.setMinSize(226, 130);
        imageContainer.setStyle(
                "-fx-background-color:#002222;"
                        + "-fx-background-radius:8;"
        );

        String imageUrl = a.getItem().getImageUrl();
        System.out.println("[DEBUG] Card imageUrl: " + imageUrl);
        if (imageUrl != null && !imageUrl.isBlank()) {
            new Thread(() -> {
                try {
                    java.io.File imgFile = new java.io.File(imageUrl);
                    if (!imgFile.exists()) return;

                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                            imgFile.toURI().toString(), 226, 130, false, true, true
                    );
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            cardImage.setImage(img);
                            javafx.scene.shape.Rectangle clip =
                                    new javafx.scene.shape.Rectangle(226, 130);
                            clip.setArcWidth(8);
                            clip.setArcHeight(8);
                            cardImage.setClip(clip);
                        }
                    });
                } catch (Exception ignored) {}
            }, "card-img-" + a.getAuctionId()).start();
        }

        // ── Labels ────────────────────────────────────────────────────────────
        Label catLabel = new Label(catIcon + " " + a.getItem().getType().name());
        catLabel.setStyle(
                "-fx-background-color:#99D1D3; -fx-text-fill:#000022;"
                        + "-fx-font-size:10; -fx-background-radius:6; -fx-padding:2 8 2 8;");

        Label idLabel = new Label("Phiên " + a.getAuctionId());
        idLabel.setStyle("-fx-text-fill:#f0f2f1; -fx-font-size:10;");

        Label nameLabel = new Label(a.getItem().getName());
        nameLabel.setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label priceLabel = new Label("💰 " + priceStr);
        priceLabel.setStyle("-fx-text-fill:#6ee7b7; -fx-font-size:13; -fx-font-weight:bold;");

        Label startPriceLabel = new Label("Khởi điểm: " + startPriceStr);
        startPriceLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:10;");

        Label bidCountLabel = new Label("📊 " + bidCount + " lượt đặt");
        bidCountLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:10;");

        Label timeLabel = new Label("⏱ " + timeInfo);
        timeLabel.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:11;");

        Label startTimeLabel = new Label("Bắt đầu: " + a.getStartTime().format(FMT));
        startTimeLabel.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:9;");

        Label endTimeLabel = new Label("Kết thúc: " + a.getEndTime().format(FMT));
        endTimeLabel.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:9;");

        Label statusBadge = new Label(statusStr);
        statusBadge.setStyle(getStatusStyle(a.getStatus()));

        Button actionBtn = new Button(getActionText(a));
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setStyle(
                "-fx-background-color:#16a34a; -fx-text-fill:white;"
                        + "-fx-background-radius:8; -fx-padding:6 12; -fx-cursor:hand;");
        actionBtn.setDisable(a.getStatus() == CANCELED);
        actionBtn.setOnAction(e -> handleAction(a, actionBtn, statusBadge));

        // ── Card: padding đều 12px, ảnh nằm trong padding (không sát mép) ───────
        VBox card = new VBox(8,
                imageContainer,
                catLabel, idLabel, nameLabel,
                priceLabel, startPriceLabel, bidCountLabel,
                timeLabel, startTimeLabel, endTimeLabel,
                statusBadge, actionBtn
        );
        card.setPrefWidth(250);
        card.setStyle(
                "-fx-background-color:#003333; -fx-padding:12;"
                        + "-fx-background-radius:12;"
                        + "-fx-border-color:#6ee7b7; -fx-border-radius:12; -fx-border-width:1;");
        return card;
    }

    // =========================================================================
    //  Action
    // =========================================================================

    private void handleAction(Auction a, Button actionBtn, Label statusBadge) {
        if (a.getStatus() == RUNNING) {

            long remaining = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(), a.getEndTime());

            String leaderName;
            if (a.getLeadingBidderId() == -1) {
                leaderName = "";
            } else if (a.getLeadingBidderId() == SessionManager.getUserId()) {
                leaderName = SessionManager.getUsername();
            } else {
                leaderName = "Người khác #" + a.getLeadingBidderId();
            }

            String imageUrl = a.getItem().getImageUrl();

            LiveBiddingController.prepareSession(
                    a.getAuctionId(),
                    SessionManager.getUsername(),
                    "Phiên #" + a.getAuctionId(),
                    a.getItem().getName(),
                    a.getItem().getDescription(),
                    imageUrl,
                    a.getItem().getStartingPrice(),
                    a.getCurrentPrice(),
                    a.getBidIncrement(),
                    Math.max(remaining, 0),
                    leaderName, a.getItem().getSellerId()
            );

            navigateTo("/com/code/views/LiveBidding.fxml");

        } else if (a.getStatus() == OPEN) {
            String info = "Sản phẩm: "        + a.getItem().getName()
                    + "\nMô tả: "             + (a.getItem().getDescription() != null
                    ? a.getItem().getDescription() : "Không có")
                    + "\nGiá khởi điểm: "     + formatPrice(a.getItem().getStartingPrice())
                    + "\nBước giá: "          + formatPrice(a.getBidIncrement())
                    + "\nBắt đầu lúc: "       + a.getStartTime().format(FMT)
                    + "\nKết thúc lúc: "      + a.getEndTime().format(FMT)
                    + "\nDanh mục: "          + a.getItem().getType().name();
            showAlert(Alert.AlertType.INFORMATION,
                    "Chi tiết phiên #" + a.getAuctionId(), info);

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
                                    "Bạn đã thực hiện thanh toán cho phiên #" + a.getAuctionId());
                        } else {
                            Alert err = new Alert(Alert.AlertType.ERROR, "Lỗi: " + res.getMessage());
                            err.showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert err = new Alert(Alert.AlertType.ERROR,
                                "Lỗi kết nối: " + ex.getMessage());
                        err.showAndWait();
                    });
                }
                getBalanceFromServer();
            }, "mark-as-paid-lot").start();

        } else {
            int bidCount = a.getBids().size();
            String winnerInfo = "";
            if (a.getLeadingBidderId() != -1) {
                winnerInfo = "\nNgười thắng: "
                        + (a.getLeadingBidderId() == SessionManager.getUserId()
                        ? SessionManager.getUsername()
                        : "Người khác #" + a.getLeadingBidderId());
            }
            String info = "Sản phẩm: "             + a.getItem().getName()
                    + "\nTrạng thái: "              + mapStatus(a.getStatus())
                    + "\nGiá cuối: "                + formatPrice(a.getCurrentPrice())
                    + "\nSố lượt đặt giá: "         + bidCount
                    + winnerInfo
                    + "\nKết thúc lúc: "            + a.getEndTime().format(FMT);
            showAlert(Alert.AlertType.INFORMATION,
                    "Kết quả phiên #" + a.getAuctionId(), info);
        }
    }

    private void getBalanceFromServer() {
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_MY_INFO));
                Platform.runLater(() -> {
                    if (handleBanResponse(res)) return;
                    if (res.getData() != null) {
                        try {
                            User u = res.getDataAs(User.class);
                            SessionManager.setUser(u);
                            refreshBalance();
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "Lỗi kết nối: " + ex.getMessage());
                    err.showAndWait();
                });
            }
        }, "update-balance").start();
    }

    // =========================================================================
    //  Style helpers
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
        if (a.getStatus() == AuctionStatus.OPEN) {
            return "Bắt đầu lúc " + a.getStartTime().format(FMT);
        }
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
            case RUNNING  -> "Vào phòng đấu giá →";
            case OPEN     -> "Xem chi tiết";
            case FINISHED -> {
                if (a.getLeadingBidderId() == SessionManager.getUserId()) {
                    if (LocalDateTime.now().isBefore(a.getEndTime().plusHours(24))) {
                        yield "Thanh toán";
                    }
                }
                yield "Xem kết quả";
            }
            default -> "Xem kết quả";
        };
    }

    private void setBuyerMode() {
        modeBuyerButton.setStyle("-fx-background-color:white; -fx-text-fill:#065f3b;"
                + "-fx-font-weight:bold; -fx-font-size:12; -fx-background-radius:16;"
                + "-fx-padding:4 12; -fx-cursor:hand;");
        modeSellerButton.setStyle("-fx-background-color:transparent;"
                + "-fx-text-fill:rgba(255,255,255,0.8); -fx-font-size:12;"
                + "-fx-background-radius:16; -fx-padding:4 12; -fx-cursor:hand;");
    }

    private void setSellerMode() {
        if (!SessionManager.hasRole(Role.SELLER) && !SessionManager.hasRole(Role.ADMIN)) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                    "Tài khoản của bạn chưa có quyền người bán.");
            return;
        }
        getBalanceFromServer();
        navigateTo("/com/code/views/SellerDashboard.fxml");
    }

    private void setActiveFilter(Button activeBtn) {
        String inactive = "-fx-background-color:#065f3b; -fx-text-fill:#9fe6c8;"
                + "-fx-background-radius:8; -fx-padding:5 12;";
        String active   = "-fx-background-color:#16a34a; -fx-text-fill:white;"
                + "-fx-background-radius:8; -fx-padding:5 12; -fx-font-weight:bold;";
        filterAllButton          .setStyle(inactive);
        filterActiveButton       .setStyle(inactive);
        filterUpcomingButton     .setStyle(inactive);
        filterEndedButton        .setStyle(inactive);
        filterPendingPaymentButton.setStyle(inactive);
        activeBtn.setStyle(active);
    }
}