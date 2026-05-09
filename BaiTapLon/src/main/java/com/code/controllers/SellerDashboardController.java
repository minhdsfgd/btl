package com.code.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

    // Header + stats
    @FXML private Label lblUsernameNav;
    @FXML private Label lblTotalLots;
    @FXML private Label lblActiveLots;
    @FXML private Label lblSoldLots;

    // Table
    @FXML private TextField txtSearch;
    @FXML private TableView<LotRow> tableLots;
    @FXML private TableColumn<LotRow, String> colId;
    @FXML private TableColumn<LotRow, String> colProject;
    @FXML private TableColumn<LotRow, String> colPrice;
    @FXML private TableColumn<LotRow, String> colStatus;

    // Right panel
    @FXML private VBox vboxActivities;

    // Menu buttons
    @FXML private Button btnMenuOverview;
    @FXML private Button btnMenuLots;
    @FXML private Button btnMenuSessions;
    @FXML private Button btnMenuNotif;

    // Data
    private final ObservableList<LotRow> allLots = FXCollections.observableArrayList();
    private final ObservableList<String> activities = FXCollections.observableArrayList();
    private final ObservableList<String> notificationHistory = FXCollections.observableArrayList();

    // Menu style
    private static final String MENU_ACTIVE =
            "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6;";
    private static final String MENU_INACTIVE =
            "-fx-background-color:#e0e0e0; -fx-text-fill:#1f2937; -fx-background-radius:6;";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUsernameNav.setText(LoginController.getCurrentUsername());

        configureTable();
        refreshStats();
        renderActivities();
        setActiveMenu(btnMenuOverview);
    }

    // =============================
    // Public APIs for external data
    // =============================

    /**
     * Gọi từ màn hình khác / service khi đã có dữ liệu lô hàng (từ DB).
     */
    public void setLots(List<LotRow> lots) {
        allLots.clear();
        if (lots != null) {
            allLots.addAll(lots);
        }
        tableLots.setItems(allLots);
        refreshStats();
    }

    /**
     * Gọi từ service để truyền hoạt động gần đây.
     */
    public void setActivities(List<String> activityList) {
        activities.clear();
        if (activityList != null) {
            activities.addAll(activityList);
        }
        renderActivities();
    }

    /**
     * Gọi từ service để truyền lịch sử thông báo.
     */
    public void setNotificationHistory(List<String> notifications) {
        notificationHistory.clear();
        if (notifications != null) {
            notificationHistory.addAll(notifications);
        }
    }

    /**
     * Thêm 1 thông báo mới runtime (ví dụ vừa có bid mới).
     */
    public void addNotification(String message) {
        if (message == null || message.isBlank()) return;
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        notificationHistory.add(0, timestamp + " - " + message);

        // Giữ tối đa 100 thông báo gần nhất
        if (notificationHistory.size() > 100) {
            notificationHistory.remove(notificationHistory.size() - 1);
        }
    }

    // ============
    // UI Handlers
    // ============

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        if (keyword.isBlank()) {
            tableLots.setItems(allLots);
            return;
        }

        ObservableList<LotRow> filtered = allLots.filtered(l ->
                l.getId().toLowerCase().contains(keyword)
                        || l.getProject().toLowerCase().contains(keyword)
                        || l.getStatus().toLowerCase().contains(keyword)
        );
        tableLots.setItems(filtered);
    }

    @FXML
    private void handleMenuOverview() {
        setActiveMenu(btnMenuOverview);
    }

    @FXML
    private void handleMenuLots() {
        setActiveMenu(btnMenuLots);
    }

    @FXML
    private void handleMenuSessions() {
        setActiveMenu(btnMenuSessions);
        navigateTo("/com/code/views/AuctionList.fxml");
    }

    @FXML
    private void handleMenuNotif() {
        setActiveMenu(btnMenuNotif);
        showNotificationHistoryDialog();
    }

    @FXML
    private void handleAddLot() {
        new Alert(Alert.AlertType.INFORMATION, "Chức năng thêm lô sẽ tích hợp với DB sau.").showAndWait();
    }

    @FXML
    private void handleLogout() {
        LoginController.clearSession();
        navigateTo("/com/code/views/Login.fxml");
    }

    // ================
    // Private helpers
    // ================

    private void configureTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableLots.setItems(allLots);
    }

    private void refreshStats() {
        long total = allLots.size();
        long active = allLots.stream().filter(l -> "Đang đấu giá".equalsIgnoreCase(l.getStatus())).count();
        long sold = allLots.stream().filter(l -> "Đã bán".equalsIgnoreCase(l.getStatus())).count();

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
        if (notificationHistory.isEmpty()) {
            listView.setItems(FXCollections.observableArrayList("Chưa có thông báo."));
        } else {
            listView.setItems(notificationHistory);
        }

        BorderPane root = new BorderPane(listView);
        root.setPadding(new Insets(10));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Lịch sử thông báo");
        dialog.setScene(new Scene(root, 540, 360));
        dialog.showAndWait();
    }

    private void setActiveMenu(Button activeButton) {
        List<Button> buttons = List.of(btnMenuOverview, btnMenuLots, btnMenuSessions, btnMenuNotif);
        for (Button b : buttons) {
            b.setStyle(b == activeButton ? MENU_ACTIVE : MENU_INACTIVE);
        }
    }

    // ============
    // Row Model
    // ============

    public static class LotRow {
        private final String id;
        private final String project;
        private final String price;
        private final String status;

        public LotRow(String id, String project, String price, String status) {
            this.id = id;
            this.project = project;
            this.price = price;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getProject() {
            return project;
        }

        public String getPrice() {
            return price;
        }

        public String getStatus() {
            return status;
        }
    }
}