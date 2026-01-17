package gui.logic.staff;

import java.util.Map;

import entities.MonthlyReport;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import logic.BistroClientGUI;
import javafx.geometry.Pos;

public class AnalyticsPanel {

    // ===== KPIs =====
    @FXML private Label totalReservationsLabel;
    @FXML private Label customersThisMonthLabel;
    @FXML private Label distributionTitleLabel;
    @FXML private Label distributionSubtitleLabel;


    // ===== Charts =====
    @FXML private BarChart<String, Number> arrivalBarChart;
    @FXML private LineChart<String, Number> reservationsLineChart;
    @FXML private Label totalOnTimeLabel;
    @FXML private Label totalLateLabel;

    // ===== Bottom =====
    @FXML private VBox peakTimesBox;

    // ===== Selector =====
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private ComboBox<String> monthCombo;

    public void initialize() {
    	reportTypeCombo.getItems().setAll("MEMBERS", "TIMES");
    	reportTypeCombo.getSelectionModel().select("MEMBERS");

    	loadMonths();

    	reportTypeCombo.setOnAction(e -> loadMonths());
    }

    private void loadMonths() {
        String type = reportTypeCombo.getValue();
        if (type == null) return;

        new Thread(() -> {
            BistroClientGUI.client.getMonthlyReportsCTRL().requestAvailableMonths(type);

            Platform.runLater(() -> {
                monthCombo.getItems().clear();

                for (int[] ym : BistroClientGUI.client.getMonthlyReportsCTRL().getAvailableMonths()) {
                    monthCombo.getItems().add(ym[0] + "-" + String.format("%02d", ym[1]));
                }

                // Pick previous month if available
                selectPreviousMonthIfExists();

                // Load only if we actually have something selected
                if (monthCombo.getValue() != null && !isCurrentMonthSelected()) {
                    onLoadReport();
                }
            });
        }, "analytics-load-months").start();
    }

    
    /**
     * Selects the previous month (relative to today) if it exists in the month list.
     * Falls back to first item if not found.
     */
    private void selectPreviousMonthIfExists() {
        if (monthCombo.getItems().isEmpty()) return;

        java.time.YearMonth prev = java.time.YearMonth.now().minusMonths(1);
        String target = prev.getYear() + "-" + String.format("%02d", prev.getMonthValue());

        int idx = monthCombo.getItems().indexOf(target);
        if (idx >= 0) {
            monthCombo.getSelectionModel().select(idx);
        } else {
            monthCombo.getSelectionModel().selectFirst();
        }
    }


    @FXML
    private void onLoadReport() {
        String ym = monthCombo.getValue();
        if (ym == null) return;

        int year = Integer.parseInt(ym.substring(0, 4));
        int month = Integer.parseInt(ym.substring(5, 7));

        new Thread(() -> {
            BistroClientGUI.client.getMonthlyReportsCTRL()
                    .requestReport(reportTypeCombo.getValue(), year, month, false);

            MonthlyReport report =
                    BistroClientGUI.client.getMonthlyReportsCTRL().getCurrentMonthlyReport();

            Platform.runLater(() -> updateDashboard(report));
        }).start();
    }

    private void updateDashboard(MonthlyReport r) {
        if (r == null) return;

        totalReservationsLabel.setText(String.valueOf(r.getTotalReservations()));
        customersThisMonthLabel.setText(String.valueOf(r.getTotalCostumer()));

        updateBarChart(r);
        updateLineChart(r);
        updateBottom(r);
    }

    private void updateBarChart(MonthlyReport r) {
        arrivalBarChart.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();

        if ("TIMES".equalsIgnoreCase(r.getReportType())) {
            s.getData().add(new XYChart.Data<>("On Time", r.getTotalOnTimeCostumer()));
            s.getData().add(new XYChart.Data<>("Late", r.getTotalLateCostumer()));
            totalOnTimeLabel.setText(String.valueOf(r.getTotalOnTimeCostumer()));
            totalLateLabel.setText(String.valueOf(r.getTotalLateCostumer()));
        } else {
            s.getData().add(new XYChart.Data<>("Reservations", r.getTotalReservations()));
            s.getData().add(new XYChart.Data<>("Waitlist", sum(r.getWaitlistByDay())));
        }

        arrivalBarChart.getData().add(s);
    }

    private void updateLineChart(MonthlyReport r) {
        reservationsLineChart.getData().clear();
        prepareDayAxisFromCombo();
        if ("TIMES".equalsIgnoreCase(r.getReportType())) {
            addSeries("On Time", r.getOnTimeArrivalsByDay());
            addSeries("Late", r.getLateArrivalsByDay());
        } else {
            addSeries("Reservations", r.getReservationsByDay());
            addSeries("Waitlist", r.getWaitlistByDay());
        }
    }

    /**
     * Adds a sorted series (by day-of-month) to the trend chart.
     *
     * @param name series name
     * @param data day->count map (may be null)
     */
    private void addSeries(String name, Map<Integer, Integer> data) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(name);

