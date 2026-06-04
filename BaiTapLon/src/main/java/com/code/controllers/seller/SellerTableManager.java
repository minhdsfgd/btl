package com.code.controllers.seller;

import com.code.models.Auction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.code.models.AuctionStatus.*;

//Quản lý toàn bộ {@link TableView} của màn hình Seller Dashboard.

public class SellerTableManager {

    // ── Style constants ───────────────────────────────────────────────────────

    private static final String ROW_NORMAL   =
            "-fx-background-color:white; -fx-border-color:#e0e0e0;" +
                    "-fx-border-width:0 0 1 0;";
    private static final String ROW_SELECTED =
            "-fx-background-color:#c8f5e1; -fx-border-color:#e0e0e0;" +
                    "-fx-border-width:0 0 1 0;";
    private static final String ROW_EMPTY    =
            "-fx-background-color:transparent; -fx-border-color:transparent;";
    private static final String BTN_CANCEL   =
            "-fx-background-color:#c62828; -fx-text-fill:white;" +
                    "-fx-font-size:10; -fx-background-radius:4; -fx-padding:3 6; -fx-cursor:hand;";

    // ── State ─────────────────────────────────────────────────────────────────

    private final TableView<LotRow>             table;
    private final ObservableList<LotRow>        allLots = FXCollections.observableArrayList();
    private final CancelCallback                onCancel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SellerTableManager(TableView<LotRow>    table,
                              TableColumn<LotRow, String> colId,
                              TableColumn<LotRow, String> colProject,
                              TableColumn<LotRow, String> colPrice,
                              TableColumn<LotRow, String> colStatus,
                              TableColumn<LotRow, String> colTimeInfo,
                              TableColumn<LotRow, Void>   colAction,
                              CancelCallback onCancel) {
        this.table    = table;
        this.onCancel = onCancel;
        configure(colId, colProject, colPrice, colStatus, colTimeInfo, colAction);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void load(List<Auction> auctions) {
        allLots.clear();
        for (Auction a : auctions) {
            allLots.add(toRow(a));
        }
        table.setItems(allLots);
    }

    public void filter(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            table.setItems(allLots);
            return;
        }
        String kw = keyword.toLowerCase();
        table.setItems(allLots.filtered(l ->
                l.getId().toLowerCase().contains(kw)
                        || l.getProject().toLowerCase().contains(kw)
                        || l.getStatus().toLowerCase().contains(kw)));
    }

    public Stats getStats() {
        long total  = allLots.size();
        long active = allLots.stream().filter(l -> "Đang đấu giá".equals(l.getStatus())).count();
        long sold   = allLots.stream().filter(l -> "Đã bán".equals(l.getStatus())).count();
        return new Stats(total, active, sold);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public LotRow toRow(Auction a) {
        return new LotRow(
                "#" + a.getAuctionId(),
                a.getItem().getName(),
                String.format("%,.0f đ", a.getCurrentPrice()),
                mapStatus(a),
                formatTime(a)
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void configure(TableColumn<LotRow, String> colId,
                           TableColumn<LotRow, String> colProject,
                           TableColumn<LotRow, String> colPrice,
                           TableColumn<LotRow, String> colStatus,
                           TableColumn<LotRow, String> colTimeInfo,
                           TableColumn<LotRow, Void>   colAction) {

        // Bind data
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colProject.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProject()));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrice()));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colTimeInfo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTimeInfo()));

        // Chữ đen cho tất cả cột text
        for (TableColumn<LotRow, String> col : List.of(colId, colProject, colPrice, colStatus, colTimeInfo)) {
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill: black;");
                }
            });
        }

        colId.setStyle("-fx-alignment: CENTER;");
        colPrice.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");
        colTimeInfo.setStyle("-fx-alignment: CENTER;");
        colAction.setStyle("-fx-alignment: CENTER;");

        table.setStyle("-fx-control-inner-background: white;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        applyRowFactory();
        configureCancelColumn(colAction);
    }

    private void applyRowFactory() {
        table.setRowFactory(tv -> {
            TableRow<LotRow> row = new TableRow<>() {
                @Override
                protected void updateItem(LotRow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle(ROW_EMPTY);
                    } else {
                        setStyle(isSelected() ? ROW_SELECTED : ROW_NORMAL);
                    }
                }
            };
            row.selectedProperty().addListener((obs, was, isNow) -> {
                if (row.getItem() != null)
                    row.setStyle(isNow ? ROW_SELECTED : ROW_NORMAL);
            });
            return row;
        });
    }

    private void configureCancelColumn(TableColumn<LotRow, Void> colAction) {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Hủy phiên");
            {
                btnCancel.setStyle(BTN_CANCEL);
                btnCancel.setOnAction(e -> {
                    LotRow row = getTableView().getItems().get(getIndex());
                    onCancel.execute(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                LotRow row = getTableView().getItems().get(getIndex());
                boolean canCancel = "Đang đấu giá".equals(row.getStatus())
                        || "Sắp mở".equals(row.getStatus());
                setGraphic(canCancel ? btnCancel : null);
            }
        });
    }

    private static String mapStatus(Auction a) {
        return switch (a.getStatus()) {
            case RUNNING  -> "Đang đấu giá";
            case OPEN     -> "Sắp mở";
            case FINISHED -> "Đã kết thúc";
            case PAID     -> "Đã bán";
            case CANCELED -> "Đã hủy";
        };
    }

    private static String formatTime(Auction a) {
        LocalDateTime now = LocalDateTime.now();
        if (a.getStatus() == RUNNING) {
            long mins = ChronoUnit.MINUTES.between(now, a.getEndTime());
            return mins > 0 ? mins + " phút còn lại" : "Sắp kết thúc";
        }
        if (a.getStatus() == OPEN) {
            long mins = ChronoUnit.MINUTES.between(now, a.getStartTime());
            return mins <= 0 ? "Bắt đầu ngay" : "Bắt đầu trong " + mins + " phút";
        }
        return "Kết thúc";
    }

    // ── Nested types ─────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface CancelCallback {
        void execute(LotRow row);
    }

    public record Stats(long total, long active, long sold) {}

    // ── Row Model ─────────────────────────────────────────────────────────────

    public static final class LotRow {
        private final String id;
        private final String project;
        private final String price;
        private final String status;
        private final String timeInfo;

        public LotRow(String id, String project, String price,
                      String status, String timeInfo) {
            this.id       = id;
            this.project  = project;
            this.price    = price;
            this.status   = status;
            this.timeInfo = timeInfo;
        }

        public String getId()       { return id;       }
        public String getProject()  { return project;  }
        public String getPrice()    { return price;    }
        public String getStatus()   { return status;   }
        public String getTimeInfo() { return timeInfo; }
    }
}