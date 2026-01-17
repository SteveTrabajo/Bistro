package entities;

import java.io.Serializable;

/**
 * reportType: "MEMBERS" or "TIMES"
 * force: regenerate even if exists in DB.
 * ReportRequest entity representing a request for generating a report.
 */
public class ReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

	private String reportType;
	private int year;
	private int month; // 1..12
	private boolean force;

	/*
	 * Creates a ReportRequest instance with default values.
	 */
	public ReportRequest() {
	}

	/*
	 * Creates a ReportRequest instance.
	 * @param reportType the type of report ("MEMBERS" or "TIMES")
	 * @param year       the year for the report
	 * @param month      the month for the report (1-12)
	 * @param force      whether to force regeneration of the report
	 */
	public ReportRequest(String reportType, int year, int month, boolean force) {
		this.reportType = reportType;
		this.year = year;
		this.month = month;
		this.force = force;
	}

	/*
	 * Gets the report type.
	 * @return the type of report ("MEMBERS" or "TIMES")
	 */
	public String getReportType() {
		return reportType;
	}

	/*
	 * Sets the report type.
	 * @param reportType the type of report ("MEMBERS" or "TIMES")
	 */
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	/*
	 * Gets the year for the report.
	 * @return the year
	 */
	public int getYear() {
		return year;
	}

	/*
	 * Sets the year for the report.
	 * @param year the year
	 */
	public void setYear(int year) {
		this.year = year;
	}

	/*
	 * Gets the month for the report.
	 * @return the month (1-12)
	 */
	public int getMonth() {
		return month;
	}

	/*
	 * Sets the month for the report.
	 * @param month the month (1-12)
	 */
	public void setMonth(int month) {
		this.month = month;
	}

	/*
	 * Checks whether to force regeneration of the report.
	 * @return true if regeneration is forced, false otherwise
	 */
	public boolean isForce() {
		return force;
	}

	/*
	 * Sets whether to force regeneration of the report.
	 * @param force true to force regeneration, false otherwise
	 */
	public void setForce(boolean force) {
		this.force = force;
	}
}
// end of ReportRequest.java