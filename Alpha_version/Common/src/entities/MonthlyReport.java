package entities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;


/**
 * MonthlyReport entity representing the data structure for monthly reservation reports.
 */
public class MonthlyReport implements Serializable {
    private static final long serialVersionUID = 1L;
    //************************ Instance Variables ************************//
    private String reportYear;
    private String reportMonth;
	private int totalReservations; 				//total Reservation in this month
	private int totalCostumer; 					//total Costumer in this month
	private int totalLateCostumer; 				//total late Costumer in this month
	private int totalOnTimeCostumer; 			//total on time Costumer in this month
	private int totalMemberReservations; 		//total Member Reservation in this month
	private int memberReservationRate; 			//Member Reservation Rate in this month


	//************************ Constructors ************************//
	
	public MonthlyReport() {
		this.reportMonth = String.valueOf(LocalDate.now().getMonth().name());
		this.reportYear = String.valueOf(LocalDate.now().getYear());
	}
	
	//************************ Getters and Setters ************************//
	
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