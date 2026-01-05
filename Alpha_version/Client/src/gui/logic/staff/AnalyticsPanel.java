package gui.logic.staff;

import java.net.URL;
import java.util.ResourceBundle;

import entities.MonthlyReport;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AnalyticsPanel {
	// --- KPI Labels ---
    @FXML private Label totalReservationsLabel;
    @FXML private Label totalReservationsDelta;
    
    @FXML private Label avgMonthlyLabel;
    @FXML private Label avgMonthlyDelta;
    
    @FXML private Label onTimeRateLabel;
    @FXML private Label onTimeDelta;
    
    @FXML private Label customersThisMonthLabel;
    @FXML private Label currentMonthLabel;

    // --- Chart 1: Arrival Times ---
    @FXML private BarChart<String, Number> arrivalBarChart;
    @FXML private Label totalOnTimeLabel;
    @FXML private Label totalLateLabel;

    // --- Chart 2: Monthly Trends ---
    @FXML private LineChart<String, Number> reservationsLineChart;
    @FXML private Label peakMonthLabel;
    @FXML private Label peakMonthValueLabel;
    @FXML private Label lowestMonthLabel;
    @FXML private Label lowestMonthValueLabel;
    @FXML private Label growthRateLabel;

    // --- Dynamic Bottom Sections ---
    @FXML private VBox peakTimesBox;
    @FXML private VBox partySizeBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial setup if needed (e.g., clear charts)
        arrivalBarChart.setAnimated(true);
        reservationsLineChart.setAnimated(true);
    }

    /**
     * Call this method when data arrives from the server via OCSF client.
     * @param data The DTO containing all statistics.
     */
    public void updateDashboard(MonthlyReport data) {
        if (data == null) return;

        // 1. Update KPIs
        totalReservationsLabel.setText(String.valueOf(data.getTotalReservations2025()));
        // Assuming getter for delta exists in DTO
        setDeltaLabel(totalReservationsDelta, 5.2); // Example value, replace with data.get...

        avgMonthlyLabel.setText(String.valueOf(data.getAvgMonthlyReservations())); // Add getter
        setDeltaLabel(avgMonthlyDelta, 2.1);

        onTimeRateLabel.setText(String.format("%.1f%%", data.getOnTimeArrivalRate() * 100));
        setDeltaLabel(onTimeDelta, -0.5);

        customersThisMonthLabel.setText(String.valueOf(data.getCustomersThisMonth())); // Add getter
        currentMonthLabel.setText("January"); // Or data.getCurrentMonthName()

        // 2. Update Bar Chart (Arrival Times)
        updateArrivalChart(data);

        // 3. Update Line Chart (Trends)
        updateTrendChart(data);

        // 4. Update Summaries
        totalOnTimeLabel.setText(String.valueOf(data.getTotalOnTime()));
        totalLateLabel.setText(String.valueOf(data.getTotalLate()));
        
        peakMonthLabel.setText(data.getPeakMonth()); // Add getter
        peakMonthValueLabel.setText(String.valueOf(data.getPeakMonthValue())); // Add getter
        
        lowestMonthLabel.setText(data.getLowestMonth()); // Add getter
        lowestMonthValueLabel.setText(String.valueOf(data.getLowestMonthValue())); // Add getter
        
        growthRateLabel.setText(String.format("%+.1f%%", data.getGrowthRateYearly())); // Add getter

        // 5. Build Dynamic Bottom Rows
        populateDistributionRows(peakTimesBox, data.getPeakReservationTimes(), "ad-bar-blue");
        populateDistributionRows(partySizeBox, data.getPartySizeDistribution(), "ad-bar-green");
    }

    private void setDeltaLabel(Label label, double value) {
        label.setText(String.format("%+.1f%%", value));
        // Simple logic to change color class based on positive/negative
        label.getStyleClass().removeAll("ad-kpi-delta", "ad-kpi-delta-red");
        if (value >= 0) {
            label.setStyle("-fx-text-fill: #16a34a;"); // Green
        } else {
            label.setStyle("-fx-text-fill: #ef4444;"); // Red
        }
    }

    private void updateArrivalChart(MonthlyReportData data) {
        arrivalBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Arrivals");

        Map<String, Integer> dist = data.getArrivalTimeDistribution();
        if (dist != null) {
            dist.forEach((category, count) -> {
                series.getData().add(new XYChart.Data<>(category, count));
            });
        }
        arrivalBarChart.getData().add(series);
    }

    private void updateTrendChart(MonthlyReportData data) {
        reservationsLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("2025");

        Map<String, Integer> trends = data.getMonthlyReservationsMap();
        // Ensure month order is correct (Logic depends on Map implementation, LinkedHashMap is best)
        if (trends != null) {
            trends.forEach((month, count) -> {
                series.getData().add(new XYChart.Data<>(month, count));
            });
        }
        reservationsLineChart.getData().add(series);
    }

    /**
     * Dynamically creates progress bar rows for the bottom cards
     */
    private void populateDistributionRows(VBox container, Map<String, Integer> dataMap, String colorStyleClass) {
        container.getChildren().clear();
        if (dataMap == null || dataMap.isEmpty()) return;

        // Calculate total for percentage calculation
        int total = dataMap.values().stream().mapToInt(Integer::intValue).sum();

        dataMap.forEach((key, value) -> {
            double progress = (double) value / total;
            
            // Build the UI structure matching your CSS logic
            VBox rowContainer = new VBox(4);
            rowContainer.getStyleClass().add("ad-row");

            // Title and Percentage Row
            HBox labelsBox = new HBox();
            Label title = new Label(key);
            title.getStyleClass().add("ad-row-title");
            
            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label pct = new Label(String.format("%.0f%%", progress * 100));
            pct.getStyleClass().add("ad-row-pct");
            
            labelsBox.getChildren().addAll(title, spacer, pct);

            // Progress Bar
            ProgressBar pb = new ProgressBar(progress);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.getStyleClass().add(colorStyleClass); // e.g., ad-bar-green

            rowContainer.getChildren().addAll(labelsBox, pb);
            container.getChildren().add(rowContainer);
        });
    }
}
}
