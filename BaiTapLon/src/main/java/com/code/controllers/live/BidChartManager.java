package com.code.controllers.live;

import com.code.models.Bid;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Quản lý toàn bộ LineChart trong màn hình LiveBidding.
 *
 * Trách nhiệm:
 *  - Setup chart lần đầu (trục, formatter, màu nền)
 *  - Load lại toàn bộ lịch sử bid khi vào phòng
 *  - Thêm điểm realtime khi có bid mới
 *  - Giữ tối đa MAX_POINTS điểm để chart không rối
 *
 * Tách ra để LiveBiddingController không còn biết gì về XYChart.
 */
public class BidChartManager {

    private static final int MAX_POINTS = 30;

    private final LineChart<Number, Number>        chart;
    private final XYChart.Series<Number, Number>   series;
    private       int                              pointCount = 0;

    public BidChartManager(LineChart<Number, Number> chart, NumberAxis yAxis) {
        this.chart  = chart;
        this.series = new XYChart.Series<>();
        series.setName("Giá đặt");
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        setupYAxisFormatter(yAxis);
        applyChartStyle();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Load lại toàn bộ lịch sử bid (gọi khi vừa vào phòng).
     * Tự sort theo timestamp để chart luôn đồng biến.
     */
    public void loadHistory(List<Bid> bids) {
        if (bids == null) return;
        series.getData().clear();
        pointCount = 0;

        List<Bid> sorted = new ArrayList<>(bids);
        sorted.sort(Comparator.comparing(Bid::getTimestamp)); // cũ → mới

        for (Bid bid : sorted) {
            pointCount++;
            series.getData().add(new XYChart.Data<>(pointCount, bid.getAmount()));
        }

        styleSeriesLine();
    }

    /**
     * Thêm một điểm mới realtime (gọi khi nhận BID_PLACED event).
     * Điểm mới = đỏ nổi bật, điểm cũ = xanh nhạt.
     */
    public void addPoint(double amount) {
        int index = ++pointCount;
        XYChart.Data<Number, Number> point = new XYChart.Data<>(index, amount);
        series.getData().add(point);

        // Giữ tối đa MAX_POINTS
        if (series.getData().size() > MAX_POINTS) {
            series.getData().remove(0);
        }

        Platform.runLater(() -> {
            // Điểm áp chót → xanh nhạt
            int size = series.getData().size();
            if (size >= 2) {
                XYChart.Data<Number, Number> prev = series.getData().get(size - 2);
                if (prev.getNode() != null)
                    prev.getNode().setStyle(
                            "-fx-background-color:#6ee7b7, #002222;" +
                                    "-fx-background-radius:4px;");
            }
            // Điểm mới nhất → đỏ
            if (point.getNode() != null)
                point.getNode().setStyle(
                        "-fx-background-color:#ef5350, #002222;" +
                                "-fx-background-radius:5px;");

            styleSeriesLine();
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setupYAxisFormatter(NumberAxis yAxis) {
        yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                double v = n.doubleValue();
                if (v >= 1_000_000_000) return String.format("%.1fB", v / 1_000_000_000);
                if (v >= 1_000_000)     return String.format("%.1fM", v / 1_000_000);
                if (v >= 1_000)         return String.format("%.0fk", v / 1_000);
                return String.format("%.0f", v);
            }
            @Override public Number fromString(String s) { return 0; }
        });
    }

    private void applyChartStyle() {
        Platform.runLater(() -> {
            if (chart.lookup(".chart-plot-background") != null)
                chart.lookup(".chart-plot-background")
                        .setStyle("-fx-background-color:#002222;");

            chart.lookupAll(".chart-horizontal-grid-lines")
                    .forEach(n -> n.setStyle("-fx-stroke:rgba(110,231,183,0.2);"));

            chart.lookupAll(".axis-label")
                    .forEach(n -> n.setStyle("-fx-text-fill:#c8f5e1; -fx-font-size:10px;"));

            chart.lookupAll(".tick-mark")
                    .forEach(n -> n.setStyle("-fx-stroke:#4a9a7a;"));

            styleSeriesLine();
        });
    }

    private void styleSeriesLine() {
        if (series.getNode() != null)
            series.getNode().setStyle("-fx-stroke:#6ee7b7; -fx-stroke-width:2px;");
    }
}