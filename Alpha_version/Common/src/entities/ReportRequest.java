package entities;

import java.io.Serializable;

/**
 * reportType: "MEMBERS" or "TIMES"
 * force: regenerate even if exists in DB.
 */
public class ReportRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reportType;
    private int year;
    private int month; // 1..12
    private boolean force;

    public ReportRequest() {}

    public ReportRequest(String reportType, int year, int month, boolean force) {
        this.reportType = reportType;
        this.year = year;
        this.month = month;
        this.force = force;
    }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public boolean isForce() { return force; }
    public void setForce(boolean force) { this.force = force; }
}
