package com.code.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.code.util.ControllerUtils.navigateTo;

public class AdminPanelController implements Initializable {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Label lblAdminName;

    // ── Stats ────────────────────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblActiveSessions;
    @FXML private Label lblTotalSessions;
    @FXML private Label lblRevenue;

    // ── Tab buttons ──────────────────────────────────────────────────────────
    @FXML private Button tabBtnUsers;
    @FXML private Button tabBtnSessions;

    // ── Panels ───────────────────────────────────────────────────────────────
    @FXML private VBox panelUsers;
    @FXML private VBox panelSessions;

    // ── User panel ───────────────────────────────────────────────────────────
    @FXML private TextField     tfUserSearch;
    @FXML private ComboBox<String> cmbRoleFilter;
    @FXML private TableView<UserRow>   tableUsers;
    @FXML private TableColumn<UserRow, Integer> colUserId;
    @FXML private TableColumn<UserRow, String>  colUserName;
    @FXML private TableColumn<UserRow, String>  colUserRole;
    @FXML private TableColumn<UserRow, String>  colUserStatus;
    @FXML private TableColumn<UserRow, Void>    colUserAction;
    @FXML private Label lblUserMsg;

    // ── Session panel ────────────────────────────────────────────────────────
    @FXML private TextField     tfSessionSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private TableView<SessionRow>   tableSessions;
    @FXML private TableColumn<SessionRow, Integer> colSessionId;
    @FXML private TableColumn<SessionRow, String>  colSessionProduct;
    @FXML private TableColumn<SessionRow, String>  colSessionStartPrice;
    @FXML private TableColumn<SessionRow, String>  colSessionCurPrice;
    @FXML private TableColumn<SessionRow, String>  colSessionStatus;
    @FXML private TableColumn<SessionRow, Void>    colSessionAction;
    @FXML private Label lblSessionMsg;

    // ── Internal data ────────────────────────────────────────────────────────
    private ObservableList<UserRow>    allUsers    = FXCollections.observableArrayList();
    private ObservableList<SessionRow> allSessions = FXCollections.observableArrayList();

