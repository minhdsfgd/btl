package com.code.controllers;

import com.code.client.SessionManager;
import com.code.controllers.seller.AuctionFormValidator;
import com.code.controllers.seller.ItemFormValidator;
import com.code.controllers.seller.SellerApiService;
import com.code.controllers.seller.SellerTableManager;
import com.code.controllers.seller.SellerTableManager.LotRow;
import com.code.models.Auction;
import com.code.models.AuctionStatus;
import com.code.models.Item;
import com.code.models.User;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.code.util.ControllerUtils.*;

public class SellerDashboardController implements Initializable {

    // ── FXML fields ───────────────────────────────────────────────────────────

    @FXML private Label    lblUsernameNav;
    @FXML private Label    lblBalanceNav;
    @FXML private Label    lblTotalLots;
    @FXML private Label    lblActiveLots;
    @FXML private Label    lblSoldLots;

    @FXML private TextField                     txtSearch;
    @FXML private TableView<LotRow>             tableLots;
    @FXML private TableColumn<LotRow, String>   colId;
    @FXML private TableColumn<LotRow, String>   colProject;
    @FXML private TableColumn<LotRow, String>   colPrice;
    @FXML private TableColumn<LotRow, String>   colStatus;
    @FXML private TableColumn<LotRow, String>   colTimeInfo;
    @FXML private TableColumn<LotRow, Void>     colAction;

    @FXML private VBox        vboxActivities;
    @FXML private VBox        panelActivities;
    @FXML private AnchorPane  rootPane;
    @FXML private ImageView   backgroundImage;

    @FXML private Button btnMenuLots;
    @FXML private Button btnMenuNotif;
    @FXML private Button btnMenuActivity;
    @FXML private Button modeBuyerButton;
    @FXML private Button modeSellerButton;

    // ── Helpers (inject qua constructor hoặc default) ─────────────────────────

    private final SellerApiService      api;
    private final AuctionFormValidator  auctionValidator;
    private final ItemFormValidator     itemValidator;

    private SellerTableManager tableManager;

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<String>  activities          = new ArrayList<>();
    private final List<String>  notificationHistory = new ArrayList<>();
    private final Set<Integer>  usedItemIds         = new HashSet<>();

    // ── Style constants ───────────────────────────────────────────────────────

    private static final String MENU_ACTIVE =
            "-fx-background-color:#16a34a; -fx-text-fill:white; -fx-font-weight:bold;" +
                    "-fx-background-radius:6; -fx-padding:5 12; -fx-cursor:hand;";
    private static final String MENU_INACTIVE =
            "-fx-background-color:#065f3b; -fx-text-fill:white;" +
                    "-fx-background-radius:6; -fx-padding:5 12; -fx-cursor:hand;";

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Production constructor — JavaFX instantiates this via FXMLLoader. */
    public SellerDashboardController() {
        this(new SellerApiService(), new AuctionFormValidator(), new ItemFormValidator());
    }

    /** Test constructor — inject mock dependencies. */
    public SellerDashboardController(SellerApiService api,
                                     AuctionFormValidator auctionValidator,
                                     ItemFormValidator itemValidator) {
        this.api              = api;
        this.auctionValidator = auctionValidator;
        this.itemValidator    = itemValidator;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bindBackgroundImage();

        lblUsernameNav.setText(SessionManager.getUsername());

        // Khởi tạo SellerTableManager sau khi FXML đã inject các cột
        tableManager = new SellerTableManager(
                tableLots, colId, colProject, colPrice, colStatus, colTimeInfo, colAction,
                this::handleCancelLot   // truyền callback hủy phiên
        );

        hideActivitiesPanel();
        setSellerToggleActive();
        refreshBalance();
        loadMyAuctions();
    }

    // ── FXML Handlers ─────────────────────────────────────────────────────────

    @FXML private void handleSearch() {
        tableManager.filter(txtSearch.getText().trim());
    }

    @FXML private void handleMenuLots()       { setActiveMenu(btnMenuLots); }

    @FXML private void handleMenuNotif() {
        setActiveMenu(btnMenuNotif);
        showNotificationHistoryDialog();
    }

