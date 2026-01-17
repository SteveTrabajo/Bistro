package logic.api.subjects;

import java.util.List;

import comms.Api;
import comms.Message;
import entities.MonthlyReport;
import entities.ReportRequest;
import entities.User;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.ReportsService;

public class ServerReportsSubject {

    private ServerReportsSubject() {}

    public static void register(ServerRouter router, ReportsService reportsService, ServerLogger logger) {

        router.on("reports", "listMonths", (msg, client) -> {
            User sessionUser = (User) client.getInfo("user");
            if (sessionUser == null) {
                client.sendToClient(new Message(Api.REPLY_REPORTS_LIST_MONTHS_FAIL, "Not logged in"));
                return;
            }

            String reportType = (String) msg.getData(); // "MEMBERS" | "TIMES"
            try {
                List<int[]> months = reportsService.listMonths(reportType);
                client.sendToClient(new Message(Api.REPLY_REPORTS_LIST_MONTHS_OK, months));
            } catch (Exception e) {
                client.sendToClient(new Message(Api.REPLY_REPORTS_LIST_MONTHS_FAIL, e.getMessage()));
            }
        });

        router.on("reports", "getOrGenerate", (msg, client) -> {
            User sessionUser = (User) client.getInfo("user");
            if (sessionUser == null) {
                client.sendToClient(new Message(Api.REPLY_REPORTS_GET_OR_GENERATE_FAIL, "Not logged in"));
                return;
            }

            ReportRequest req = (ReportRequest) msg.getData();
            try {
                MonthlyReport report = reportsService.getOrGenerate(req);
                client.sendToClient(new Message(Api.REPLY_REPORTS_GET_OR_GENERATE_OK, report));
            } catch (Exception e) {
                client.sendToClient(new Message(Api.REPLY_REPORTS_GET_OR_GENERATE_FAIL, e.getMessage()));
            }
        });
    }
}
