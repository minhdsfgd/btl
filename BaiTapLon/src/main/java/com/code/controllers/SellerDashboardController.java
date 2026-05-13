package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import com.code.util.ControllerUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import static com.code.util.ControllerUtils.navigateTo;
import static com.code.util.ControllerUtils.showAlert;

public class SellerDashboardController implements Initializable {

    @FXML private Label lblUsernameNav;
    @FXML private Label lblBalanceNav;
    @FXML private Label lblTotalLots;
    @FXML private Label lblActiveLots;
    @FXML private Label lblSoldLots;

    @FXML private TextField         txtSearch;
    @FXML private TableView<LotRow> tableLots;
    @FXML private TableColumn<LotRow, String> colId;
    @FXML private TableColumn<LotRow, String> colProject;
    @FXML private TableColumn<LotRow, String> colPrice;
    @FXML private TableColumn<LotRow, String> colStatus;
    @FXML private TableColumn<LotRow, String> colTimeInfo;
    @FXML private TableColumn<LotRow, Void>   colAction;

    @FXML private VBox vboxActivities;

    @FXML private Button btnMenuOverview;
    @FXML private Button btnMenuLots;
    @FXML private Button btnMenuNotif;
    @FXML private Button btnMenuActivity;
    private final ObservableList<LotRow>  allLots              = FXCollections.observableArrayList();
    private final ObservableList<String>  activities           = FXCollections.observableArrayList();
    private final ObservableList<String>  notificationHistory  = FXCollections.observableArrayList();