    @FXML private void handleShowActivities() {
        setActiveMenu(btnMenuActivity);
        if (panelActivities == null) return;
        boolean show = !panelActivities.isVisible();
        panelActivities.setVisible(show);
        panelActivities.setManaged(show);
    }

    @FXML private void handleAddLot()        { showCreateAuctionDialog(); }
    @FXML private void handleCreateItem()    { showCreateItemDialog(); }
    @FXML private void handleSwitchToBuyer() { navigateTo("/com/code/views/AuctionList.fxml"); }

    @FXML private void handleLogout() {
        new Thread(() -> {
            api.logout();
            Platform.runLater(() -> {
                SessionManager.clear();
                navigateTo("/com/code/views/Login.fxml");
            });
        }, "logout-thread").start();
    }

    // ── Public API (dùng bởi LiveBiddingController hoặc test) ─────────────────

    public void addNotification(String message) {
        if (message == null || message.isBlank()) return;
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        notificationHistory.add(0, ts + " — " + message);
        if (notificationHistory.size() > 100)
            notificationHistory.remove(notificationHistory.size() - 1);
    }

    // ── Network + data loading ─────────────────────────────────────────────────

    private void refreshBalance() {
        new Thread(() -> {
            try {
                Response res = api.fetchMyInfo();
                if (res.isSuccess() && res.getData() instanceof User freshUser) {
                    Platform.runLater(() -> {
                        SessionManager.setUser(freshUser);
                        long bal = (long) freshUser.getBalance();
                        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
                        lblBalanceNav.setText(nf.format(bal) + " ₫");
                    });
                } else {
                    Platform.runLater(() -> handleBanResponse(res));
                }
            } catch (Exception ex) {
                System.err.println("[Seller] Không refresh được balance: " + ex.getMessage());
            }
        }, "balance-refresh").start();
    }

