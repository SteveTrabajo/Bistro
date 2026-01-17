package logic.services;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import entities.MonthlyReport;
import entities.ReportRequest;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

/**
 * Service for generating and retrieving monthly reports.
 */
public class ReportsService {
    private final BistroDataBase_Controller db;
    private final ServerLogger logger;

    /**
	 * Constructs a ReportsService with the given database controller and logger.
	 *
	 * @param dbController The database controller to use for data access.
	 * @param logger       The logger for logging operations.
	 */
    public ReportsService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.db = dbController;
        this.logger = logger;
    }

    /**
	 * Lists available months for the given report type.
	 *
	 * @param reportType The type of report.
	 * @return A list of int arrays, each containing [year, month].
	 */
    public List<int[]> listMonths(String reportType) {
        return db.listReportMonths(reportType);
    }

    /**
     * Gets an existing report or generates a new one if not found or forced.
     * @param req The report request containing type, year, month, and force flag.
     * @return The monthly report.
     */
    public MonthlyReport getOrGenerate(ReportRequest req) {
        if (req == null) throw new IllegalArgumentException("ReportRequest is null");
        if (req.getMonth() < 1 || req.getMonth() > 12) throw new IllegalArgumentException("Invalid month");
        // Check year validity (e.g., between 2000 and current year)
        String type = req.getReportType();
        int year = req.getYear();
        int month = req.getMonth();
        // Try to get existing report
        if (!req.isForce()) {
            byte[] payload = db.getReportPayload(type, year, month);
            if (payload != null) {
                MonthlyReport stored = deserializeMonthlyReport(payload);
                if (stored != null) return stored;
            }
        }
		// Generate new report
        MonthlyReport generated = generate(type, year, month);
        byte[] bytes = serializeMonthlyReport(generated);
        // Persist the generated report
        boolean ok = db.upsertReportPayload(type, year, month, bytes);
        if (!ok) throw new RuntimeException("Failed to persist report");

        return generated;
    }

    /**
	 * Generates a monthly report based on the type, year, and month.
	 *
	 * @param type  The type of report (e.g., "MEMBERS", "TIMES").
	 * @param year  The year for the report.
	 * @param month The month for the report.
	 * @return The generated MonthlyReport.
	 */
    private MonthlyReport generate(String type, int year, int month) {
        MonthlyReport r = new MonthlyReport();
        // Set basic info
        r.setReportType(type);
        r.setYearInt(year);
        r.setMonthInt(month);
        
        // Set summary statistics
        LocalDate firstDay = LocalDate.of(year, month, 1);
        
        // Assuming db methods handle month boundaries correctly
        r.setTotalReservations(db.getTotalReservation(firstDay));
        r.setTotalCostumer(db.getTotalCostumersInMonth(firstDay));
        r.setTotalLateCostumer(db.getTotalLateCostumersInMonth(firstDay));
        r.setTotalOnTimeCostumer(db.getTotalOntTimeCostumersInMonth(firstDay));
        r.setTotalMemberReservations(db.getTotalMembersReservationInMonth(firstDay));

        // Calculate member reservation percentage
        int totalRes = r.getTotalReservations();
        int memberRes = r.getTotalMemberReservations();

        // Avoid division by zero
        int pct = 0;
        if (totalRes > 0) {
            pct = (int) Math.round((memberRes * 100.0) / totalRes);
        }
        r.setMemberReservationPrecetage(pct);


        // Fill graphs depending on report type
        if ("MEMBERS".equalsIgnoreCase(type)) {
            Map<Integer, Integer> resByDay = db.getReservationsByDay(year, month);
            Map<Integer, Integer> wlByDay = db.getWaitlistJoinsByDay(year, month);
            r.setReservationsByDay(resByDay);
            r.setWaitlistByDay(wlByDay);
        } else if ("TIMES".equalsIgnoreCase(type)) {
            r.setLateArrivalsByDay(db.getLateArrivalsByDay(year, month));
            r.setOnTimeArrivalsByDay(db.getOnTimeArrivalsByDay(year, month));
            r.setLatenessBuckets(db.getLatenessBuckets(year, month));
            r.setOverstayBuckets(db.getOverstayBuckets(year, month));
        } else {
            throw new IllegalArgumentException("Unknown report type: " + type);
        }
        
        
        return r;
    }

    /**
     * Serializes a MonthlyReport to a byte array.
     * @param r The MonthlyReport to serialize.
     * @return The serialized byte array.
     */
    private static byte[] serializeMonthlyReport(MonthlyReport r) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(r);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Serialize report failed", e);
        }
    }

    /**
	 * Deserializes a MonthlyReport from a byte array.
	 * @param bytes The byte array to deserialize.
	 * @return The deserialized MonthlyReport, or null if deserialization fails.
	 */
    private static MonthlyReport deserializeMonthlyReport(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            Object o = in.readObject();
            return (MonthlyReport) o;
        } catch (Exception e) {
            return null; // if schema changed, regenerate
        }
    }
}
