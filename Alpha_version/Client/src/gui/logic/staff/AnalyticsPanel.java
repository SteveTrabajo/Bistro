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
        reportTypeCombo.getSelectionModel().selectFirst();

        loadMonths();

        reportTypeCombo.setOnAction(e -> loadMonths());
    }

    private void loadMonths() {
        new Thread(() -> {
            BistroClientGUI.client.getMonthlyReportsCTRL()
                    .requestAvailableMonths(reportTypeCombo.getValue());

            Platform.runLater(() -> {
                monthCombo.getItems().clear();
                for (int[] ym : BistroClientGUI.client.getMonthlyReportsCTRL().getAvailableMonths()) {
                    monthCombo.getItems().add(ym[0] + "-" + String.format("%02d", ym[1]));
                }
                if (!monthCombo.getItems().isEmpty()) {
                    monthCombo.getSelectionModel().selectFirst();
                    onLoadReport();
                }
            });
        }).start();
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
        prepareDayAxis(r.getYearInt(), r.getMonthInt());

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
                .forEach(e -> {
                    // Use 2-digit day label so "1" and "01" don't collide visually and ordering is stable
                    String dayLabel = String.format("%02d", e.getKey());
                    s.getData().add(new XYChart.Data<>(dayLabel, e.getValue()));
                });
        }

        reservationsLineChart.getData().add(s);
    }


    private void updateBottom(MonthlyReport r) {
        peakTimesBox.getChildren().clear();

        if ("TIMES".equalsIgnoreCase(r.getReportType())) {
            populateDistribution(r.getLatenessBuckets());
        } else {
            populateDistributionFromIntMap(r.getWaitlistByDay());
        }
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
     * Prepares the x-axis categories for the selected year/month so points don’t collapse.
     *
     * @param year report year
     * @param month report month (1-12)
     */
    private void prepareDayAxis(int year, int month) {
        // assumes xAxis is CategoryAxis (as in your FXML)
        javafx.scene.chart.CategoryAxis x =
            (javafx.scene.chart.CategoryAxis) reservationsLineChart.getXAxis();

        x.getCategories().clear();

        int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            x.getCategories().add(String.format("%02d", d));
        }
    }

}