    private void loadMyAuctions() {
        new Thread(() -> {
            try {
                Response res = api.fetchMyAuctions();
                Platform.runLater(() -> {
                    if (handleBanResponse(res)) return;

                    activities.clear();

                    List<Auction> auctions = api.extractAuctions(res);
                    tableManager.load(auctions);

                    for (Auction a : auctions) {
                        activities.add(0, buildActivityLabel(a));
                    }

                    updateStats();
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

    // ── Hủy phiên ─────────────────────────────────────────────────────────────

    private void handleCancelLot(LotRow row) {
        int auctionId;
        try {
            auctionId = Integer.parseInt(row.getId().replace("#", "").trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xác định được ID phiên.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xác nhận hủy phiên \"" + row.getProject() + "\" (#" + auctionId + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Hủy phiên đấu giá");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            int id = auctionId;
            new Thread(() -> {
                try {
                    Response res = api.cancelAuction(id);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã hủy phiên thành công!");
                            loadMyAuctions();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", res.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", ex.getMessage()));
                }
            }, "cancel-lot").start();
        });
    }

    // ── Dialog: Tạo phiên đấu giá ─────────────────────────────────────────────

    private void showCreateAuctionDialog() {
        new Thread(() -> {
            try {
                Response resAuctions = api.fetchMyAuctions();
                Response resItems    = api.fetchMyItems();

                Platform.runLater(() -> {
                    if (!resItems.isSuccess()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi tải sản phẩm: " + resItems.getMessage());
                        return;
                    }

                    List<Item> items = api.extractItems(resItems);
                    if (items.isEmpty()) {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                                "Bạn chưa có sản phẩm. Vui lòng tạo sản phẩm trước.");
                        return;
                    }

                    // Lọc item đang được dùng trong phiên active
                    usedItemIds.clear();
                    api.extractAuctions(resAuctions).stream()
                            .filter(a -> a.getStatus() == AuctionStatus.OPEN
                                    || a.getStatus() == AuctionStatus.RUNNING
                                    || a.getStatus() == AuctionStatus.PAID)
                            .forEach(a -> usedItemIds.add(a.getItem().getItemId()));

                    List<Item> available = items.stream()
                            .filter(it -> !usedItemIds.contains(it.getItemId()))
                            .toList();

                    if (available.isEmpty()) {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo",
                                "Tất cả sản phẩm đang trong phiên đấu giá hoặc đã được bán.");
                        return;
                    }
                    buildCreateAuctionForm(available);
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", ex.getMessage()));
            }
        }, "load-items-for-auction").start();
    }

    private void buildCreateAuctionForm(List<Item> items) {
        // ── Build form fields ──────────────────────────────────────────────────
        ComboBox<String>   cmbItem         = new ComboBox<>();
        Map<String, Integer> itemMap       = new LinkedHashMap<>();
        for (Item it : items) {
            String key = "#" + it.getItemId() + " — " + it.getName()
                    + " (" + String.format("%,.0f đ", it.getStartingPrice()) + ")";
            cmbItem.getItems().add(key);
            itemMap.put(key, it.getItemId());
        }
        cmbItem.getSelectionModel().selectFirst();

        TextField        tfBidIncrement = new TextField();
        tfBidIncrement.setPromptText("Bậc tăng giá (VNĐ)");

        DatePicker       dpStart        = new DatePicker(LocalDate.now());
        Spinner<Integer> spStartH       = intSpinner(0, 23, 12);
        Spinner<Integer> spStartM       = intSpinner(0, 59, 0);

        DatePicker       dpEnd          = new DatePicker(LocalDate.now().plusDays(1));
        Spinner<Integer> spEndH         = intSpinner(0, 23, 12);
        Spinner<Integer> spEndM         = intSpinner(0, 59, 0);

        Label            lblErr         = errorLabel();

        VBox vbox = new VBox(10,
                label("Chọn sản phẩm:"), cmbItem,
                label("Bậc tăng giá (VNĐ):"), tfBidIncrement,
                label("Thời gian bắt đầu:"),
                new HBox(5, dpStart, label("Giờ:"), spStartH, label("Phút:"), spStartM),
                label("Thời gian kết thúc:"),
                new HBox(5, dpEnd, label("Giờ:"), spEndH, label("Phút:"), spEndM),
                lblErr
        );
        vbox.setPadding(new Insets(15));

        Stage dialog = buildDialog("Tạo phiên đấu giá", new Scene(vbox, 600, 420));

        Button btnCreate = new Button("Tạo phiên");
        Button btnCancel = new Button("Huỷ");
        btnCancel.setOnAction(e -> dialog.close());
        btnCreate.setOnAction(e -> {
            commitSpinners(spStartH, spStartM, spEndH, spEndM);

            // Delegate validation sang AuctionFormValidator
            AuctionFormValidator.ValidationResult result = auctionValidator.validate(
                    cmbItem.getValue(),
                    tfBidIncrement.getText(),
                    dpStart.getValue(), spStartH.getValue(), spStartM.getValue(),
                    dpEnd.getValue(),   spEndH.getValue(),   spEndM.getValue()
            );

            if (!result.isValid()) {
                lblErr.setText(result.getError().orElse("Dữ liệu không hợp lệ."));
                return;
            }

            lblErr.setText("");
            int    itemId       = itemMap.get(cmbItem.getValue());
            double bidIncrement = auctionValidator.parseBidIncrement(tfBidIncrement.getText());
            LocalDateTime start = auctionValidator.toDateTime(dpStart.getValue(), spStartH.getValue(), spStartM.getValue());
            LocalDateTime end   = auctionValidator.toDateTime(dpEnd.getValue(),   spEndH.getValue(),   spEndM.getValue());

            new Thread(() -> {
                try {
                    Response res = api.createAuction(itemId, bidIncrement, start, end);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo phiên đấu giá thành công.");
                            dialog.close();
                            loadMyAuctions();
                        } else {
                            lblErr.setText("Lỗi: " + res.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblErr.setText("Lỗi kết nối: " + ex.getMessage()));
                }
            }, "create-auction").start();
        });

        HBox btnBox = new HBox(10, btnCreate, btnCancel);
        btnBox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(btnBox);
        dialog.showAndWait();
    }

    // ── Dialog: Tạo sản phẩm ──────────────────────────────────────────────────

    private void showCreateItemDialog() {
        ComboBox<String> cmbType = new ComboBox<>();
        cmbType.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        cmbType.setValue("ELECTRONICS");

        TextField tfName       = new TextField();     tfName.setPromptText("Tên sản phẩm");
        TextArea  taDesc       = new TextArea();       taDesc.setPromptText("Mô tả sản phẩm"); taDesc.setPrefRowCount(3);
        TextField tfStartPrice = new TextField();     tfStartPrice.setPromptText("Giá khởi điểm (VNĐ)");

        Label     lblExtra1    = label("Thương hiệu:");
        TextField tfExtra1     = new TextField();     tfExtra1.setPromptText("VD: Samsung, Picasso, Toyota...");
        Label     lblExtra2    = label("Bảo hành (tháng):");
        TextField tfExtra2     = new TextField();     tfExtra2.setPromptText("VD: 12");

        // Cập nhật label khi đổi loại
        cmbType.setOnAction(e -> {
            switch (cmbType.getValue()) {
                case "ELECTRONICS" -> { lblExtra1.setText("Thương hiệu:");  lblExtra2.setText("Bảo hành (tháng):"); tfExtra2.setPromptText("VD: 12"); }
                case "ART"         -> { lblExtra1.setText("Tên tác giả:");  lblExtra2.setText("Chất liệu:");        tfExtra2.setPromptText("VD: Sơn dầu"); }
                case "VEHICLE"     -> { lblExtra1.setText("Biển số xe:");   lblExtra2.setText("Năm sản xuất:");     tfExtra2.setPromptText("VD: 2020"); }
            }
        });

        // Image picker
        final String[]   chosenBase64 = {null};
        ImageView        preview      = new ImageView();
        preview.setFitWidth(180); preview.setFitHeight(110); preview.setPreserveRatio(true);
        Label            lblImgPath   = new Label("Chưa chọn ảnh");
        lblImgPath.setStyle("-fx-text-fill:#555; -fx-font-size:11;");
        Button btnChooseImg = new Button("📁 Chọn ảnh sản phẩm");
        btnChooseImg.setOnAction(e -> pickImage(btnChooseImg, chosenBase64, preview, lblImgPath));

        Label lblErr = errorLabel();

        VBox vbox = new VBox(10,
                label("Loại sản phẩm:"), cmbType,
                label("Tên sản phẩm:"), tfName,
                label("Mô tả:"), taDesc,
                label("Giá khởi điểm (VNĐ):"), tfStartPrice,
                lblExtra1, tfExtra1,
                lblExtra2, tfExtra2,
                label("Ảnh sản phẩm:"),
                new HBox(10, btnChooseImg, lblImgPath),
                preview, lblErr
        );
        vbox.setPadding(new Insets(15));

        Stage dialog = buildDialog("Tạo sản phẩm mới",
                new Scene(new ScrollPane(vbox) {{ setFitToWidth(true); setStyle("-fx-background-color:white;"); }}, 500, 640));

        Button btnCreate = new Button("Tạo sản phẩm");
        Button btnCancel = new Button("Huỷ");
        btnCancel.setOnAction(e -> dialog.close());
        btnCreate.setOnAction(e -> {
            String type = cmbType.getValue();

            // Delegate validation sang ItemFormValidator
            ItemFormValidator.ValidationResult result = itemValidator.validate(
                    tfName.getText().trim(),
                    tfStartPrice.getText().trim(),
                    type,
                    tfExtra2.getText().trim()
            );

            if (!result.isValid()) {
                lblErr.setText(result.getError().orElse("Dữ liệu không hợp lệ."));
                return;
            }

            lblErr.setText("");

            // Delegate item creation sang ItemFormValidator — không còn switch trong controller
            Item item = itemValidator.buildItem(
                    type, SessionManager.getUserId(),
                    tfName.getText().trim(),
                    taDesc.getText().trim(),
                    tfStartPrice.getText().trim(),
                    tfExtra1.getText().trim(),
                    tfExtra2.getText().trim(),
                    chosenBase64[0]
            );

            new Thread(() -> {
                try {
                    Response res = api.createItem(item);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo sản phẩm thành công.");
                            dialog.close();
                            activities.add(0, "Tạo sản phẩm mới: " + item.getName());
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

        HBox btnBox = new HBox(10, btnCreate, btnCancel);
        btnBox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(btnBox);
        dialog.showAndWait();
    }

    // ── Notification dialog ────────────────────────────────────────────────────

    private void showNotificationHistoryDialog() {
        ListView<String> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(
                notificationHistory.isEmpty()
                        ? List.of("Chưa có thông báo nào.")
                        : notificationHistory));
        listView.setPrefHeight(300);

        BorderPane root = new BorderPane(listView);
        root.setPadding(new Insets(10));
        Label title = new Label("🔔 Lịch sử thông báo");
        title.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-padding:0 0 8 0;");
        root.setTop(title);

        Stage dialog = buildDialog("Thông báo hệ thống", new Scene(root, 500, 360));
        dialog.showAndWait();
    }

    // ── Private UI helpers ────────────────────────────────────────────────────

    private void bindBackgroundImage() {
        if (backgroundImage != null && rootPane != null) {
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }
    }

    private void hideActivitiesPanel() {
        if (panelActivities != null) {
            panelActivities.setVisible(false);
            panelActivities.setManaged(false);
        }
    }

    private void setSellerToggleActive() {
        if (modeBuyerButton == null || modeSellerButton == null) return;
        modeBuyerButton.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:rgba(255,255,255,0.8);" +
                        "-fx-font-size:12; -fx-background-radius:16; -fx-padding:4 12; -fx-cursor:hand;");
        modeSellerButton.setStyle(
                "-fx-background-color:white; -fx-text-fill:#065f3b; -fx-font-weight:bold;" +
                        "-fx-font-size:12; -fx-background-radius:16; -fx-padding:4 12; -fx-cursor:hand;");
    }

    private void setActiveMenu(Button activeBtn) {
        for (Button btn : new Button[]{ btnMenuLots, btnMenuNotif, btnMenuActivity }) {
            if (btn != null)
                btn.setStyle(btn == activeBtn ? MENU_ACTIVE : MENU_INACTIVE);
        }
    }

    private void updateStats() {
        SellerTableManager.Stats s = tableManager.getStats();
        lblTotalLots.setText(String.valueOf(s.total()));
        lblActiveLots.setText(String.valueOf(s.active()));
        lblSoldLots.setText(String.valueOf(s.sold()));
    }

    private void renderActivities() {
        vboxActivities.getChildren().clear();
        if (activities.isEmpty()) {
            Label empty = new Label("Chưa có hoạt động.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill:white;");
            vboxActivities.getChildren().add(empty);
            return;
        }
        for (String act : activities) {
            Label lbl = new Label("• " + act);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill:white;");
            vboxActivities.getChildren().add(lbl);
        }
    }

    private String buildActivityLabel(Auction a) {
        return "Phiên #" + a.getAuctionId()
                + " — " + a.getItem().getName()
                + " — " + tableManager.toRow(a).getStatus();
    }

    // ── Static UI factory helpers ─────────────────────────────────────────────

    private static Label label(String text) {
        return new Label(text);
    }

    private static Label errorLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill:#c62828; -fx-font-size:11;");
        return lbl;
    }

    private static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> sp = new Spinner<>(min, max, initial);
        sp.setEditable(true);
        sp.setPrefWidth(70);
        return sp;
    }

    private static Stage buildDialog(String title, Scene scene) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle(title);
        d.setScene(scene);
        return d;
    }

    private static void commitSpinners(Spinner<?>... spinners) {
        for (Spinner<?> sp : spinners) sp.commitValue();
    }

    private static void pickImage(Button anchor, String[] base64Out,
                                  ImageView preview, Label pathLabel) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File file = fc.showOpenDialog(anchor.getScene().getWindow());
        if (file == null) return;
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            base64Out[0] = Base64.getEncoder().encodeToString(bytes);
            pathLabel.setText(file.getName());
            preview.setImage(new javafx.scene.image.Image(
                    file.toURI().toString(), 180, 110, true, true));
        } catch (Exception ex) {
            pathLabel.setText("Lỗi đọc file: " + ex.getMessage());
            base64Out[0] = null;
        }
    }
}