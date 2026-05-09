package com.code.controllers;

import com.code.models.Role;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.code.util.ControllerUtils.navigateTo;

public class AuctionListController {

    @FXML private Label usernameLabel;
    @FXML private Button modeBuyerButton;
    @FXML private Button modeSellerButton;
    @FXML private Button logoutButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button filterAllButton;
    @FXML private Button filterActiveButton;
    @FXML private Button filterUpcomingButton;
    @FXML private Button filterEndedButton;
    @FXML private FlowPane auctionListContainer;

    private String currentFilter = "ALL";
    private final List<AuctionItem> allItems = new ArrayList<>();

    @FXML
    public void initialize() {
        usernameLabel.setText(LoginController.getCurrentUsername());

        loadSampleData();
        renderList(allItems);

        logoutButton.setOnAction(e -> {
            LoginController.clearSession();
            navigateTo("/com/code/views/Login.fxml");
        });

        modeBuyerButton.setOnAction(e -> setBuyerMode());
        modeSellerButton.setOnAction(e -> setSellerMode());

        searchButton.setOnAction(e -> handleSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleSearch();
            }
        });

        filterAllButton.setOnAction(e -> handleFilter("ALL", filterAllButton));
        filterActiveButton.setOnAction(e -> handleFilter("ACTIVE", filterActiveButton));
        filterUpcomingButton.setOnAction(e -> handleFilter("UPCOMING", filterUpcomingButton));
        filterEndedButton.setOnAction(e -> handleFilter("ENDED", filterEndedButton));

        setActiveFilter(filterAllButton);
        setBuyerMode();
    }

    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        List<AuctionItem> result = new ArrayList<>();

        for (AuctionItem item : allItems) {
            boolean matchKeyword = keyword.isEmpty()
                    || item.getName().toLowerCase().contains(keyword);
            boolean matchFilter = matchesFilter(item);
            if (matchKeyword && matchFilter) {
                result.add(item);
            }
        }

        renderList(result);
    }

    private void handleFilter(String filter, Button activeBtn) {
        currentFilter = filter;
        setActiveFilter(activeBtn);
        handleSearch();
    }

    private boolean matchesFilter(AuctionItem item) {
        return switch (currentFilter) {
            case "ACTIVE" -> item.getStatus().equals("Đang diễn ra");
            case "UPCOMING" -> item.getStatus().equals("Sắp mở");
            case "ENDED" -> item.getStatus().equals("Kết thúc");
            default -> true;
        };
    }

    private void renderList(List<AuctionItem> items) {
        auctionListContainer.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("Không có phiên đấu giá nào.");
            empty.setStyle("-fx-text-fill:#9fe6c8; -fx-font-size:13;");
            auctionListContainer.getChildren().add(empty);
            return;
        }

        for (AuctionItem item : items) {
            auctionListContainer.getChildren().add(buildCard(item));
        }
    }

    private HBox buildCard(AuctionItem item) {
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:bold;");

        Label categoryLabel = new Label(item.getCategory());
        categoryLabel.setStyle("-fx-text-fill:#9fe6c8;");

        Label priceLabel = new Label("Giá hiện tại: " + item.getCurrentPrice());
        priceLabel.setStyle("-fx-text-fill:#22c55e;");

        Label timeLabel = new Label("⏱ " + item.getTimeLeft());
        timeLabel.setStyle("-fx-text-fill:#9fe6c8;");

        VBox infoBox = new VBox(5, nameLabel, categoryLabel, priceLabel, timeLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label statusBadge = new Label(item.getStatus());
        statusBadge.setStyle(getStatusStyle(item.getStatus()));

        Button actionBtn = new Button(getActionText(item.getStatus()));
        actionBtn.setStyle("-fx-background-color:#16a34a; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:5 12;");
        actionBtn.setOnAction(e -> handleAction(item));

        VBox actionBox = new VBox(10, statusBadge, actionBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(infoBox, actionBox);
        card.setStyle("-fx-background-color:#065f3b; -fx-padding:15; -fx-background-radius:10;");
        return card;
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "Đang diễn ra" ->
                    "-fx-background-color:#d1fae5; -fx-text-fill:#065f3b; -fx-padding:4 10; -fx-background-radius:20;";
            case "Sắp mở" ->
                    "-fx-background-color:#fef9c3; -fx-text-fill:#854d0e; -fx-padding:4 10; -fx-background-radius:20;";
            case "Kết thúc" ->
                    "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; -fx-padding:4 10; -fx-background-radius:20;";
            default ->
                    "-fx-background-color:#e5e7eb; -fx-text-fill:#374151; -fx-padding:4 10; -fx-background-radius:20;";
        };
    }

    private String getActionText(String status) {
        return switch (status) {
            case "Đang diễn ra" -> "Xem chi tiết";
            case "Sắp mở" -> "Chỉnh sửa";
            case "Kết thúc" -> "Xem kết quả";
            default -> "Xem";
        };
    }

    private void handleAction(AuctionItem item) {
        if ("Đang diễn ra".equals(item.getStatus())) {
            LiveBiddingController.prepareSession(
                    LoginController.getCurrentUsername(),
                    item.getName(),
                    item.getName(),
                    "Sản phẩm đang được đấu giá",
                    parseVnd(item.getCurrentPrice()) - 100_000,
                    parseVnd(item.getCurrentPrice()),
                    10_000,
                    2 * 60 * 60
            );
            navigateTo("/com/code/views/LiveBidding.fxml");
        }
    }

    private void setBuyerMode() {
        modeBuyerButton.setStyle("-fx-background-color:white;-fx-text-fill:#065f3b;-fx-font-weight:bold;-fx-font-size:12;-fx-background-radius:16;-fx-padding:4 12;-fx-cursor:hand;");
        modeSellerButton.setStyle("-fx-background-color:transparent;-fx-text-fill:rgba(255,255,255,0.75);-fx-font-size:12;-fx-background-radius:16;-fx-padding:4 12;-fx-cursor:hand;");
    }

    private void setSellerMode() {
        if (!LoginController.currentUserHasRole(Role.SELLER)
                && !LoginController.currentUserHasRole(Role.ADMIN)) {
            new Alert(Alert.AlertType.INFORMATION, "Tài khoản của bạn chưa có quyền người bán.").showAndWait();
            return;
        }
        navigateTo("/com/code/views/SellerDashboard.fxml");
    }

    private void setActiveFilter(Button activeBtn) {
        String inactive = "-fx-background-color:#065f3b; -fx-text-fill:#9fe6c8; -fx-background-radius:8; -fx-padding:5 12;";
        String active = "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:5 12; -fx-font-weight:bold;";

        filterAllButton.setStyle(inactive);
        filterActiveButton.setStyle(inactive);
        filterUpcomingButton.setStyle(inactive);
        filterEndedButton.setStyle(inactive);
        activeBtn.setStyle(active);
    }

    private void loadSampleData() {
        allItems.add(new AuctionItem("Đồng hồ Rolex cổ", "Đồng hồ", "15,000,000 đ", "2 giờ 30 phút", "Đang diễn ra"));
        allItems.add(new AuctionItem("Tranh sơn dầu", "Nghệ thuật", "8,500,000 đ", "Chưa bắt đầu", "Sắp mở"));
        allItems.add(new AuctionItem("Xe đạp địa hình", "Xe cộ", "3,200,000 đ", "Đã kết thúc", "Kết thúc"));
        allItems.add(new AuctionItem("Laptop Dell XPS 15", "Điện tử", "22,000,000 đ", "5 giờ 10 phút", "Đang diễn ra"));
        allItems.add(new AuctionItem("Túi Hermes Birkin", "Thời trang", "45,000,000 đ", "1 ngày", "Sắp mở"));
    }

    private double parseVnd(String value) {
        String raw = value.replaceAll("\\D", "");
        if (raw.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(raw);
    }

    public static class AuctionItem {
        private final String name;
        private final String category;
        private final String currentPrice;
        private final String timeLeft;
        private final String status;

        public AuctionItem(String name, String category, String currentPrice, String timeLeft, String status) {
            this.name = name;
            this.category = category;
            this.currentPrice = currentPrice;
            this.timeLeft = timeLeft;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getCurrentPrice() {
            return currentPrice;
        }

        public String getTimeLeft() {
            return timeLeft;
        }

        public String getStatus() {
            return status;
        }
    }
}