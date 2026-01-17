package logic.services;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import entities.MonthlyReport;
import entities.ReportRequest;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class ReportsService {
    private final BistroDataBase_Controller db;
    private final ServerLogger logger;

    public ReportsService(BistroDataBase_Controller dbController, ServerLogger logger) {
        this.db = dbController;
        this.logger = logger;
    }

    public List<int[]> listMonths(String reportType) {
        return db.listReportMonths(reportType);
    }

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