    // ========================================================================
    //  Initializable
    // ========================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUserTable();
        setupSessionTable();
        setupFilters();
        loadMockData();
        lblAdminName.setText(LoginController.getCurrentUsername());
        showPanel(true);          // default: user panel visible
    }

    // ========================================================================
    //  Public API – call these from outside to inject data
    // ========================================================================

    /** Set the logged-in admin's display name. */
    public void setAdminName(String name) {
        lblAdminName.setText(name);
    }

    /** Populate the user table and refresh stats. */
    public void setUsers(List<UserRow> users) {
        allUsers.setAll(users);
        tableUsers.setItems(allUsers);
        refreshStats();
    }

    /** Populate the session table and refresh stats. */
    public void setSessions(List<SessionRow> sessions) {
        allSessions.setAll(sessions);
        tableSessions.setItems(allSessions);
        refreshStats();
    }

    // ========================================================================
    //  FXML handlers
    // ========================================================================

    @FXML
    private void handleLogout() {
        LoginController.clearSession();
        navigateTo("/com/code/views/Login.fxml");
    }

    @FXML
    private void switchToUsers() {
        showPanel(true);
    }

    @FXML
    private void switchToSessions() {
        showPanel(false);
    }

    @FXML
    private void handleUserSearch() {
        String keyword = tfUserSearch.getText().trim().toLowerCase();
        String role    = cmbRoleFilter.getValue();

        List<UserRow> filtered = allUsers.stream()
                .filter(u -> keyword.isEmpty()
                        || u.getName().toLowerCase().contains(keyword))
                .filter(u -> role == null || role.equals("Tất cả") || u.getRole().equals(role))
                .collect(Collectors.toList());

        tableUsers.setItems(FXCollections.observableArrayList(filtered));
        lblUserMsg.setText(filtered.isEmpty() ? "Không tìm thấy kết quả." : "");
    }

    @FXML
    private void handleSessionSearch() {
        String keyword = tfSessionSearch.getText().trim().toLowerCase();
        String status  = cmbStatusFilter.getValue();

        List<SessionRow> filtered = allSessions.stream()
                .filter(s -> keyword.isEmpty()
                        || s.getProduct().toLowerCase().contains(keyword))
                .filter(s -> status == null || status.equals("Tất cả") || s.getStatus().equals(status))
                .collect(Collectors.toList());

        tableSessions.setItems(FXCollections.observableArrayList(filtered));
        lblSessionMsg.setText(filtered.isEmpty() ? "Không tìm thấy kết quả." : "");
    }

    @FXML
    private void handleAddUser() {
        // TODO: open "Add User" dialog
        System.out.println("Thêm người dùng clicked");
    }

    @FXML
    private void handleCreateSession() {
        // TODO: open "Create Session" dialog
        System.out.println("Tạo phiên clicked");
    }

    // ========================================================================
    //  Private helpers
    // ========================================================================

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Style header + cells
        styleTable(tableUsers);

        // Action column: Edit + Delete buttons
        colUserAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = createActionBtn("Sửa",  "#388e3c");
            private final Button btnDelete = createActionBtn("Xoá",  "#c62828");
            private final HBox   box       = new HBox(4, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    UserRow row = getTableView().getItems().get(getIndex());
                    handleEditUser(row);
                });
                btnDelete.setOnAction(e -> {
                    UserRow row = getTableView().getItems().get(getIndex());
                    handleDeleteUser(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupSessionTable() {
        colSessionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSessionProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSessionStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colSessionCurPrice.setCellValueFactory(new PropertyValueFactory<>("curPrice"));
        colSessionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        styleTable(tableSessions);

        colSessionAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = createActionBtn("Sửa",    "#388e3c");
            private final Button btnDelete = createActionBtn("Xoá",    "#c62828");
            private final HBox   box       = new HBox(4, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    SessionRow row = getTableView().getItems().get(getIndex());
                    handleEditSession(row);
                });
                btnDelete.setOnAction(e -> {
                    SessionRow row = getTableView().getItems().get(getIndex());
                    handleDeleteSession(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupFilters() {
        cmbRoleFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "Admin", "Người dùng"));
        cmbRoleFilter.getSelectionModel().selectFirst();

        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "Đang mở", "Đã kết thúc", "Chờ duyệt"));
        cmbStatusFilter.getSelectionModel().selectFirst();
    }

    private void loadMockData() {
        setUsers(List.of(
                new UserRow(1, "admin", "Admin", "Hoạt động"),
                new UserRow(2, "seller", "Người dùng", "Hoạt động"),
                new UserRow(3, "user", "Người dùng", "Hoạt động")
        ));

        setSessions(List.of(
                new SessionRow(101, "Dong ho Rolex co", "12.000.000", "15.000.000", "Đang mở"),
                new SessionRow(102, "Laptop Dell XPS 15", "20.000.000", "22.000.000", "Đã kết thúc")
        ));
    }

    /** Toggle between the two panels and update tab-button styles. */
    private void showPanel(boolean usersTab) {
        panelUsers.setVisible(usersTab);
        panelUsers.setManaged(usersTab);
        panelSessions.setVisible(!usersTab);
        panelSessions.setManaged(!usersTab);

        String active   = "-fx-background-color: #16a34a; -fx-text-fill: white;"
                + " -fx-font-size: 13; -fx-font-weight: bold;"
                + " -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 7 0 7 0;";
        String inactive = "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.5);"
                + " -fx-border-color: rgba(255,255,255,0.2);"
                + " -fx-border-radius: 7; -fx-background-radius: 7;"
                + " -fx-font-size: 13; -fx-cursor: hand; -fx-padding: 7 0 7 0;";

        tabBtnUsers.setStyle(usersTab    ? active : inactive);
        tabBtnSessions.setStyle(usersTab ? inactive : active);
    }

    /** Recalculate and display summary stats. */
    private void refreshStats() {
        lblTotalUsers.setText(String.valueOf(allUsers.size()));

        long active = allSessions.stream()
                .filter(s -> "Đang mở".equals(s.getStatus())).count();
        lblActiveSessions.setText(String.valueOf(active));
        lblTotalSessions.setText(String.valueOf(allSessions.size()));

        // Revenue: sum of current prices for ended sessions (example logic)
        double revenue = allSessions.stream()
                .filter(s -> "Đã kết thúc".equals(s.getStatus()))
                .mapToDouble(s -> parsePrice(s.getCurPrice()))
                .sum();
        lblRevenue.setText(String.format("%,.0f đ", revenue));
    }

    private double parsePrice(String priceStr) {
        try {
            return Double.parseDouble(priceStr.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private <T> void styleTable(TableView<T> table) {
        table.setStyle(
                "-fx-background-color: #0f3d2a;"
                        + "-fx-control-inner-background: #0f3d2a;"
                        + "-fx-table-cell-border-color: #1e5e38;"
                        + "-fx-text-fill: white;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Button createActionBtn(String text, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + ";"
                + "-fx-text-fill: white; -fx-background-radius: 4;"
                + "-fx-font-size: 10; -fx-cursor: hand; -fx-padding: 3 7 3 7;");
        return btn;
    }

    // ========================================================================
    //  Row-level actions (implement or delegate as needed)
    // ========================================================================

    private void handleEditUser(UserRow row) {
        // TODO: open Edit User dialog pre-filled with row data
        System.out.println("Edit user: " + row.getName());
    }

    private void handleDeleteUser(UserRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoá người dùng \"" + row.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                allUsers.remove(row);
                tableUsers.setItems(allUsers);
                refreshStats();
            }
        });
    }

    private void handleEditSession(SessionRow row) {
        // TODO: open Edit Session dialog
        System.out.println("Edit session: " + row.getProduct());
    }

    private void handleDeleteSession(SessionRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoá phiên \"" + row.getProduct() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                allSessions.remove(row);
                tableSessions.setItems(allSessions);
                refreshStats();
            }
        });
    }

    // ========================================================================
    //  Inner model classes (replace with your real model / DTO if needed)
    // ========================================================================

    public static class UserRow {
        private final int    id;
        private final String name;
        private final String role;
        private final String status;

        public UserRow(int id, String name, String role, String status) {
            this.id     = id;
            this.name   = name;
            this.role   = role;
            this.status = status;
        }

        public int    getId()     { return id; }
        public String getName()   { return name; }
        public String getRole()   { return role; }
        public String getStatus() { return status; }
    }

    public static class SessionRow {
        private final int    id;
        private final String product;
        private final String startPrice;
        private final String curPrice;
        private final String status;

        public SessionRow(int id, String product, String startPrice, String curPrice, String status) {
            this.id         = id;
            this.product    = product;
            this.startPrice = startPrice;
            this.curPrice   = curPrice;
            this.status     = status;
        }

        public int    getId()         { return id; }
        public String getProduct()    { return product; }
        public String getStartPrice() { return startPrice; }
        public String getCurPrice()   { return curPrice; }
        public String getStatus()     { return status; }
    }
}