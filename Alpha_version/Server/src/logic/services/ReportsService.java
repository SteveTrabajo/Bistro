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
 * Handles generation and caching of monthly reports.
 * Supports two report types: MEMBERS (reservation/waitlist stats) and TIMES (arrival timing data).
 * Reports are serialized and cached in the database to avoid regenerating them each time.
 */
public class ReportsService {
	
	/** Database controller for all DB operations */
    private final BistroDataBase_Controller db;
    
    /** Logger for tracking service activity */
    private final ServerLogger logger;

    /**
     * Creates a new ReportsService with required dependencies.
     * 
     * @param dbController database controller for DB access
     * @param logger server logger for logging events
     */
    public ReportsService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.db = dbController;
        this.logger = logger;
    }

    /**
     * Lists all months that have report data available for the given type.
     * 
     * @param reportType "MEMBERS" or "TIMES"
     * @return list of [year, month] pairs that have data
     */
    public List<int[]> listMonths(String reportType) {
        return db.listReportMonths(reportType);
    }

    /**
     * Gets an existing report from cache or generates a new one.
     * If force=true in the request, always regenerates even if cached.
     * 
     * @param req the report request containing type, year, month, and force flag
     * @return the generated or cached MonthlyReport
     * @throws IllegalArgumentException if request is invalid
     * @throws RuntimeException if report cannot be saved to database
     */
    public MonthlyReport getOrGenerate(ReportRequest req) {
        if (req == null) throw new IllegalArgumentException("ReportRequest is null");
        if (req.getMonth() < 1 || req.getMonth() > 12) throw new IllegalArgumentException("Invalid month");

        String type = req.getReportType();
        int year = req.getYear();
        int month = req.getMonth();

        if (!req.isForce()) {
            byte[] payload = db.getReportPayload(type, year, month);
            if (payload != null) {
                MonthlyReport stored = deserializeMonthlyReport(payload);
                if (stored != null) return stored;
            }
        }

        MonthlyReport generated = generate(type, year, month);
        byte[] bytes = serializeMonthlyReport(generated);

        boolean ok = db.upsertReportPayload(type, year, month, bytes);
        if (!ok) throw new RuntimeException("Failed to persist report");

        return generated;
    }

    /**
     * Generates a fresh report for the given type and time period.
     * MEMBERS report: reservation counts, waitlist joins by day.
     * TIMES report: late arrivals, on-time arrivals, overstay data.
     * 
     * @param type the report type
     * @param year the year
     * @param month the month (1-12)
     * @return the generated report with all data populated
     */
    private MonthlyReport generate(String type, int year, int month) {
        MonthlyReport r = new MonthlyReport();
        
        r.setReportType(type);
        r.setYearInt(year);
        r.setMonthInt(month);
        

        LocalDate firstDay = LocalDate.of(year, month, 1);

        r.setTotalReservations(db.getTotalReservation(firstDay));
        r.setTotalCostumer(db.getTotalCostumersInMonth(firstDay));
        r.setTotalLateCostumer(db.getTotalLateCostumersInMonth(firstDay));
        r.setTotalOnTimeCostumer(db.getTotalOntTimeCostumersInMonth(firstDay));
        r.setTotalMemberReservations(db.getTotalMembersReservationInMonth(firstDay));

        int totalRes = r.getTotalReservations();
        int memberRes = r.getTotalMemberReservations();

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
     * Serializes a MonthlyReport to bytes for database storage.
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
     * Deserializes a MonthlyReport from bytes.
     * Returns null if deserialization fails (e.g., schema changed).
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