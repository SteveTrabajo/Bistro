package logic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import comms.Api;
import comms.Message;
import entities.MonthlyReport;
import entities.ReportRequest;

public class MonthlyReportsController {
	
	/** Available months returned from the server as pairs {@code [year, month]}. */
	private List<int[]> availableMonths = new ArrayList<>();
	
	private final BistroClient client;
	
	private MonthlyReport currentMonthlyReport;
	
	public MonthlyReportsController(BistroClient bistroClient) {
		this.client = bistroClient;
	}
	
	public MonthlyReport getCurrentMonthlyReport() {
		return currentMonthlyReport;
	}
	
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


	public void requestReport(String type, int year, int month, boolean force) {
	    ReportRequest req = new ReportRequest(type, year, month, force);
	    BistroClientGUI.client.handleMessageFromClientUI(
	        new Message(Api.ASK_REPORTS_GET_OR_GENERATE, req)
	    );
	}

}
