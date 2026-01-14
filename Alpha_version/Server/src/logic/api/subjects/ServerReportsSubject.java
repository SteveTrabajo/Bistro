package logic.api.subjects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import comms.Api;
import comms.Message;
import entities.User;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.ReportsService;

import entities.MonthlyReport;

public class ServerReportsSubject {
	
	private ServerReportsSubject() {}
	
	public static void register(ServerRouter router, ReportsService reportsService, ServerLogger logger) {
		
		router.on("monthlyReports", "getData", (msg, client) -> {
			User sessionUser = (User) client.getInfo("user");
			if (sessionUser == null) {
				logger.log("[SECURITY] Unauthorized monthlyReports attempt from " + client);
				client.sendToClient(new Message(Api.REPLY_MONTHLY_REPORT_DATA_FAIL, "Unauthorized"));
				return;
			}
			
			try {
				LocalDate date = LocalDate.now(); // TODO: maybe get it from the client msg???
				MonthlyReport monthlyReports = reportsService.getMontlyReport(date);
				
				if (monthlyReports == null) {
					client.sendToClient(new Message(Api.REPLY_MONTHLY_REPORT_DATA_FAIL, null));
				}
				
	            client.sendToClient(new Message(Api.REPLY_MONTHLY_REPORT_DATA_OK, monthlyReports));
			}
			catch (Exception e) {
				client.sendToClient(new Message(Api.REPLY_MONTHLY_REPORT_DATA_FAIL, null));
			}

//			@SuppressWarnings("unchecked")
		
		});
	}
}
