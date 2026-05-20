package com.code.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.code.models.Role;
import com.code.network.Request;
import com.code.network.Response;
import static com.code.util.ControllerUtils.navigateTo;
import com.code.network.UpdateUserData;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
        lblAdminName.setText(com.code.client.SessionManager.getUsername());
        showPanel(true);
        loadDataFromServer();
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
        new Thread(() -> {
            try {
                // QUAN TRỌNG:
                // dùng sendRequest để đọc Response từ server
                com.code.client.SocketClient.getInstance()
                        .sendRequest(com.code.network.Request.of(
                                com.code.network.RequestType.LOGOUT));

            } catch (Exception e) {
                System.err.println("Lỗi logout: " + e.getMessage());
            }

            javafx.application.Platform.runLater(() -> {
                com.code.client.SessionManager.clear();
                navigateTo("/com/code/views/Login.fxml");
            });

        }, "logout-thread").start();
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
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(15));

        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Tên đăng nhập (≥ 4 ký tự)");

        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText("Mật khẩu (≥ 6 ký tự)");

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("BIDDER", "SELLER", "ADMIN");
        cmbRole.setValue("BIDDER");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11;");

        vbox.getChildren().addAll(
                new Label("Tên đăng nhập:"), tfUsername,
                new Label("Mật khẩu:"), pfPassword,
                new Label("Vai trò:"), cmbRole,
                lblError
        );

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);
        Button btnSave   = new Button("Tạo tài khoản");
        Button btnCancel = new Button("Huỷ");
        btnBox.getChildren().addAll(btnSave, btnCancel);
        vbox.getChildren().add(btnBox);

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Thêm người dùng mới");
        dialog.setScene(new javafx.scene.Scene(vbox, 380, 320));

        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            String uname = tfUsername.getText().trim();
            String pwd   = pfPassword.getText();
            if (uname.length() < 4) { lblError.setText("Tên đăng nhập phải ≥ 4 ký tự"); return; }
            if (pwd.length() < 6)   { lblError.setText("Mật khẩu phải ≥ 6 ký tự"); return; }

            com.code.models.Role role;
            try { role = com.code.models.Role.valueOf(cmbRole.getValue()); }
            catch (Exception ex) { role = com.code.models.Role.BIDDER; }

            final com.code.models.Role finalRole = role;
            new Thread(() -> {
                try {
                    Request req = getRequest(finalRole, uname, pwd);
                    Response res = com.code.client.SocketClient.getInstance().sendRequest(req);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            dialog.close();
                            lblUserMsg.setText("Tạo tài khoản \"" + uname + "\" thành công!");
                            loadDataFromServer();
                        } else {
                            lblError.setText("Lỗi: " + res.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblError.setText("Lỗi kết nối: " + ex.getMessage()));
                }
            }, "admin-create-user").start();
        });

        dialog.showAndWait();
    }

    private static Request getRequest(Role finalRole, String uname, String pwd) {
        Request req;
        if (finalRole == Role.ADMIN) {
            req = Request.of(
                    com.code.network.RequestType.CREATE_ADMIN,
                    new com.code.network.LoginData(uname, pwd));
        } else {
            req = Request.of(
                    com.code.network.RequestType.REGISTER,
                    new com.code.network.LoginData(uname, pwd, finalRole));
        }
        return req;
    }


    // ========================================================================
    //  Private helpers
    // ========================================================================

    private void setupUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ─── CĂN GIỮA CÁC Ô DỮ LIỆU ───
        colUserId.setStyle("-fx-alignment: CENTER;");
        colUserRole.setStyle("-fx-alignment: CENTER;");
        colUserStatus.setStyle("-fx-alignment: CENTER;");
        colUserAction.setStyle("-fx-alignment: CENTER;"); // Căn giữa cột hành động
        // ───────────────────────────────

        styleTable(tableUsers);

        // Action column: Edit + Delete buttons
        colUserAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = createActionBtn("Sửa",  "#388e3c");
            private final Button btnDelete = createActionBtn("Xoá",  "#c62828");
            private final HBox   box       = new HBox(4, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER); // Căn giữa các nút bấm bên trong khung chứa
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
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }
    private void setupSessionTable() {
        colSessionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSessionProduct.setCellValueFactory(new PropertyValueFactory<>("product"));
        colSessionStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colSessionCurPrice.setCellValueFactory(new PropertyValueFactory<>("curPrice"));
        colSessionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ─── CĂN GIỮA CÁC Ô DỮ LIỆU ───
        colSessionId.setStyle("-fx-alignment: CENTER;");
        colSessionStatus.setStyle("-fx-alignment: CENTER;");
        colSessionAction.setStyle("-fx-alignment: CENTER;"); // Căn giữa cột hành động
        // ───────────────────────────────

        styleTable(tableSessions);

        colSessionAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = createActionBtn("Sửa",    "#388e3c");
            private final Button btnDelete = createActionBtn("Xoá",    "#c62828");
            private final HBox   box       = new HBox(4, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER); // Căn giữa các nút bấm bên trong khung chứa
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
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }
    private void setupFilters() {
        cmbRoleFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "ADMIN", "SELLER", "BIDDER"));
        cmbRoleFilter.getSelectionModel().selectFirst();

        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "RUNNING", "OPEN", "FINISHED", "PAID", "CANCELED"));
        cmbStatusFilter.getSelectionModel().selectFirst();
    }

    private void loadDataFromServer() {
        // Tải danh sách user
        new Thread(() -> {
            try {
                Response res = com.code.client.SocketClient.getInstance()
                        .sendRequest(com.code.network.Request.of(
                                com.code.network.RequestType.GET_ALL_USERS));
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<com.code.models.User> users =
                                res.getDataAs(List.class);
                        if (users != null) {
                            List<UserRow> rows = users.stream()
                                    .map(u -> new UserRow(
                                            u.getUserId(),
                                            u.getUsername(),
                                            u.getRoles().stream()
                                                    .map(Enum::name)
                                                    .reduce((a, b) -> a + ", " + b)
                                                    .orElse(""),
                                            u.isBanned() ? "Bị cấm" : "Hoạt động"
                                    ))
                                    .collect(java.util.stream.Collectors.toList());
                            setUsers(rows);
                        }
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi tải danh sách người dùng: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }, "load-admin-users").start();

        // Tải danh sách phiên đấu giá
        new Thread(() -> {
            try {
                Response res = com.code.client.SocketClient.getInstance()
                        .sendRequest(com.code.network.Request.of(
                                com.code.network.RequestType.GET_ALL_AUCTIONS));
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<com.code.models.Auction> auctions =
                                res.getDataAs(List.class);
                        if (auctions != null) {
                            List<SessionRow> rows = auctions.stream()
                                    .map(a -> new SessionRow(
                                            a.getAuctionId(),
                                            a.getItem().getName(),
                                            String.format("%,.0f đ",
                                                    a.getItem().getStartingPrice()),
                                            String.format("%,.0f đ",
                                                    a.getCurrentPrice()),
                                            a.getStatus().name()
                                    ))
                                    .collect(java.util.stream.Collectors.toList());
                            setSessions(rows);
                        }
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi tải danh sách phiên đấu giá: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }, "load-admin-auctions").start();
    }

    private void showPanel(boolean usersTab) {
        panelUsers.setVisible(usersTab);
        panelUsers.setManaged(usersTab);
        panelSessions.setVisible(!usersTab);
        panelSessions.setManaged(!usersTab);

        // Nút ĐANG CHỌN: Màu xanh lá tươi sáng giống nút "+ Thêm" (#16a34a)
        String active = "-fx-background-color: #16a34a; -fx-text-fill: white;"
                + " -fx-font-size: 13; -fx-font-weight: bold;"
                + " -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 7 0 7 0;";

        // Nút KHÔNG CHỌN: Màu xám sáng rõ ràng, chữ đen đậm đậm nét (Không bị mờ chìm)
        String inactive = "-fx-background-color: #e0e0e0; -fx-text-fill: #1f2937;"
                + " -fx-font-size: 13; -fx-font-weight: bold;"
                + " -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 7 0 7 0;";

        tabBtnUsers.setStyle(usersTab ? active : inactive);
        tabBtnSessions.setStyle(usersTab ? inactive : active);
    }
    /** Recalculate and display summary stats. */
    private void refreshStats() {
        lblTotalUsers.setText(String.valueOf(allUsers.size()));

        // Trạng thái thực tế được lưu là tên enum: RUNNING, OPEN, FINISHED, PAID, CANCELED
        long active = allSessions.stream()
                .filter(s -> "RUNNING".equals(s.getStatus()) || "OPEN".equals(s.getStatus())).count();
        lblActiveSessions.setText(String.valueOf(active));
        lblTotalSessions.setText(String.valueOf(allSessions.size()));

        // Doanh thu: tổng giá cuối các phiên FINISHED hoặc PAID
        double revenue = allSessions.stream()
                .filter(s -> "FINISHED".equals(s.getStatus()) || "PAID".equals(s.getStatus()))
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
        // 1. TRẢ LẠI MÀU XANH LỤC ĐẬM NỀN BẢN CHO BẢNG VÀ ÉP CHỮ MÀU ĐEN
        table.setStyle(
                "-fx-background-color: #0f3d2a;" // Màu xanh đậm ngoài cùng của bảng
                        + "-fx-control-inner-background: #0f3d2a;" // Màu nền của vùng trống bên trong bảng
                        + "-fx-table-cell-border-color: transparent;" // Ẩn các đường kẻ mặc định của dòng trống
                        + "-fx-text-background-color: #000000;" // Ép cứng màu chữ của cell có dữ liệu thành màu đen
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 2. Quản lý màu sắc động cho từng dòng (RowFactory)
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<T> row = new javafx.scene.control.TableRow<>();

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    // Dòng rỗng -> Cho suốt hoàn toàn để lộ nền xanh đậm #0f3d2a phía dưới
                    row.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                } else {
                    // Dòng có dữ liệu -> Đổ nền trắng hoàn hảo, chữ đen, kẻ viền xám mỏng bên dưới
                    row.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-text-fill: #000000;");
                }
            });

            // Hiệu ứng đổi màu nhẹ khi click chọn dòng dữ liệu
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (row.getItem() != null) {
                    if (isNowSelected) {
                        // Khi được chọn: Nền xanh ngọc nhạt, giữ chữ đen
                        row.setStyle("-fx-background-color: #c8f5e1; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-text-fill: #000000;");
                    } else {
                        // Khi bỏ chọn: Trả về nền trắng
                        row.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-text-fill: #000000;");
                    }
                }
            });

            return row;
        });
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
        showEditUserDialog(row.getId());
    }

    private void handleDeleteUser(UserRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoá người dùng \"" + row.getName() + "\" - điều này không thể hoàn tác?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        com.code.network.Response res = com.code.client.SocketClient.getInstance()
                                .sendRequest(com.code.network.Request.of(
                                        com.code.network.RequestType.DELETE_USER, row.getId()));
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                allUsers.remove(row);
                                tableUsers.setItems(allUsers);
                                refreshStats();
                                lblUserMsg.setText("Đã xoá user thành công.");
                            } else {
                                lblUserMsg.setText("Lỗi: " + res.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> lblUserMsg.setText("Lỗi kết nối: " + ex.getMessage()));
                    }
                }, "admin-delete-user").start();
            }
        });
    }

    private void handleEditSession(SessionRow row) {
        // Hiển thị thông tin phiên và cho phép hủy hoặc đánh dấu thanh toán
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(15));

        Label lblId      = new Label("ID phiên: #" + row.getId());
        Label lblProduct = new Label("Sản phẩm: " + row.getProduct());
        Label lblStart   = new Label("Giá khởi điểm: " + row.getStartPrice());
        Label lblCur     = new Label("Giá hiện tại: " + row.getCurPrice());
        Label lblStatus  = new Label("Trạng thái: " + row.getStatus());
        lblStatus.setStyle("-fx-font-weight: bold;");

        Label lblMsg = new Label();
        lblMsg.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11;");

        vbox.getChildren().addAll(lblId, lblProduct, lblStart, lblCur, lblStatus,
                new javafx.scene.control.Separator(), lblMsg);

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(8);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Quản lý phiên #" + row.getId());

        // Nút Hủy phiên (CANCEL_AUCTION)
        if ("OPEN".equals(row.getStatus()) || "RUNNING".equals(row.getStatus())) {
            Button btnCancel = createActionBtn("⛔ Hủy phiên", "#c62828");
            btnCancel.setPrefWidth(120);
            btnCancel.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Xác nhận hủy phiên đấu giá #" + row.getId() + "?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(b -> {
                    if (b == ButtonType.YES) {
                        sendSessionAction(com.code.network.RequestType.CANCEL_AUCTION,
                                row.getId(), row, dialog, lblMsg);
                    }
                });
            });
            btnBox.getChildren().add(btnCancel);
        }

        // Nút Đánh dấu đã thanh toán (MARK_AS_PAID)
        if ("FINISHED".equals(row.getStatus())) {
            Button btnPaid = createActionBtn("✅ Đánh dấu thanh toán", "#388e3c");
            btnPaid.setPrefWidth(160);
            btnPaid.setOnAction(e -> sendSessionAction(
                    com.code.network.RequestType.MARK_AS_PAID, row.getId(), row, dialog, lblMsg));
            btnBox.getChildren().add(btnPaid);
        }

        Button btnClose = new Button("Đóng");
        btnClose.setOnAction(e -> dialog.close());
        btnBox.getChildren().add(btnClose);

        vbox.getChildren().add(btnBox);
        dialog.setScene(new javafx.scene.Scene(vbox, 340, 280));
        dialog.showAndWait();
    }

    private void sendSessionAction(com.code.network.RequestType type, int sessionId,
                                   SessionRow row, javafx.stage.Stage dialog, Label lblMsg) {
        new Thread(() -> {
            try {
                Response res = com.code.client.SocketClient.getInstance()
                        .sendRequest(com.code.network.Request.of(type, sessionId));
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        dialog.close();
                        lblSessionMsg.setText("Thao tác thành công với phiên #" + sessionId);
                        loadDataFromServer();
                    } else {
                        lblMsg.setText("Lỗi: " + res.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> lblMsg.setText("Lỗi kết nối: " + ex.getMessage()));
            }
        }, "admin-session-action").start();
    }

    private void handleDeleteSession(SessionRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hủy phiên \"" + row.getProduct() + "\" (ID #" + row.getId() + ")?\n"
                + "Hành động này không thể hoàn tác.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận hủy phiên");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        Response res = com.code.client.SocketClient.getInstance()
                                .sendRequest(com.code.network.Request.of(
                                        com.code.network.RequestType.CANCEL_AUCTION, row.getId()));
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                allSessions.remove(row);
                                tableSessions.setItems(allSessions);
                                refreshStats();
                                lblSessionMsg.setText("Đã hủy phiên #" + row.getId() + " thành công.");
                            } else {
                                lblSessionMsg.setText("Lỗi: " + res.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() ->
                                lblSessionMsg.setText("Lỗi kết nối: " + ex.getMessage()));
                    }
                }, "admin-delete-session").start();
            }
        });
    }

    private void showEditUserDialog(int userId) {
        try {
            com.code.network.Response res = com.code.client.SocketClient.getInstance()
                    .sendRequest(com.code.network.Request.of(
                            com.code.network.RequestType.GET_ALL_USERS));

            if (!res.isSuccess()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Không tải được dữ liệu user");
                alert.showAndWait();
                return;
            }

            @SuppressWarnings("unchecked")
            java.util.List<com.code.models.User> users = res.getDataAs(java.util.List.class);
            com.code.models.User user = users.stream()
                    .filter(u -> u.getUserId() == userId)
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Không tìm thấy user");
                alert.showAndWait();
                return;
            }

            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
            vbox.setPadding(new javafx.geometry.Insets(15));

            TextField tfUsername = new TextField(user.getUsername());
            tfUsername.setPromptText("Tên đăng nhập");

            PasswordField pfPassword = new PasswordField();
            pfPassword.setPromptText("Mật khẩu mới (để trống = không thay đổi)");

            TextField tfBalance = new TextField(String.valueOf(user.getBalance()));
            tfBalance.setPromptText("Số dư");

            CheckBox cbActive = new CheckBox("Kích hoạt");
            cbActive.setSelected(user.isActive());

            CheckBox cbBanned = new CheckBox("This button is for decoration");
            cbBanned.setSelected(user.isBanned());

            vbox.getChildren().addAll(
                    new Label("Tên đăng nhập:"), tfUsername,
                    new Label("Mật khẩu mới:"), pfPassword,
                    new Label("Số dư:"), tfBalance,
                    cbActive, cbBanned
            );

            javafx.scene.Scene scene = new javafx.scene.Scene(vbox, 400, 350);
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("Sửa người dùng #" + userId);
            dialog.setScene(scene);

            javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(10);
            btnBox.setAlignment(javafx.geometry.Pos.CENTER);
            Button btnSave = new Button("Lưu");
            Button btnCancel = new Button("Huỷ");

            btnCancel.setOnAction(e -> dialog.close());
            btnSave.setOnAction(e -> {
                saveUserChanges(user, tfUsername.getText(), pfPassword.getText(),
                        tfBalance.getText(), cbActive.isSelected(), cbBanned.isSelected(), dialog);
            });

            btnBox.getChildren().addAll(btnSave, btnCancel);
            ((javafx.scene.layout.VBox) vbox).getChildren().add(btnBox);

            dialog.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void saveUserChanges(com.code.models.User user, String username, String password,
                                 String balanceStr, boolean active, boolean banned, javafx.stage.Stage dialog) {
        try {
            Double balance = null;
            try {
                balance = Double.parseDouble(balanceStr);
            } catch (NumberFormatException ignored) {}

            com.code.network.UpdateUserData data = new com.code.network.UpdateUserData(
                    user.getUserId(),
                    username.isEmpty() ? null : username,
                    password.isEmpty() ? null : password,
                    balance,
                    active,
                    null
            );

            com.code.network.Response res = com.code.client.SocketClient.getInstance()
                    .sendRequest(com.code.network.Request.of(
                            com.code.network.RequestType.UPDATE_USER, data));

            Platform.runLater(() -> {
                if (res.isSuccess()) {
                    dialog.close();
                    lblUserMsg.setText("Cập nhật user thành công!");
                    loadDataFromServer();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, res.getMessage());
                    alert.showAndWait();
                }
            });
        } catch (Exception ex) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi: " + ex.getMessage());
                alert.showAndWait();
            });
        }
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
