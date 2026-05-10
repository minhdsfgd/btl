package com.code.controllers;

import com.code.client.SessionManager;
import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import static com.code.util.ControllerUtils.navigateTo;

public class SellerDashboardController implements Initializable {

    @FXML private Label lblUsernameNav;
    @FXML private Label lblTotalLots;
    @FXML private Label lblActiveLots;
    @FXML private Label lblSoldLots;

    @FXML private TextField         txtSearch;
    @FXML private TableView<LotRow> tableLots;
    @FXML private TableColumn<LotRow, String> colId;
    @FXML private TableColumn<LotRow, String> colProject;
    @FXML private TableColumn<LotRow, String> colPrice;
    @FXML private TableColumn<LotRow, String> colStatus;

    @FXML private VBox vboxActivities;

    @FXML private Button btnMenuOverview;
    @FXML private Button btnMenuLots;
    @FXML private Button btnMenuSessions;
    @FXML private Button btnMenuNotif;

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
        return new LotRow(
                "#" + a.getAuctionId(),
                a.getItem().getName(),
                String.format("%,.0f đ", a.getCurrentPrice()),
                mapStatus(a)
        );
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
    @FXML private void handleMenuSessions()  {
        setActiveMenu(btnMenuSessions);
        navigateTo("/com/code/views/AuctionList.fxml");
    }
    @FXML private void handleMenuNotif()     {
        setActiveMenu(btnMenuNotif);
        showNotificationHistoryDialog();
    }

    @FXML
    private void handleAddLot() {
        new Alert(Alert.AlertType.INFORMATION,
                "Chức năng thêm lô sẽ tích hợp với form tạo phiên đấu giá.").showAndWait();
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
        tableLots.setItems(allLots);
    }

    private void refreshStats() {
        long total  = allLots.size();
        long active = allLots.stream().filter(l -> "Đang đấu giá".equals(l.getStatus())).count();
        long sold   = allLots.stream().filter(l -> "Đã bán".equals(l.getStatus())).count();
        lblTotalLots.setText(String.valueOf(total));
        lblActiveLots.setText(String.valueOf(active));
        lblSoldLots.setText(String.valueOf(sold));
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

    private void showNotificationHistoryDialog() {
        ListView<String> listView = new ListView<>();
        listView.setItems(notificationHistory.isEmpty()
                ? FXCollections.observableArrayList("Chưa có thông báo.")
                : notificationHistory);
        BorderPane root = new BorderPane(listView);
        root.setPadding(new Insets(10));
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Lịch sử thông báo");
        dialog.setScene(new Scene(root, 540, 360));
        dialog.showAndWait();
    }

    private void setActiveMenu(Button activeButton) {
        List.of(btnMenuOverview, btnMenuLots, btnMenuSessions, btnMenuNotif).forEach(b ->
                b.setStyle(b == activeButton ? MENU_ACTIVE : MENU_INACTIVE));
    }

    // ── Row Model ─────────────────────────────────────────────────────────────

    public static class LotRow {
        private final String id;
        private final String project;
        private final String price;
        private final String status;

        public LotRow(String id, String project, String price, String status) {
            this.id = id; this.project = project;
            this.price = price; this.status = status;
        }

        public String getId()      { return id; }
        public String getProject() { return project; }
        public String getPrice()   { return price; }
        public String getStatus()  { return status; }
    }
}
