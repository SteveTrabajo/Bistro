package entities;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object for carrying analytics data from Server to Client.
 */
public class MonthlyReport implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- KPI Cards Data ---
    private int totalReservations2025;
    private double totalReservationsDelta; // e.g., 5.2 (percent)

    private int avgMonthlyReservations;
    private double avgMonthlyDelta;

    private double onTimeArrivalRate; // e.g., 0.85 for 85%
    private double onTimeArrivalDelta;

    private int customersThisMonth;
    private String currentMonthName;

    // --- Chart 1: Arrival Times (Bar Chart) ---
    // Key: Category (e.g., "On Time", "Late < 15m", "Late > 15m"), Value: Count
    private Map<String, Integer> arrivalTimeDistribution;
    private int totalOnTime;
    private int totalLate;

    // --- Chart 2: Monthly Trends (Line Chart) ---
    // Key: Month Name, Value: Count
    private Map<String, Integer> monthlyReservationsMap;
    
    // --- Summary Box Data ---
    private String peakMonth;
    private int peakMonthValue;
    private String lowestMonth;
    private int lowestMonthValue;
    private double growthRateYearly;

    // --- Bottom Cards: Distributions ---
    // Key: Hour (e.g., "18:00"), Value: Count
    private Map<String, Integer> peakReservationTimes;
    // Key: Party Size (e.g., "2 Guests"), Value: Count
    private Map<String, Integer> partySizeDistribution;

    // Constructor, Getters, and Setters
    public MonthlyReportData() {}

    // Generate Getters and Setters for all fields...
    public int getTotalReservations2025() { return totalReservations2025; }
    public void setTotalReservations2025(int totalReservations2025) { this.totalReservations2025 = totalReservations2025; }
    // ... (המשך ליצור את כולם) ...
    
    public Map<String, Integer> getArrivalTimeDistribution() { return arrivalTimeDistribution; }
    public void setArrivalTimeDistribution(Map<String, Integer> arrivalTimeDistribution) { this.arrivalTimeDistribution = arrivalTimeDistribution; }
    
    public Map<String, Integer> getMonthlyReservationsMap() { return monthlyReservationsMap; }
    public void setMonthlyReservationsMap(Map<String, Integer> monthlyReservationsMap) { this.monthlyReservationsMap = monthlyReservationsMap; }
    
    public Map<String, Integer> getPeakReservationTimes() { return peakReservationTimes; }
    public void setPeakReservationTimes(Map<String, Integer> peakReservationTimes) { this.peakReservationTimes = peakReservationTimes; }
    
    public Map<String, Integer> getPartySizeDistribution() { return partySizeDistribution; }
    public void setPartySizeDistribution(Map<String, Integer> partySizeDistribution) { this.partySizeDistribution = partySizeDistribution; }

    // Helpers for specific fields
    public double getOnTimeArrivalRate() { return onTimeArrivalRate; }
    public void setOnTimeArrivalRate(double onTimeArrivalRate) { this.onTimeArrivalRate = onTimeArrivalRate; }
    
    public int getTotalOnTime() { return totalOnTime; }
    public void setTotalOnTime(int totalOnTime) { this.totalOnTime = totalOnTime; }
    
    public int getTotalLate() { return totalLate; }
    public void setTotalLate(int totalLate) { this.totalLate = totalLate; }
    
    // Add remaining getters/setters as needed
}