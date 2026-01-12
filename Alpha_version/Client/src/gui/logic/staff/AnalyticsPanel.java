package gui.logic.staff;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import entities.MonthlyReport;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import logic.BistroClientGUI;

public class AnalyticsPanel {
	//*********************** FXML Variables ************************//
    @FXML 
    private Label totalReservationsLabel;
    
    @FXML 
    private Label totalReservationsDelta;
    
    @FXML 
    private Label rateOfMemberInReservationLabel;
            
    @FXML 
    private Label onTimeRateLabel;
    
    @FXML 
    private Label onTimeDelta;
    
    @FXML 
    private Label customersThisMonthLabel;
    
    @FXML 
    private Label currentMonthLabel;

    // ****** Chart Arrival Times ******
    @FXML private BarChart<String, Number> arrivalBarChart;
    @FXML private Label totalOnTimeLabel;
    @FXML private Label totalLateLabel;

    // ****** Chart Monthly Trends ******
    @FXML private LineChart<String, Number> reservationsLineChart;
    @FXML private Label peakMonthLabel;
    @FXML private Label peakMonthValueLabel;
    @FXML private Label lowestMonthLabel;
    @FXML private Label lowestMonthValueLabel;
    @FXML private Label growthRateLabel;

    // ****** Dynamic Bottom Sections ******
    @FXML private VBox peakTimesBox;
    @FXML private VBox partySizeBox;
    
    //*********************** FXML Methods ************************//
    
    public void initialize() {
        // Initial setup if needed (e.g., clear charts)
        arrivalBarChart.setAnimated(true);
        reservationsLineChart.setAnimated(true);
        BistroClientGUI.client.getMonthlyReportsCTRL().requestMonthlyReportData();
        Platform.runLater(() -> {
        	updateDashboard(BistroClientGUI.client.getMonthlyReportsCTRL().getCurrentMonthlyReport());
        });
    }

    /**
     * Updates the entire dashboard with new data from MonthlyReport
     * @param data The MonthlyReport 
     */
    public void updateDashboard(MonthlyReport data) {
        if (data == null) return;

        // 1. Update KPIs 
        totalReservationsLabel.setText(String.valueOf(data.getTotalReservations()));
        // Assuming getter for delta exists in DTO
        //setDeltaLabel(totalReservationsDelta, 5.2); // Example value, replace with data.get...

        rateOfMemberInReservationLabel.setText(String.valueOf(data.getMemberReservationPrecetage()) + "%");
        //setDeltaLabel(avgMonthlyDelta, 2.1);

        onTimeRateLabel.setText(String.format("%.1f%%", 0.52 * 100));
        //setDeltaLabel(onTimeDelta, -0.5);

        customersThisMonthLabel.setText(String.valueOf(data.getTotalCostumer()));      
        currentMonthLabel.setText(data.getMonth()); 

        // 2. Update Bar Chart (Arrival Times)
        updateArrivalChart(data);

        // 3. Update Line Chart (Trends)
        updateTrendChart(data);

        // 4. Update Summaries
        totalOnTimeLabel.setText(String.valueOf(data.getTotalOnTimeCostumer()));
        totalLateLabel.setText(String.valueOf(data.getTotalLateCostumer()));
        
//        data.getPeakMonth()); 
//        peakMonthValueLabel.setText(String.valueOf(data.getPeakMonthValue())); 
//        
//        lowestMonthLabel.setText(data.getLowestMonth()); 
//        lowestMonthValueLabel.setText(String.valueOf(data.getLowestMonthValue())); 
//        
//        growthRateLabel.setText(String.format("%+.1f%%", data.getGrowthRateYearly())); 

//        // 5. Build Dynamic Bottom Rows
//        populateDistributionRows(peakTimesBox, data.getPeakReservationTimes(), "ad-bar-blue");
//        populateDistributionRows(partySizeBox, data.getDinersAmountDistribution(), "ad-bar-green");
    }

    /**
	 * Helper to set delta label text and color based on value
	 * @param label The Label to update
	 * @param value The delta value (positive/negative)
	 */
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

    /**
	 * Updates the arrival times bar chart with new data
	 * @param data The MonthlyReport DTO
	 */
    private void updateArrivalChart(MonthlyReport data) {
        arrivalBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Arrivals");

//        Map<String, Integer> dist = data.getArrivalTimeDistribution();
//        if (dist != null) {
//            dist.forEach((category, count) -> {
//                series.getData().add(new XYChart.Data<>(category, count));
//            });
//        }
//        arrivalBarChart.getData().add(series);
    }

    /**
     * Updates the monthly reservations trend line chart
     * @param data
     */
    private void updateTrendChart(MonthlyReport data) {
        reservationsLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(data.getYear());

//        Map<String, Integer> trends = data.getMonthlyReservationsMap();
//        // Ensure month order is correct (Logic depends on Map implementation, LinkedHashMap is best)
//        if (trends != null) {
//            trends.forEach((month, count) -> {
//                series.getData().add(new XYChart.Data<>(month, count));
//            });
//        }
//        reservationsLineChart.getData().add(series);
    }


	/**
     * Dynamically creates progress bar rows for the bottom cards
     * @param container The VBox container to populate
     * @param dataMap The data map to visualize (key: label, value: count)
     * @param colorStyleClass The CSS style class for the progress bar color
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
