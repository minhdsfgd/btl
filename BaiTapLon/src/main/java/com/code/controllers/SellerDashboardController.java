package com.code.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.code.util.ControllerUtils.navigateTo;

public class SellerDashboardController implements Initializable {

    @FXML private Label lblUsernameNav;
    @FXML private Label lblTotalLots;
    @FXML private Label lblActiveLots;
    @FXML private Label lblSoldLots;

    @FXML private TextField txtSearch;
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

    private final ObservableList<LotRow> allLots = FXCollections.observableArrayList();

    private static final String MENU_ACTIVE =
            "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold;";
    private static final String MENU_INACTIVE =
            "-fx-background-color:#e0e0e0; -fx-text-fill:#1f2937;";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblUsernameNav.setText(LoginController.getCurrentUsername());

        configureTable();
        loadSampleLots();
        loadActivities();
        refreshStats();

        setActiveMenu(btnMenuOverview);
    }

    private void configureTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableLots.setItems(allLots);
    }

    private void loadSampleLots() {
        allLots.setAll(
                new LotRow("L001", "Đồng hồ Rolex cổ", "15,000,000 VND", "Đang đấu giá"),
                new LotRow("L002", "Laptop Dell XPS 15", "22,000,000 VND", "Đang đấu giá"),
                new LotRow("L003", "Tranh sơn dầu", "8,500,000 VND", "Đã bán"),
                new LotRow("L004", "Xe đạp địa hình", "3,200,000 VND", "Chờ mở")
        );
    }

    private void loadActivities() {
        vboxActivities.getChildren().clear();

        List<String> activities = List.of(
                "Tạo lô hàng L004",
                "Cập nhật giá khởi điểm L002",
                "Có người đặt giá mới ở L001"
        );

        for (String act : activities) {
            Label item = new Label("• " + act);
            item.setWrapText(true);
            vboxActivities.getChildren().add(item);
        }
    }

    private void refreshStats() {
        long total = allLots.size();
        long active = allLots.stream().filter(l -> "Đang đấu giá".equals(l.getStatus())).count();
        long sold = allLots.stream().filter(l -> "Đã bán".equals(l.getStatus())).count();

        lblTotalLots.setText(String.valueOf(total));
        lblActiveLots.setText(String.valueOf(active));
        lblSoldLots.setText(String.valueOf(sold));
    }

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim().toLowerCase();

        if (keyword.isBlank()) {
            tableLots.setItems(allLots);
            return;
        }

        ObservableList<LotRow> filtered = allLots.filtered(l ->
                l.getId().toLowerCase().contains(keyword) ||
                        l.getProject().toLowerCase().contains(keyword)
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
        new Alert(Alert.AlertType.INFORMATION, "Bạn chưa có thông báo mới.").showAndWait();
    }

    @FXML
    private void handleAddLot() {
        new Alert(Alert.AlertType.INFORMATION, "Chức năng thêm lô sẽ bổ sung sau.").showAndWait();
    }

    @FXML
    private void handleLogout() {
        LoginController.clearSession();
        navigateTo("/com/code/views/Login.fxml");
    }

    private void setActiveMenu(Button active) {
        List<Button> menus = List.of(btnMenuOverview, btnMenuLots, btnMenuSessions, btnMenuNotif);
        for (Button b : menus) {
            b.setStyle(b == active ? MENU_ACTIVE : MENU_INACTIVE);
        }
    }

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