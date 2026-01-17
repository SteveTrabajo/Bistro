package logic.api.subjects;

import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.api.ClientRouter;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;

import entities.MonthlyReport;

public class ClientMonthlyReportsSubject {

    private ClientMonthlyReportsSubject() {}

    public static void register(ClientRouter router) {

        // New pipeline: reports.getOrGenerate.ok
        router.on("reports", "getOrGenerate.ok", msg -> {
            MonthlyReport report = (MonthlyReport) msg.getData();
            BistroClientGUI.client.getMonthlyReportsCTRL().setCurrentMonthlyReport(report);
            BistroClient.awaitResponse = false; // unblock waiting thread
        });

        // reports.getOrGenerate.fail
        router.on("reports", "getOrGenerate.fail", msg -> {
        	BistroClient.awaitResponse = false;

            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Reports Failed");
                a.setHeaderText("Could not list report months");
                a.setContentText(String.valueOf(msg.getData()));
                a.showAndWait();
            });
        });
        
        router.on("reports", "listMonths.ok", msg -> {
            @SuppressWarnings("unchecked")
            List<int[]> months = (List<int[]>) msg.getData();

            BistroClientGUI.client.getMonthlyReportsCTRL().setAvailableMonths(months);
            BistroClient.awaitResponse = false;
        });

        router.on("reports", "listMonths.fail", msg -> {
            BistroClient.awaitResponse = false;

            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Reports Failed");
                a.setHeaderText("Could not list report months");
                a.setContentText(String.valueOf(msg.getData()));
                a.showAndWait();
            });
        });
    }
}
