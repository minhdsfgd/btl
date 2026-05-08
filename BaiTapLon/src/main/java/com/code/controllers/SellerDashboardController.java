package com.code.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SellerDashboardController implements Initializable {

    // ── Header ───────────────────────────────────────────────────────────────
    @FXML private Label lblUsername;

    // ── Stats cards ──────────────────────────────────────────────────────────
    @FXML private Label lblTotalLots;
    @FXML private Label lblActiveLots;
    @FXML private Label lblSoldLots;

    // ── Lot table ─────────────────────────────────────────────────────────────
    @FXML private TextField  txtSearch;
    @FXML private TableView<LotRow>       tableLots;
    @FXML private TableColumn<LotRow, String> colId;
    @FXML private TableColumn<LotRow, String> colProject;
    @FXML private TableColumn<LotRow, String> colPrice;
    @FXML private TableColumn<LotRow, String> colStatus;

    // ── Revenue chart & activity feed ────────────────────────────────────────
    @FXML private LineChart<String, Number> chartRevenue;
    @FXML private VBox vboxActivities;

    // ── Sidebar logout ────────────────────────────────────────────────────────
    @FXML private Button btnLogout;

    // ── Internal state ────────────────────────────────────────────────────────
    private final ObservableList<LotRow> allLots = FXCollections.observableArrayList();

    // ========================================================================
    //  Initializable
    // ========================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        styleChart();
    }

    // ========================================================================
    //  Public API
    // ========================================================================

    /** Display logged-in seller's name. */
    public void setUsername(String username) {
        lblUsername.setText(username);
    }

    /**
     * Populate the lot table and refresh all stat cards.
     */
    public void setLots(List<LotRow> lots) {
        allLots.setAll(lots);
        tableLots.setItems(allLots);
        refreshStats();
    }

    /**
     * Feed revenue data into the line chart.
     * Keys = month labels (e.g. "T1", "T2" …), values = revenue amounts.
     */
    public void setRevenueData(List<String> labels, List<Number> values) {
        chartRevenue.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < labels.size(); i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), values.get(i)));
        }
        chartRevenue.getData().add(series);

        // Style the line green after data is added
        series.getNode().setStyle("-fx-stroke: #4caf50; -fx-stroke-width: 2;");
    }

    /**
     * Add an activity entry to the recent-activity feed.
     *
     * @param icon  small emoji / symbol, e.g. "🟢"
     * @param text  activity description
     * @param time  timestamp string, e.g. "10:32"
     */
    public void addActivity(String icon, String text, String time) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 0, 4, 0));
        row.setStyle("-fx-border-color: #054529; -fx-border-width: 0 0 1 0;");

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(12));

        Label textLbl = new Label(text);
        textLbl.setTextFill(Color.web("#cccccc"));
        textLbl.setFont(Font.font(10));
        textLbl.setWrapText(true);
        textLbl.setMaxWidth(130);

        Label timeLbl = new Label(time);
        timeLbl.setTextFill(Color.web("#6a8a75"));
        timeLbl.setFont(Font.font(9));

        VBox info = new VBox(2, textLbl, timeLbl);
        row.getChildren().addAll(iconLbl, info);
        vboxActivities.getChildren().add(0, row);   // newest on top
    }

    // ========================================================================
    //  FXML handlers
    // ========================================================================

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            tableLots.setItems(allLots);
            return;
        }
        ObservableList<LotRow> filtered = allLots.stream()
                .filter(r -> r.getId().toLowerCase().contains(keyword)
                        || r.getProject().toLowerCase().contains(keyword)
                        || r.getStatus().toLowerCase().contains(keyword))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        tableLots.setItems(filtered);
    }

    @FXML
    private void handleLogout() {
        // TODO: navigate back to login screen
        Stage stage = (Stage) btnLogout.getScene().getWindow();
        stage.close();
    }

    // ========================================================================
    //  Private helpers
    // ========================================================================

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Colour-code the status cell
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String color = switch (status) {
                        case "Đang đấu"  -> "#4caf50";
                        case "Đã bán"    -> "#ff9800";
                        case "Chờ duyệt" -> "#4dd0e1";
                        default          -> "#a0a0a0";
                    };
                    setStyle("-fx-text-fill: " + color + "; -fx-alignment: CENTER;");
                }
            }
        });

        colPrice.setStyle("-fx-alignment: CENTER_RIGHT;");
        tableLots.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableLots.setPlaceholder(new Label("Không có dữ liệu") {{
            setStyle("-fx-text-fill: #4a7a5e; -fx-font-size: 11;");
        }});

        // Live search on key release
        txtSearch.setOnKeyReleased(e -> handleSearch());
    }

    private void styleChart() {
        chartRevenue.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-plot-background-color: transparent;"
                        + "-fx-chart-horizontal-grid-lines-visible: false;");
        chartRevenue.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: transparent;");
    }

    private void refreshStats() {
        lblTotalLots.setText(String.valueOf(allLots.size()));
        lblActiveLots.setText(String.valueOf(
                allLots.stream().filter(r -> "Đang đấu".equals(r.getStatus())).count()));
        lblSoldLots.setText(String.valueOf(
                allLots.stream().filter(r -> "Đã bán".equals(r.getStatus())).count()));
    }

    // ========================================================================
    //  Inner model
    // ========================================================================

    public static class LotRow {
        private final String id;
        private final String project;
        private final String price;
        private final String status;

        public LotRow(String id, String project, String price, String status) {
            this.id      = id;
            this.project = project;
            this.price   = price;
            this.status  = status;
        }

        public String getId()      { return id; }
        public String getProject() { return project; }
        public String getPrice()   { return price; }
        public String getStatus()  { return status; }
    }
}