    private static final String MENU_ACTIVE   =
            "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6;";
    private static final String MENU_INACTIVE =
            "-fx-background-color:#e0e0e0; -fx-text-fill:#1f2937; -fx-background-radius:6;";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUsernameNav.setText(SessionManager.getUsername());
        if (SessionManager.getUser() != null) {
            long bal = (long) SessionManager.getUser().getBalance();
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            lblBalanceNav.setText(nf.format(bal) + " ₫");
        }
        configureTable();
        setActiveMenu(btnMenuOverview);
        loadMyAuctions();
    }

    // ── Load từ server ────────────────────────────────────────────────────────

    private void loadMyAuctions() {
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_MY_AUCTIONS));

                Platform.runLater(() -> {
                    allLots.clear();
                    activities.clear();

                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Auction> list = res.getDataAs(List.class);
                        if (list != null) {
                            for (Auction a : list) {
                                allLots.add(auctionToLotRow(a));
                                activities.add(0, "Phiên #" + a.getAuctionId()
                                        + " — " + a.getItem().getName()
                                        + " — " + mapStatus(a));
                            }
                        }
                    } else {
                        activities.add("Không tải được dữ liệu: " + res.getMessage());
                    }

                    tableLots.setItems(allLots);
                    refreshStats();
                    renderActivities();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    activities.add("Lỗi kết nối: " + ex.getMessage());
                    renderActivities();
                });
            }
        }, "load-seller-auctions").start();
    }

     private LotRow auctionToLotRow(Auction a) {
         String timeInfo = formatAuctionTime(a);
         return new LotRow(
                 "#" + a.getAuctionId(),
                 a.getItem().getName(),
                 String.format("%,.0f đ", a.getCurrentPrice()),
                 mapStatus(a),
                 timeInfo
         );
     }

     private String formatAuctionTime(Auction a) {
         java.time.LocalDateTime now = java.time.LocalDateTime.now();
         if (a.getStatus() == com.code.models.AuctionStatus.RUNNING) {
             long mins = java.time.temporal.ChronoUnit.MINUTES.between(now, a.getEndTime());
             return mins > 0 ? mins + " phút còn lại" : "Sắp kết thúc";
         }
         if (a.getStatus() == com.code.models.AuctionStatus.OPEN) {
             long mins = java.time.temporal.ChronoUnit.MINUTES.between(now, a.getStartTime());
             if (mins <= 0) return "Bắt đầu ngay";
             return "Bắt đầu trong " + mins + " phút";
         }
         return "Kết thúc";
     }

    private String mapStatus(Auction a) {
        return switch (a.getStatus()) {
            case RUNNING  -> "Đang đấu giá";
            case OPEN     -> "Sắp mở";
            case FINISHED -> "Đã kết thúc";
            case PAID     -> "Đã bán";
            case CANCELED -> "Đã hủy";
        };
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        if (keyword.isBlank()) { tableLots.setItems(allLots); return; }
        tableLots.setItems(allLots.filtered(l ->
                l.getId().toLowerCase().contains(keyword)
                        || l.getProject().toLowerCase().contains(keyword)
                        || l.getStatus().toLowerCase().contains(keyword)));
    }

    @FXML private void handleMenuOverview()  { setActiveMenu(btnMenuOverview); }
    @FXML private void handleMenuLots()      { setActiveMenu(btnMenuLots); }
    @FXML private void handleMenuNotif()     {
        setActiveMenu(btnMenuNotif);
        showNotificationHistoryDialog();
    }

    private void showNotificationHistoryDialog() {
    }

    @FXML
    private void handleAddLot() {
        showCreateAuctionDialog();
    }

    @FXML
    private void handleCreateItem() {
        showCreateItemDialog();
    }

    private void handleCancelLot(LotRow row) {
        // Parse auction ID from "#123" format
        String idStr = row.getId().replace("#", "").trim();
        int auctionId;
        try { auctionId = Integer.parseInt(idStr); }
        catch (NumberFormatException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Không xác định được ID phiên.");
            a.showAndWait(); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xác nhận hủy phiên \"" + row.getProject() + "\" (#" + auctionId + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hủy phiên đấu giá");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                final int id = auctionId;
                new Thread(() -> {
                    try {
                        Response res = SocketClient.getInstance()
                                .sendRequest(Request.of(RequestType.CANCEL_AUCTION, id));
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Đã hủy phiên thành công!");
                                ok.showAndWait();
                                loadMyAuctions();
                            } else {
                                Alert err = new Alert(Alert.AlertType.ERROR, "Lỗi: " + res.getMessage());
                                err.showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            Alert err = new Alert(Alert.AlertType.ERROR, "Lỗi kết nối: " + ex.getMessage());
                            err.showAndWait();
                        });
                    }
                }, "cancel-lot").start();
            }
        });
    }

    @FXML
    private void handleSwitchToBuyer() {
        com.code.util.ControllerUtils.navigateTo("/com/code/views/AuctionList.fxml");
    }

    @FXML
    private void handleLogout() {
        try { SocketClient.getInstance().sendAsync(Request.of(RequestType.LOGOUT)); }
        catch (Exception ignored) {}
        SessionManager.clear();
        navigateTo("/com/code/views/Login.fxml");
    }

    // ── Public APIs ───────────────────────────────────────────────────────────

    public void setLots(List<LotRow> lots) {
        allLots.clear();
        if (lots != null) allLots.addAll(lots);
        tableLots.setItems(allLots);
        refreshStats();
    }

    public void addNotification(String message) {
        if (message == null || message.isBlank()) return;
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        notificationHistory.add(0, ts + " - " + message);
        if (notificationHistory.size() > 100)
            notificationHistory.remove(notificationHistory.size() - 1);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void configureTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTimeInfo.setCellValueFactory(new PropertyValueFactory<>("timeInfo"));

        // Cột hành động: nút Hủy (nếu phiên OPEN/RUNNING)
        colAction.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final Button btnCancel = new Button("Hủy phiên");
            {
                btnCancel.setStyle("-fx-background-color:#c62828; -fx-text-fill:white;"
                        + "-fx-font-size:10; -fx-background-radius:4; -fx-padding:3 6;");
                btnCancel.setOnAction(e -> {
                    LotRow row = getTableView().getItems().get(getIndex());
                    handleCancelLot(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                LotRow row = getTableView().getItems().get(getIndex());
                boolean canCancel = "Đang đấu giá".equals(row.getStatus())
                        || "Sắp mở".equals(row.getStatus());
                setGraphic(canCancel ? btnCancel : null);
            }
        });

        tableLots.setItems(allLots);
        tableLots.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
    }

     private void refreshStats() {
         long total  = allLots.size();
         long active = allLots.stream().filter(l -> "Đang đấu giá".equals(l.getStatus())).count();
         long sold   = allLots.stream().filter(l -> "Đã bán".equals(l.getStatus())).count();
         lblTotalLots.setText(String.valueOf(total));
         lblActiveLots.setText(String.valueOf(active));
         lblSoldLots.setText(String.valueOf(sold));
     }

     private void setActiveMenu(Button activeBtn) {
         for (Button btn : new Button[] { btnMenuOverview, btnMenuLots, btnMenuNotif, btnMenuActivity }) {
             btn.setStyle(btn == activeBtn ? MENU_ACTIVE : MENU_INACTIVE);
         }
     }

    private void renderActivities() {
        vboxActivities.getChildren().clear();
        if (activities.isEmpty()) {
            Label empty = new Label("Chưa có hoạt động.");
            empty.setWrapText(true);
            vboxActivities.getChildren().add(empty);
            return;
        }
        for (String act : activities) {
            Label label = new Label("• " + act);
            label.setWrapText(true);
            vboxActivities.getChildren().add(label);
        }
    }
    
    @FXML
    private void handleShowActivities() {
        setActiveMenu(btnMenuActivity);

        ListView<String> listView = new ListView<>();
        listView.setItems(activities.isEmpty()
                ? FXCollections.observableArrayList("Chưa có hoạt động nào.")
                : activities);
        listView.setPrefHeight(300);

        BorderPane root = new BorderPane(listView);
        root.setPadding(new Insets(10));

        // Tiêu đề
        Label title = new Label("🕐 Hoạt động gần đây");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 0 0 8 0;");
        root.setTop(title);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Hoạt động gần đây");
        dialog.setScene(new Scene(root, 500, 360));
        dialog.showAndWait();
    }
    

    private void showCreateAuctionDialog() {
        // Load seller's items
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_MY_ITEMS));

                Platform.runLater(() -> {
                    if (!res.isSuccess()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Lỗi tải sản phẩm: " + res.getMessage());
                        alert.showAndWait();
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    java.util.List<com.code.models.Item> items = res.getDataAs(java.util.List.class);

                    if (items == null || items.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                "Bạn chưa có sản phẩm nào. Vui lòng tạo sản phẩm trước.");
                        alert.showAndWait();
                        return;
                    }

                    showCreateAuctionForm(items);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Lỗi kết nối: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }, "load-seller-items").start();
    }

    private void showCreateAuctionForm(java.util.List<com.code.models.Item> items) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        // Item selection
        ComboBox<String> cmbItem = new ComboBox<>();
        java.util.Map<String, Integer> itemMap = new java.util.HashMap<>();
        for (com.code.models.Item item : items) {
            String display = "#" + item.getItemId() + " - " + item.getName() +
                           " (" + String.format("%,.0f đ", item.getStartingPrice()) + ")";
            cmbItem.getItems().add(display);
            itemMap.put(display, item.getItemId());
        }
        cmbItem.getSelectionModel().selectFirst();

        // Bid increment
        TextField tfBidIncrement = new TextField();
        tfBidIncrement.setPromptText("Bậc tăng giá (VNĐ)");

        // Start time
        DatePicker dpStartDate = new DatePicker(java.time.LocalDate.now());
        Spinner<Integer> spStartHour = new Spinner<>(0, 23, 12);
        Spinner<Integer> spStartMin = new Spinner<>(0, 59, 0);

        // End time
        DatePicker dpEndDate = new DatePicker(java.time.LocalDate.now().plusDays(1));
        Spinner<Integer> spEndHour = new Spinner<>(0, 23, 12);
        Spinner<Integer> spEndMin = new Spinner<>(0, 59, 0);

        // Layout
        vbox.getChildren().addAll(
                new Label("Chọn sản phẩm:"), cmbItem,
                new Label("Bậc tăng giá (VNĐ):"), tfBidIncrement,
                new Label("Thời gian bắt đầu:"),
                new HBox(5, dpStartDate, new Label("Giờ:"), spStartHour,
                        new Label("Phút:"), spStartMin),
                new Label("Thời gian kết thúc:"),
                new HBox(5, dpEndDate, new Label("Giờ:"), spEndHour,
                        new Label("Phút:"), spEndMin)
        );

        Scene scene = new Scene(vbox, 600, 450);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Tạo phiên đấu giá");
        dialog.setScene(scene);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button btnCreate = new Button("Tạo phiên");
        Button btnCancel = new Button("Huỷ");

        btnCancel.setOnAction(e -> dialog.close());
        btnCreate.setOnAction(e -> {
            createAuctionSession(itemMap, cmbItem.getValue(), tfBidIncrement.getText(),
                    dpStartDate.getValue(), spStartHour.getValue(), spStartMin.getValue(),
                    dpEndDate.getValue(), spEndHour.getValue(), spEndMin.getValue(), dialog);
        });

        btnBox.getChildren().addAll(btnCreate, btnCancel);
        ((VBox) vbox).getChildren().add(btnBox);

        dialog.showAndWait();
    }

    private void createAuctionSession(java.util.Map<String, Integer> itemMap, String selectedItem,
                                     String bidIncrementStr, java.time.LocalDate startDate,
                                     int startHour, int startMin, java.time.LocalDate endDate,
                                     int endHour, int endMin, Stage dialog) {
        try {
            if (selectedItem == null || selectedItem.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn sản phẩm");
                alert.showAndWait();
                return;
            }

            double bidIncrement;
            try {
                bidIncrement = Double.parseDouble(bidIncrementStr);
                if (bidIncrement <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Bậc tăng giá phải > 0");
                alert.showAndWait();
                return;
            }

            java.time.LocalDateTime startTime = java.time.LocalDateTime.of(
                    startDate, java.time.LocalTime.of(startHour, startMin));
            java.time.LocalDateTime endTime = java.time.LocalDateTime.of(
                    endDate, java.time.LocalTime.of(endHour, endMin));

            // Validate end time after start time
            if (!endTime.isAfter(startTime)) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Thời gian kết thúc phải sau thời gian bắt đầu");
                alert.showAndWait();
                return;
            }

            // Validate start time is not in the past
            if (startTime.isBefore(java.time.LocalDateTime.now())) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Thời gian bắt đầu phải là thời điểm trong tương lai");
                alert.showAndWait();
                return;
            }

            int itemId = itemMap.get(selectedItem);
            com.code.network.CreateAuctionData data = new com.code.network.CreateAuctionData(
                    itemId, bidIncrement, startTime, endTime);

            new Thread(() -> {
                try {
                    Response res = SocketClient.getInstance()
                            .sendRequest(Request.of(RequestType.CREATE_AUCTION, data));

                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION,"Thành công",
                                    "Tạo phiên đấu giá thành công");
                            dialog.close();
                            loadMyAuctions();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR,
                                    "Lỗi: " + res.getMessage());
                            alert.showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Lỗi kết nối: " + ex.getMessage());
                        alert.showAndWait();
                    });
                }
            }, "create-auction").start();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void showCreateItemDialog() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        ComboBox<String> cmbType = new ComboBox<>();
        cmbType.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        cmbType.setValue("ELECTRONICS");

        TextField tfName = new TextField();
        tfName.setPromptText("Tên sản phẩm");

        javafx.scene.control.TextArea taDesc = new javafx.scene.control.TextArea();
        taDesc.setPromptText("Mô tả sản phẩm");
        taDesc.setPrefRowCount(3);

        TextField tfStartPrice = new TextField();
        tfStartPrice.setPromptText("Giá khởi điểm (VNĐ)");

        // Trường đặc thù theo loại
        Label lblExtra1 = new Label("Thương hiệu:");
        TextField tfExtra1 = new TextField();
        tfExtra1.setPromptText("VD: Samsung, Picasso, Toyota...");

        Label lblExtra2 = new Label("Bảo hành (tháng) / Chất liệu / Năm SX:");
        TextField tfExtra2 = new TextField();
        tfExtra2.setPromptText("VD: 12 (tháng), Sơn dầu, 2020");

        // Cập nhật nhãn khi đổi loại
        cmbType.setOnAction(e -> {
            switch (cmbType.getValue()) {
                case "ELECTRONICS" -> { lblExtra1.setText("Thương hiệu:"); lblExtra2.setText("Bảo hành (tháng):"); tfExtra2.setPromptText("VD: 12"); }
                case "ART"         -> { lblExtra1.setText("Tên tác giả:"); lblExtra2.setText("Chất liệu:"); tfExtra2.setPromptText("VD: Sơn dầu"); }
                case "VEHICLE"     -> { lblExtra1.setText("Biển số xe:"); lblExtra2.setText("Năm sản xuất:"); tfExtra2.setPromptText("VD: 2020"); }
            }
        });

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11;");

        vbox.getChildren().addAll(
                new Label("Loại sản phẩm:"), cmbType,
                new Label("Tên sản phẩm:"), tfName,
                new Label("Mô tả:"), taDesc,
                new Label("Giá khởi điểm (VNĐ):"), tfStartPrice,
                lblExtra1, tfExtra1,
                lblExtra2, tfExtra2,
                lblErr
        );

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button btnCreate = new Button("Tạo sản phẩm");
        Button btnCancel = new Button("Huỷ");
        btnBox.getChildren().addAll(btnCreate, btnCancel);
        vbox.getChildren().add(btnBox);

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Tạo sản phẩm mới");
        dialog.setScene(new javafx.scene.Scene(vbox, 480, 500));

        btnCancel.setOnAction(e -> dialog.close());
        btnCreate.setOnAction(e -> {
            String name     = tfName.getText().trim();
            String desc     = taDesc.getText().trim();
            String priceStr = tfStartPrice.getText().trim().replaceAll("[^\\d.]", "");
            String type     = cmbType.getValue();
            String extra1   = tfExtra1.getText().trim();
            String extra2   = tfExtra2.getText().trim();

            if (name.isEmpty()) { lblErr.setText("Vui lòng nhập tên sản phẩm."); return; }
            double startPrice;
            try {
                startPrice = Double.parseDouble(priceStr);
                if (startPrice <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                lblErr.setText("Giá khởi điểm không hợp lệ (phải > 0)."); return;
            }

            int sellerId = SessionManager.getUserId();
            com.code.models.Item item;
            try {
                item = switch (type) {
                    case "ELECTRONICS" -> {
                        int warranty = extra2.isEmpty() ? 0 : Integer.parseInt(extra2.replaceAll("[^\\d]", ""));
                        yield new com.code.models.Electronics(0, sellerId, name, desc, startPrice,
                                extra1.isEmpty() ? "Unknown" : extra1, warranty);
                    }
                    case "ART" -> new com.code.models.Art(0, sellerId, name, desc, startPrice,
                            extra1.isEmpty() ? "Khuyết danh" : extra1,
                            extra2.isEmpty() ? "" : extra2);
                    case "VEHICLE" -> {
                        int year = extra2.isEmpty() ? 0 : Integer.parseInt(extra2.replaceAll("[^\\d]", ""));
                        yield new com.code.models.Vehicle(0, sellerId, name, desc, startPrice,
                                extra1.isEmpty() ? "" : extra1, year);
                    }
                    default -> throw new IllegalStateException("Loại không hợp lệ");
                };
            } catch (NumberFormatException ex) {
                lblErr.setText("Bảo hành/Năm SX phải là số nguyên."); return;
            } catch (Exception ex) {
                lblErr.setText("Lỗi: " + ex.getMessage()); return;
            }

            final com.code.models.Item finalItem = item;
            new Thread(() -> {
                try {
                    Response res = SocketClient.getInstance()
                            .sendRequest(Request.of(RequestType.CREATE_ITEM, finalItem));
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            //dialog.close();
                            showAlert(Alert.AlertType.INFORMATION,"Thành công",
                                    "Tạo sản phẩm thành công");
                            dialog.close();
                            activities.add(0, "Tạo sản phẩm mới: " + name);
                            renderActivities();
                        } else {
                            lblErr.setText("Lỗi: " + res.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblErr.setText("Lỗi kết nối: " + ex.getMessage()));
                }
            }, "create-item").start();
        });

        dialog.showAndWait();
    }

     // ── Row Model ─────────────────────────────────────────────────────────────

     public static class LotRow {
         private final String id;
         private final String project;
         private final String price;
         private final String status;
         private final String timeInfo;

         public LotRow(String id, String project, String price, String status, String timeInfo) {
             this.id = id;
             this.project = project;
             this.price = price;
             this.status = status;
             this.timeInfo = timeInfo;
         }

         public String getId()       { return id; }
         public String getProject()  { return project; }
         public String getPrice()    { return price; }
         public String getStatus()   { return status; }
         public String getTimeInfo() { return timeInfo; }
     }
}
