package entities;

import java.io.Serializable;
import java.util.Map;

/**
 * MonthlyReport entity representing the data structure for monthly reservation reports.
 */
public class MonthlyReport implements Serializable {
    private static final long serialVersionUID = 1L;
    //************************ Instance Variables ************************//
    private String reportYear;
    private String reportMonth;
	private int totalReservations; // can be for given year to compare year over year


	//************************ Constructors ************************//
	
	public MonthlyReport() {
	}
	
	//************************ Getters and Setters ************************//
	
	public int getTotalReservations() {
		return totalReservations;
	}

	public void setTotalReservations(int totalReservations) {
		this.totalReservations = totalReservations;
	}	

	public String getYear() {
		return this.reportYear;
	}
	
	public String getMonth() {
		return this.reportMonth;
	}
}