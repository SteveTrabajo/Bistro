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
	private int totalReservations; 
	private int totalCostumer; 	
	private int totalLateCostumer; 			
	private int totalOnTimeCostumer; 
	private int totalMemberReservations; 
	private int memberReservationRate; 	
	private String reportType = "MEMBERS"; 
	private int year;
	private int month; 					
	
	// Graph data
	private Map<Integer, Integer> reservationsByDay = new HashMap<>();
	private Map<Integer, Integer> waitlistByDay = new HashMap<>();

	private Map<Integer, Integer> lateArrivalsByDay = new HashMap<>();
	private Map<Integer, Integer> onTimeArrivalsByDay = new HashMap<>();
	private Map<String, Integer> latenessBuckets = new HashMap<>();
	private Map<String, Integer> overstayBuckets = new HashMap<>();



	//************************ Constructors ************************//
	
	/*
	 * Creates a MonthlyReport instance with the current month and year.
	 */
	public MonthlyReport() {
	    LocalDate now = LocalDate.now();
	    this.reportMonth = now.getMonth().name();
	    this.reportYear = String.valueOf(now.getYear());
	    setYearInt(now.getYear());
	    setMonthInt(now.getMonthValue());
	}
	
	//************************ Getters and Setters ************************//
	
	
	/*
	 * Gets the report type.
	 * @return the report type
	 */
	public String getReportType() {
		return reportType;
	}

	/*
	 * Sets the report type.
	 * @param reportType the report type
	 */
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	/*
	 * Gets the year as an integer.
	 * @return the year
	 */
	public int getYearInt() {
		return year;
	}

	/*
	 * Sets the year as an integer.
	 * @param year the year
	 */
	public void setYearInt(int year) {
		this.year = year;
		this.reportYear = String.valueOf(year);

	}

	/*
	 * Gets the month as an integer.
	 * @return the month
	 */
	public int getMonthInt() {
		return month;
	}

	/*
	 * Sets the month as an integer.
	 * @param month the month
	 */
	public void setMonthInt(int month) {
		this.month = month;
		this.reportMonth = java.time.Month.of(month).name();
	}

	/*
	 * Returns the number of reservations made for each day of the month.
	 *
	 * @return map dayOfMonth->numberOfReservations (never null)
	 */
	public Map<Integer, Integer> getReservationsByDay() {
		return reservationsByDay;
	}

	/*
	 * Sets the number of reservations made for each day of the month.
	 *
	 * @param reservationsByDay map dayOfMonth->numberOfReservations (null becomes empty map)
	 */
	public void setReservationsByDay(Map<Integer, Integer> reservationsByDay) {
		this.reservationsByDay = (reservationsByDay != null) ? reservationsByDay : new HashMap<>();
	}

	/*
	 * Returns the number of waitlisted customers for each day of the month.
	 *
	 * @return map dayOfMonth->numberOfWaitlistedCustomers (never null)
	 */
	public Map<Integer, Integer> getWaitlistByDay() {
		return waitlistByDay;
	}

	/*
	 * Sets the number of waitlisted customers for each day of the month.
	 *
	 * @param waitlistByDay map dayOfMonth->numberOfWaitlistedCustomers (null becomes empty map)
	 */
	public void setWaitlistByDay(Map<Integer, Integer> waitlistByDay) {
		this.waitlistByDay = (waitlistByDay != null) ? waitlistByDay : new HashMap<>();
	}

	/*
	 * Returns the number of late arrivals for each day of the month.
	 *
	 * @return map dayOfMonth->numberOfLateArrivals (never null)
	 */
	public Map<Integer, Integer> getLateArrivalsByDay() {
		return lateArrivalsByDay;
	}

	/*
	 * Sets the number of late arrivals for each day of the month.
	 *
	 * @param lateArrivalsByDay map dayOfMonth->numberOfLateArrivals (null becomes empty map)
	 */
	public void setLateArrivalsByDay(Map<Integer, Integer> lateArrivalsByDay) {
		this.lateArrivalsByDay = (lateArrivalsByDay != null) ? lateArrivalsByDay : new HashMap<>();
	}

	/*
	 * Returns the number of on-time arrivals for each day of the month.
	 *
	 * @return map dayOfMonth->numberOfOnTimeArrivals (never null)
	 */
	public Map<Integer, Integer> getOnTimeArrivalsByDay() {
		return onTimeArrivalsByDay;
	}

	/*
	 * Sets the number of on-time arrivals for each day of the month.
	 *
	 * @param onTimeArrivalsByDay map dayOfMonth->numberOfOnTimeArrivals (null becomes empty map)
	 */
	public void setOnTimeArrivalsByDay(Map<Integer, Integer> onTimeArrivalsByDay) {
		this.onTimeArrivalsByDay = (onTimeArrivalsByDay != null) ? onTimeArrivalsByDay : new HashMap<>();
	}

	/**
	 * Returns distribution buckets of lateness for late arrivals.
	 *
	 * @return map bucketLabel->count (never null)
	 */
	public Map<String, Integer> getLatenessBuckets() {
		return latenessBuckets;
	}

	/**
	 * Sets distribution buckets of lateness for late arrivals.
	 *
	 * @param latenessBuckets map bucketLabel->count (null becomes empty map)
	 */
	public void setLatenessBuckets(Map<String, Integer> latenessBuckets) {
		this.latenessBuckets = (latenessBuckets != null) ? latenessBuckets : new HashMap<>();
	}

	/**
	 * Returns distribution buckets of leaving delays (overstay) beyond the 2-hour
	 * slot.
	 *
	 * @return map bucketLabel->count (never null)
	 */
	public Map<String, Integer> getOverstayBuckets() {
		return overstayBuckets;
	}

	/**
	 * Sets distribution buckets of leaving delays (overstay) beyond the 2-hour
	 * slot.
	 *
	 * @param overstayBuckets map bucketLabel->count (null becomes empty map)
	 */
	public void setOverstayBuckets(Map<String, Integer> overstayBuckets) {
		this.overstayBuckets = (overstayBuckets != null) ? overstayBuckets : new HashMap<>();
	}

	/*
	 * Gets the total number of reservations.
	 * @return the totalReservations
	 */
	public int getTotalReservations() {
		return totalReservations;
	}

	/*
	 * Sets the total number of reservations.
	 * @param totalReservations the totalReservations to set
	 */
	public void setTotalReservations(int totalReservations) {
		this.totalReservations = totalReservations;
	}

	/*
	 * Gets the total number of customers.
	 * @return the totalCostumer
	 */
	public int getTotalCostumer() {
		return totalCostumer;
	}

	/*
	 * Sets the total number of customers.
	 * @param totalCostumer the totalCostumer to set
	 */
	public void setTotalCostumer(int totalCostumer) {
		this.totalCostumer = totalCostumer;
	}

	/*
	 * Gets the total number of late customers.
	 * @return the totalLateCostumer
	 */
	public int getTotalLateCostumer() {
		return totalLateCostumer;
	}

	/*
	 * Sets the total number of late customers.
	 * @param totalLateCostumer the totalLateCostumer to set
	 */
	public void setTotalLateCostumer(int totalLateCostumer) {
		this.totalLateCostumer = totalLateCostumer;
	}

	/*
	 * Gets the total number of on-time customers.
	 * @return the totalOnTimeCostumer
	 */
	public int getTotalOnTimeCostumer() {
		return totalOnTimeCostumer;
	}

	/*
	 * Sets the total number of on-time customers.
	 * @param totalOnTimeCostumer the totalOnTimeCostumer to set
	 */
	public void setTotalOnTimeCostumer(int totalOnTimeCostumer) {
		this.totalOnTimeCostumer = totalOnTimeCostumer;
	}

	/*
	 * Gets the total number of member reservations.
	 * @return the totalMemberReservations
	 */
	public int getTotalMemberReservations() {
		return totalMemberReservations;
	}

	/*
	 * Sets the total number of member reservations.
	 * @param totalMemberReservations the totalMemberReservations to set
	 */
	public void setTotalMemberReservations(int totalMemberReservations) {
		this.totalMemberReservations = totalMemberReservations;
	}

	/*
	 * Gets the member reservation percentage.
	 * @return the memberReservationRate
	 */
	public int getMemberReservationPrecetage() {
		return memberReservationRate;
	}

	/*
	 * Sets the member reservation percentage.
	 * @param memberReservationRate the memberReservationRate to set
	 */
	public void setMemberReservationPrecetage(int memberReservationRate) {
		this.memberReservationRate = memberReservationRate;
	}

	/*
	 * Calculates and returns the on-time arrival rate as a percentage.
	 * @return the on-time arrival rate percentage
	 */
	public int getOnTimeRate() {
		if (totalCostumer == 0) {
			return 0;
		}
		return (int) Math.round(((double) totalOnTimeCostumer / totalCostumer) * 100);
	}

	/*
	 * Gets the report year as a string.
	 * @return the reportYear
	 */
	public String getYear() {
		return this.reportYear;
	}

	/*
	 * Gets the report month as a string.
	 * @return the reportMonth
	 */
	public String getMonth() {
		return this.reportMonth;
	}
}
// End of MonthlyReport.java