        if (data != null && !data.isEmpty()) {
            data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> s.getData().add(new XYChart.Data<>(
                    String.format("%02d", e.getKey()),
                    e.getValue()
                )));
        }

        reservationsLineChart.getData().add(s);
    }




    /**
     * Updates the bottom distribution card to match the project story.
     *
     * <p>TIMES: lateness bucket distribution (share of arrivals by lateness range).</p>
     * <p>MEMBERS: share of waiting-list joins by day-of-month.</p>
     *
     * @param r report data
     */
    private void updateBottom(MonthlyReport r) {
        peakTimesBox.getChildren().clear();

        if ("TIMES".equalsIgnoreCase(r.getReportType())) {
            distributionTitleLabel.setText("Arrival Lateness Breakdown");
            distributionSubtitleLabel.setText("Share of arrivals by lateness range (relative to reservation time).");
            populateDistribution(renameLatenessBuckets(r.getLatenessBuckets()));
            return;
        }

        distributionTitleLabel.setText("Waiting List Breakdown");
        distributionSubtitleLabel.setText("Share of waiting-list joins across the month (by day).");
        populateDistributionFromIntMapDayLabel(r.getWaitlistByDay());
    }


    private void populateDistribution(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return;
        int total = map.values().stream().mapToInt(i -> i).sum();

        map.forEach((k, v) -> addRow(k, v, total));
    }

    private void populateDistributionFromIntMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) return;
        int total = map.values().stream().mapToInt(i -> i).sum();

        map.forEach((k, v) -> addRow(String.valueOf(k), v, total));
    }

    /**
     * Adds a single distribution row: label | progress bar | percent.
     *
     * @param label row label
     * @param value bucket value
     * @param total sum of all values
     */
    private void addRow(String label, int value, int total) {
        double pct = (total <= 0) ? 0.0 : (double) value / total;

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = new Label(label);
        l.setMinWidth(70);
        l.setMaxWidth(70);

        ProgressBar pb = new ProgressBar(pct);
        pb.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pb, Priority.ALWAYS);

        Label p = new Label(String.format("%.0f%%", pct * 100));
        p.setMinWidth(55);
        p.setMaxWidth(55);
        p.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(l, pb, p);
        peakTimesBox.getChildren().add(row);
    }


    private int sum(Map<Integer, Integer> map) {
        if (map == null) return 0;
        return map.values().stream().mapToInt(i -> i).sum();
    }
    
    /**
     * Prefills x-axis categories with all days of the selected month.
     *
     * @param year report year
     * @param month report month (1-12)
     */
    private void prepareDayAxis(int year, int month) {
        javafx.scene.chart.CategoryAxis x =
            (javafx.scene.chart.CategoryAxis) reservationsLineChart.getXAxis();

        x.getCategories().clear();
        int days = java.time.YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            x.getCategories().add(String.format("%02d", d));
        }
    }
    
    /**
     * Prepares the x-axis categories (01..daysInMonth) based on the selected month.
     */
    private void prepareDayAxisFromCombo() {
        String ym = monthCombo.getValue(); // "YYYY-MM"
        if (ym == null) return;

        int year = Integer.parseInt(ym.substring(0, 4));
        int month = Integer.parseInt(ym.substring(5, 7));

        javafx.scene.chart.CategoryAxis x =
            (javafx.scene.chart.CategoryAxis) reservationsLineChart.getXAxis();

        x.getCategories().clear();

        int days = java.time.YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            x.getCategories().add(String.format("%02d", d));
        }
    }
    /**
     * Renames lateness buckets to user-friendly labels.
     *
     * @param raw raw bucket map
     * @return map with clearer labels
     */
    private Map<String, Integer> renameLatenessBuckets(Map<String, Integer> raw) {
        if (raw == null || raw.isEmpty()) return raw;

        java.util.LinkedHashMap<String, Integer> out = new java.util.LinkedHashMap<>();
        raw.forEach((k, v) -> {
            String label = k;
            if ("on-time".equalsIgnoreCase(k)) label = "On time (<= 0m)";
            else if ("1-5".equalsIgnoreCase(k)) label = "Late 1–5 min";
            else if ("6-15".equalsIgnoreCase(k)) label = "Late 6–15 min";
            else if ("16+".equalsIgnoreCase(k)) label = "Late 16+ min";
            out.put(label, v);
        });
        return out;
    }

    /**
     * Populates distribution rows for day-of-month maps but labels the day clearly ("Day 01").
     *
     * @param map day->count
     */
    private void populateDistributionFromIntMapDayLabel(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) return;
        int total = map.values().stream().mapToInt(i -> i).sum();

        map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> addRow("Day " + String.format("%02d", e.getKey()), e.getValue(), total));
    }
    /**
     * Generates a report for the current month "so far" and loads it.
     * Uses force=true to overwrite any cached partial report.
     */
    @FXML
    private void onGenerateThisMonth() {
        String type = reportTypeCombo.getValue();
        if (type == null) return;

        java.time.YearMonth now = java.time.YearMonth.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        new Thread(() -> {
            BistroClientGUI.client.getMonthlyReportsCTRL().requestReport(type, year, month, true);
            MonthlyReport report = BistroClientGUI.client.getMonthlyReportsCTRL().getCurrentMonthlyReport();

            Platform.runLater(() -> {
                // Ensure combo shows current month even if not in listReportMonths
                String ym = year + "-" + String.format("%02d", month);
                if (!monthCombo.getItems().contains(ym)) monthCombo.getItems().add(0, ym);
                monthCombo.getSelectionModel().select(ym);

                updateDashboard(report);
            });
        }, "analytics-generate-current-month").start();
    }

    /**
     * @return true if monthCombo is currently set to the current month (YYYY-MM)
     */
    private boolean isCurrentMonthSelected() {
        String ym = monthCombo.getValue();
        if (ym == null) return false;

        java.time.YearMonth now = java.time.YearMonth.now();
        String cur = now.getYear() + "-" + String.format("%02d", now.getMonthValue());
        return cur.equals(ym);
    }


}
