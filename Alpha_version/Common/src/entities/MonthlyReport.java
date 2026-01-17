package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;


/**
 * MonthlyReport entity representing the data structure for monthly reservation reports.
 */
public class MonthlyReport implements Serializable {
    private static final long serialVersionUID = 1L;
    //************************ Instance Variables ************************//
    private String reportYear;
    private String reportMonth;
	private int totalReservations; 				// Total Reservation in this month
	private int totalCostumer; 					// Total Costumer in this month
	private int totalLateCostumer; 				// Total late Costumer in this month
	private int totalOnTimeCostumer; 			// Total on time Costumer in this month
	private int totalMemberReservations; 		// Total Member Reservation in this month
	private int memberReservationRate; 			// Member Reservation Rate in this month
	private String reportType = "MEMBERS"; 		// "MEMBERS" | "TIMES"
	private int year;
	private int month; 							// 1..12
	
	// Graph data
	private Map<Integer, Integer> reservationsByDay = new HashMap<>();
	private Map<Integer, Integer> waitlistByDay = new HashMap<>();

	private Map<Integer, Integer> lateArrivalsByDay = new HashMap<>();
	private Map<Integer, Integer> onTimeArrivalsByDay = new HashMap<>();
	private Map<String, Integer> latenessBuckets = new HashMap<>();
	private Map<String, Integer> overstayBuckets = new HashMap<>();



	//************************ Constructors ************************//
	
	public MonthlyReport() {
	    LocalDate now = LocalDate.now();
	    this.reportMonth = now.getMonth().name();
	    this.reportYear = String.valueOf(now.getYear());
	    setYearInt(now.getYear());
	    setMonthInt(now.getMonthValue());
	}
	
	//************************ Getters and Setters ************************//
	
	public String getReportType() { return reportType; }
	public void setReportType(String reportType) { this.reportType = reportType; }

	public int getYearInt() { return year; }
	
	public void setYearInt(int year) {
	    this.year = year;
	    this.reportYear = String.valueOf(year);
	    
	}

	public int getMonthInt() { return month; }
	
	public void setMonthInt(int month) {
	    this.month = month;
	    this.reportMonth = java.time.Month.of(month).name();
	}

	public Map<Integer, Integer> getReservationsByDay() { return reservationsByDay; }
	public void setReservationsByDay(Map<Integer, Integer> reservationsByDay) {
	    this.reservationsByDay = (reservationsByDay != null) ? reservationsByDay : new HashMap<>();
	}

	public Map<Integer, Integer> getWaitlistByDay() { return waitlistByDay; }
	public void setWaitlistByDay(Map<Integer, Integer> waitlistByDay) {
	    this.waitlistByDay = (waitlistByDay != null) ? waitlistByDay : new HashMap<>();
	}

	public Map<Integer, Integer> getLateArrivalsByDay() { return lateArrivalsByDay; }
	public void setLateArrivalsByDay(Map<Integer, Integer> lateArrivalsByDay) {
	    this.lateArrivalsByDay = (lateArrivalsByDay != null) ? lateArrivalsByDay : new HashMap<>();
	}

	public Map<Integer, Integer> getOnTimeArrivalsByDay() { return onTimeArrivalsByDay; }
	public void setOnTimeArrivalsByDay(Map<Integer, Integer> onTimeArrivalsByDay) {
	    this.onTimeArrivalsByDay = (onTimeArrivalsByDay != null) ? onTimeArrivalsByDay : new HashMap<>();
	}

	public Map<String, Integer> getLatenessBuckets() { return latenessBuckets; }
	public void setLatenessBuckets(Map<String, Integer> latenessBuckets) {
	    this.latenessBuckets = (latenessBuckets != null) ? latenessBuckets : new HashMap<>();
	}
	
	/**
	 * Returns distribution buckets of leaving delays (overstay) beyond the 2-hour slot.
	 *
	 * @return map bucketLabel->count (never null)
	 */
	public Map<String, Integer> getOverstayBuckets() {
	    return overstayBuckets;
	}

	/**
	 * Sets distribution buckets of leaving delays (overstay) beyond the 2-hour slot.
	 *
	 * @param overstayBuckets map bucketLabel->count (null becomes empty map)
	 */
	public void setOverstayBuckets(Map<String, Integer> overstayBuckets) {
	    this.overstayBuckets = (overstayBuckets != null) ? overstayBuckets : new HashMap<>();
	}


	public int getTotalReservations() {
		return totalReservations;
	}

	public void setTotalReservations(int totalReservations) {
		this.totalReservations = totalReservations;
	}	
	
	public int getTotalCostumer() {
		return totalCostumer;
	}

	public void setTotalCostumer(int totalCostumer) {
		this.totalCostumer = totalCostumer;
	}	
	
	public int getTotalLateCostumer() {
		return totalLateCostumer;
	}

	public void setTotalLateCostumer(int totalLateCostumer) {
		this.totalLateCostumer = totalLateCostumer;
	}	
	
	public int getTotalOnTimeCostumer() {
		return totalOnTimeCostumer;
	}

	public void setTotalOnTimeCostumer(int totalOnTimeCostumer) {
		this.totalOnTimeCostumer = totalOnTimeCostumer;
	}
	
	public int getTotalMemberReservations() {
		return totalMemberReservations;
	}

	public void setTotalMemberReservations(int totalMemberReservations) {
		this.totalMemberReservations = totalMemberReservations;
	}
	
	public int getMemberReservationPrecetage() {
		return memberReservationRate;
	}

	public void setMemberReservationPrecetage(int memberReservationRate) {
		this.memberReservationRate = memberReservationRate;
	}
	
	public int getOnTimeRate() {
		if (totalCostumer == 0) {
			return 0;
		}
		return	(int) Math.round(((double)totalOnTimeCostumer / totalCostumer) * 100);
	}
	
	public String getYear() {
		return this.reportYear;
	}
	
	public String getMonth() {
		return this.reportMonth;
	}
}