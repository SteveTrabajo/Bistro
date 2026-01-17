package logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import comms.Api;
import comms.Message;
import entities.MonthlyReport;
import entities.ReportRequest;

/**
 * Controller class for managing monthly reports in the Bistro client GUI.
 */
public class MonthlyReportsController {
	
	//******************************* Instance variables ******************************
	private List<int[]> availableMonths = new ArrayList<>();
	
	private final BistroClient client;
	
	private MonthlyReport currentMonthlyReport;
	
	//******************************** Constructor ***********************************
	
	/**
	 * Constructs a MonthlyReportsController with the specified BistroClient.
	 *
	 * @param bistroClient the BistroClient for server communication
	 */
	public MonthlyReportsController(BistroClient bistroClient) {
		this.client = bistroClient;
	}
	
	
	//******************************** Getters & Setters *****************************
	
	/**
	 * Returns the current monthly report.
	 *
	 * @return the current MonthlyReport
	 */
	public MonthlyReport getCurrentMonthlyReport() {
		return currentMonthlyReport;
	}
	
	/**
	 * Sets the current monthly report.
	 *
	 * @param currentMonthlyReport the MonthlyReport to set
	 */
	public void setCurrentMonthlyReport(MonthlyReport currentMonthlyReport) {
		this.currentMonthlyReport = currentMonthlyReport;
	}

	/**
	 * Returns the available report months for the currently selected report type.
	 *
	 * @return list of pairs {@code int[]{year, month}}
	 */
	public List<int[]> getAvailableMonths() {
	    return availableMonths;
	}

	/**
	 * Replaces the available month list.
	 *
	 * @param months list of pairs {@code int[]{year, month}} (null becomes empty list)
	 */
	public void setAvailableMonths(List<int[]> months) {
	    this.availableMonths = (months != null) ? months : new ArrayList<>();
	}
	
	//********************************** Methods *************************************
	
	/**
	 * Requests the monthly report data for the current month.
	 */
	public void requestMonthlyReportData() {
	    LocalDate now = LocalDate.now();
	    requestReport("MEMBERS", now.getYear(), now.getMonthValue(), false);
	}
	
	/**
	 * Requests the list of available months for the given report type.
	 *
	 * @param type report type (expected: "MEMBERS" or "TIMES")
	 */
	public void requestAvailableMonths(String type) {
	    BistroClientGUI.client.handleMessageFromClientUI(
	        new Message(Api.ASK_REPORTS_LIST_MONTHS, type)
	    );
	}

	/**
	 * Requests a report of the specified type for the given year and month.
	 *
	 * @param type  report type (expected: "MEMBERS" or "TIMES")
	 * @param year  the year of the report
	 * @param month the month of the report
	 * @param force if true, forces regeneration of the report even if it exists
	 */
	public void requestReport(String type, int year, int month, boolean force) {
	    ReportRequest req = new ReportRequest(type, year, month, force);
	    BistroClientGUI.client.handleMessageFromClientUI(
	        new Message(Api.ASK_REPORTS_GET_OR_GENERATE, req)
	    );
	}
}
// End of MonthlyReportsController.java