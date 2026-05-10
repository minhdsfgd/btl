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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.code.util.ControllerUtils.navigateTo;

public class AuctionListController {

    @FXML private Label     usernameLabel;
    @FXML private Button    modeBuyerButton;
    @FXML private Button    modeSellerButton;
    @FXML private Button    logoutButton;
    @FXML private TextField searchField;
    @FXML private Button    searchButton;
    @FXML private Button    filterAllButton;
    @FXML private Button    filterActiveButton;
    @FXML private Button    filterUpcomingButton;
    @FXML private Button    filterEndedButton;
    @FXML private FlowPane  auctionListContainer;

    private String currentFilter = "ALL";
    private final List<Auction> allAuctions = new ArrayList<>();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");

    @FXML
    public void initialize() {
        usernameLabel.setText(SessionManager.getUsername());

        logoutButton.setOnAction(e -> {
            sendLogout();
            SessionManager.clear();
            navigateTo("/com/code/views/Login.fxml");
        });

        modeBuyerButton.setOnAction(e -> setBuyerMode());
        modeSellerButton.setOnAction(e -> setSellerMode());

        searchButton.setOnAction(e -> handleSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleSearch();
        });

        filterAllButton.setOnAction(e -> handleFilter("ALL",      filterAllButton));
        filterActiveButton.setOnAction(e -> handleFilter("ACTIVE",   filterActiveButton));
        filterUpcomingButton.setOnAction(e -> handleFilter("UPCOMING", filterUpcomingButton));
        filterEndedButton.setOnAction(e -> handleFilter("ENDED",    filterEndedButton));

        setActiveFilter(filterAllButton);
        setBuyerMode();

        loadAuctionsFromServer();
    }

    // ── Load từ server ────────────────────────────────────────────────────────

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
                        handleSearch(); // render với filter hiện tại
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

    private void sendLogout() {
        try {
            SocketClient.getInstance().sendRequest(Request.of(RequestType.LOGOUT));
        } catch (Exception ignored) {}
    }

    // ── Search / Filter ───────────────────────────────────────────────────────

    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        List<Auction> result = new ArrayList<>();

        for (Auction a : allAuctions) {
            boolean matchKeyword = keyword.isEmpty()
                    || a.getItem().getName().toLowerCase().contains(keyword);
            boolean matchFilter  = matchesFilter(a);
            if (matchKeyword && matchFilter) result.add(a);
        }
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
            default         -> true;
        };
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void renderList(List<Auction> items) {
        auctionListContainer.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("Không có phiên đấu giá nào.");
            empty.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:13;");
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

    private HBox buildCard(Auction a) {
        String statusStr = mapStatus(a.getStatus());
        String timeInfo  = formatTimeInfo(a);
        String priceStr  = String.format("%,.0f đ", a.getCurrentPrice());

        Label nameLabel     = new Label(a.getItem().getName());
        nameLabel.setStyle("-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:bold;");

        Label typeLabel     = new Label(a.getItem().getType().name());
        typeLabel.setStyle("-fx-text-fill:#9fe6c8;");

        Label priceLabel    = new Label("Giá hiện tại: " + priceStr);
        priceLabel.setStyle("-fx-text-fill:#22c55e;");

        Label timeLabel     = new Label("⏱ " + timeInfo);
        timeLabel.setStyle("-fx-text-fill:#9fe6c8;");

        VBox infoBox = new VBox(5, nameLabel, typeLabel, priceLabel, timeLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label statusBadge   = new Label(statusStr);
        statusBadge.setStyle(getStatusStyle(a.getStatus()));

        Button actionBtn    = new Button(getActionText(a.getStatus()));
        actionBtn.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white;"
                + " -fx-background-radius:8; -fx-padding:5 12;");
        actionBtn.setDisable(a.getStatus() != AuctionStatus.RUNNING
                && a.getStatus() != AuctionStatus.OPEN);
        actionBtn.setOnAction(e -> handleAction(a));

        VBox actionBox = new VBox(10, statusBadge, actionBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(infoBox, actionBox);
        card.setStyle("-fx-background-color:#065f3b; -fx-padding:15;"
                + " -fx-background-radius:10;");
        return card;
    }

    private void handleAction(Auction a) {
        if (a.getStatus() == AuctionStatus.RUNNING) {
            long remaining = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(), a.getEndTime());
            LiveBiddingController.prepareSession(
                    a.getAuctionId(),
                    SessionManager.getUsername(),
                    "Phiên #" + a.getAuctionId(),
                    a.getItem().getName(),
                    a.getItem().getDescription(),
                    a.getCurrentPrice() - a.getBidIncrement(),
                    a.getCurrentPrice(),
                    a.getBidIncrement(),
                    Math.max(remaining, 0)
            );
            navigateTo("/com/code/views/LiveBidding.fxml");
        }
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private String mapStatus(AuctionStatus s) {
        return switch (s) {
            case RUNNING  -> "Đang diễn ra";
            case OPEN     -> "Sắp mở";
            case FINISHED -> "Kết thúc";
            case PAID     -> "Đã thanh toán";
            case CANCELED -> "Đã hủy";
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

    private String getStatusStyle(AuctionStatus s) {
        return switch (s) {
            case RUNNING  -> "-fx-background-color:#d1fae5; -fx-text-fill:#065f3b;"
                    + " -fx-padding:4 10; -fx-background-radius:20;";
            case OPEN     -> "-fx-background-color:#fef9c3; -fx-text-fill:#854d0e;"
                    + " -fx-padding:4 10; -fx-background-radius:20;";
            default       -> "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b;"
                    + " -fx-padding:4 10; -fx-background-radius:20;";
        };
    }

    private String getActionText(AuctionStatus s) {
        return switch (s) {
            case RUNNING  -> "Xem & Đặt giá";
            case OPEN     -> "Xem chi tiết";
            default       -> "Xem kết quả";
        };
    }

    private void setBuyerMode() {
        modeBuyerButton.setStyle("-fx-background-color:white;-fx-text-fill:#065f3b;"
                + "-fx-font-weight:bold;-fx-font-size:12;-fx-background-radius:16;"
                + "-fx-padding:4 12;-fx-cursor:hand;");
        modeSellerButton.setStyle("-fx-background-color:transparent;"
                + "-fx-text-fill:rgba(255,255,255,0.75);-fx-font-size:12;"
                + "-fx-background-radius:16;-fx-padding:4 12;-fx-cursor:hand;");
    }

    private void setSellerMode() {
        if (!SessionManager.hasRole(Role.SELLER) && !SessionManager.hasRole(Role.ADMIN)) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Tài khoản của bạn chưa có quyền người bán.").showAndWait();
            return;
        }
        navigateTo("/com/code/views/SellerDashboard.fxml");
    }

    private void setActiveFilter(Button activeBtn) {
        String inactive = "-fx-background-color:#065f3b; -fx-text-fill:#9fe6c8;"
                + " -fx-background-radius:8; -fx-padding:5 12;";
        String active   = "-fx-background-color:#16a34a; -fx-text-fill:white;"
                + " -fx-background-radius:8; -fx-padding:5 12; -fx-font-weight:bold;";
        filterAllButton.setStyle(inactive);
        filterActiveButton.setStyle(inactive);
        filterUpcomingButton.setStyle(inactive);
        filterEndedButton.setStyle(inactive);
        activeBtn.setStyle(active);
    }
}