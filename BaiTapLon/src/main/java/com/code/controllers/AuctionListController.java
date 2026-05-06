package com.code.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AuctionListController {

    // ===== FXML fields =====
    @FXML private Label     usernameLabel;
    @FXML private Button    logoutButton;
    @FXML private TextField searchField;
    @FXML private Button    searchButton;
    @FXML private Button    filterAllButton;
    @FXML private Button    filterActiveButton;
    @FXML private Button    filterUpcomingButton;
    @FXML private Button    filterEndedButton;
    @FXML private VBox      auctionListContainer;

    private String currentFilter = "ALL";
    private final List<AuctionItem> allItems = new ArrayList<>();

    // ===== Khởi tạo =====
    @FXML
    public void initialize() {
        loadSampleData();
        renderList(allItems);

        // Đăng xuất
        logoutButton.setOnAction(e -> handleLogout());

        // Tìm kiếm
        searchButton.setOnAction(e -> handleSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleSearch();
        });

        // Filter
        filterAllButton.setOnAction(e      -> handleFilter("ALL",      filterAllButton));
        filterActiveButton.setOnAction(e   -> handleFilter("ACTIVE",   filterActiveButton));
        filterUpcomingButton.setOnAction(e -> handleFilter("UPCOMING", filterUpcomingButton));
        filterEndedButton.setOnAction(e    -> handleFilter("ENDED",    filterEndedButton));

        // Mặc định active "Tất cả"
        setActiveFilter(filterAllButton);
    }

    // ===== Nhận username từ màn hình trước =====
    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    // ===== Tìm kiếm =====
    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        List<AuctionItem> result = new ArrayList<>();

        for (AuctionItem item : allItems) {
            boolean matchKeyword = keyword.isEmpty()
                    || item.getName().toLowerCase().contains(keyword);
            boolean matchFilter = matchesFilter(item);
            if (matchKeyword && matchFilter) result.add(item);
        }

        renderList(result);
    }

    // ===== Filter =====
    private void handleFilter(String filter, Button activeBtn) {
        currentFilter = filter;
        setActiveFilter(activeBtn);
        handleSearch();
    }

    private boolean matchesFilter(AuctionItem item) {
        return switch (currentFilter) {
            case "ACTIVE"   -> item.getStatus().equals("Đang diễn ra");
            case "UPCOMING" -> item.getStatus().equals("Sắp mở");
            case "ENDED"    -> item.getStatus().equals("Kết thúc");
            default         -> true;
        };
    }

    // ===== Render danh sách =====
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

    // ===== Tạo card =====
    private HBox buildCard(AuctionItem item) {

        // Cột trái
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

        // Cột phải
        Label statusBadge = new Label(item.getStatus());
        statusBadge.setStyle(getStatusStyle(item.getStatus()));

        Button actionBtn = new Button(getActionText(item.getStatus()));
        actionBtn.setStyle(
                "-fx-background-color:#16a34a; -fx-text-fill:white; " +
                        "-fx-background-radius:8; -fx-padding:5 12;"
        );
        actionBtn.setOnAction(e -> handleAction(item));

        VBox actionBox = new VBox(10, statusBadge, actionBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // Card
        HBox card = new HBox(infoBox, actionBox);
        card.setStyle(
                "-fx-background-color:#065f3b; -fx-padding:15; -fx-background-radius:10;"
        );

        return card;
    }

    // ===== Badge màu theo trạng thái =====
    private String getStatusStyle(String status) {
        return switch (status) {
            case "Đang diễn ra" ->
                    "-fx-background-color:#d1fae5; -fx-text-fill:#065f3b; " +
                            "-fx-padding:4 10; -fx-background-radius:20;";
            case "Sắp mở" ->
                    "-fx-background-color:#fef9c3; -fx-text-fill:#854d0e; " +
                            "-fx-padding:4 10; -fx-background-radius:20;";
            case "Kết thúc" ->
                    "-fx-background-color:#fee2e2; -fx-text-fill:#991b1b; " +
                            "-fx-padding:4 10; -fx-background-radius:20;";
            default ->
                    "-fx-background-color:#e5e7eb; -fx-text-fill:#374151; " +
                            "-fx-padding:4 10; -fx-background-radius:20;";
        };
    }

    // ===== Text nút hành động =====
    private String getActionText(String status) {
        return switch (status) {
            case "Đang diễn ra" -> "Xem chi tiết";
            case "Sắp mở"       -> "Chỉnh sửa";
            case "Kết thúc"     -> "Xem kết quả";
            default             -> "Xem";
        };
    }

    // ===== Xử lý click hành động =====
    private void handleAction(AuctionItem item) {
        System.out.println("Click: " + item.getName() + " | " + item.getStatus());
        // TODO: mở màn hình chi tiết
        // navigateTo("/com/code/views/AuctionDetail.fxml");
    }

    // ===== Đăng xuất =====
    private void handleLogout() {
        navigateTo("/com/code/views/Login.fxml");
    }

    // ===== Style filter active/inactive =====
    private void setActiveFilter(Button activeBtn) {
        String inactive = "-fx-background-color:#065f3b; -fx-text-fill:#9fe6c8; " +
                "-fx-background-radius:8; -fx-padding:5 12;";
        String active   = "-fx-background-color:#16a34a; -fx-text-fill:white; " +
                "-fx-background-radius:8; -fx-padding:5 12; -fx-font-weight:bold;";

        filterAllButton.setStyle(inactive);
        filterActiveButton.setStyle(inactive);
        filterUpcomingButton.setStyle(inactive);
        filterEndedButton.setStyle(inactive);
        activeBtn.setStyle(active);
    }

    // ===== Chuyển màn hình =====
    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Dữ liệu mẫu =====
    private void loadSampleData() {
        allItems.add(new AuctionItem("Đồng hồ Rolex cổ",  "Đồng hồ",   "15,000,000 đ", "2 giờ 30 phút", "Đang diễn ra"));
        allItems.add(new AuctionItem("Tranh sơn dầu",      "Nghệ thuật", "8,500,000 đ",  "Chưa bắt đầu",  "Sắp mở"));
        allItems.add(new AuctionItem("Xe đạp địa hình",    "Xe cộ",      "3,200,000 đ",  "Đã kết thúc",   "Kết thúc"));
        allItems.add(new AuctionItem("Laptop Dell XPS 15", "Điện tử",    "22,000,000 đ", "5 giờ 10 phút", "Đang diễn ra"));
        allItems.add(new AuctionItem("Túi Hermes Birkin",  "Thời trang", "45,000,000 đ", "1 ngày",        "Sắp mở"));
    }

    // ===== Model =====
    public static class AuctionItem {
        private final String name, category, currentPrice, timeLeft, status;

        public AuctionItem(String name, String category, String currentPrice,
                           String timeLeft, String status) {
            this.name         = name;
            this.category     = category;
            this.currentPrice = currentPrice;
            this.timeLeft     = timeLeft;
            this.status       = status;
        }

        public String getName()         { return name; }
        public String getCategory()     { return category; }
        public String getCurrentPrice() { return currentPrice; }
        public String getTimeLeft()     { return timeLeft; }
        public String getStatus()       { return status; }
    }
}