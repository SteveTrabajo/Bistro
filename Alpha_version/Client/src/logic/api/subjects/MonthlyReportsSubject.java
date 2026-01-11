package logic.api.subjects;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;
import java.util.List;

import entities.MonthlyReport;

public class MonthlyReportsSubject {
	
	private MonthlyReportsSubject() {}
	
	public static void register(ClientRouter router) {
		// Handler for new reservation creation messages
		router.on("monthlyReports", "getData.ok", msg -> {
            BistroClient.awaitResponse = false;
            MonthlyReport report = (MonthlyReport) msg.getData();
			Platform.runLater(() -> BistroClientGUI.client.getMonthlyReportsCTRL().setCurrentMonthlyReport(report));
		});
		
		router.on("monthlyReports", "getData.fail", msg -> {
			BistroClient.awaitResponse = false;
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("MonthlyReports Failed");
			alert.setHeaderText("Could not create MonthlyReports");
			alert.setContentText("An error occurred while creating your MonthlyReports. Please try again later.");
		});
	}
	
}
