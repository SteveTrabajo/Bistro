package logic.api.subjects;

import java.util.List;

import entities.Bill;
import entities.Item;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import logic.PaymentController.PaymentStatus;
import logic.api.ClientRouter;

public class ClientPaymentSubject {

	public static void register(ClientRouter router) {
		
		
		router.on("payment", "billItemsList.ok", msg -> {
			BistroClient.awaitResponse = false;
			List<Item> items =(List<Item>) msg.getData();
			if(items != null) {
				BistroClientGUI.client.getPaymentCTRL().setBillItemsList(items);
			}
			else {
				BistroClientGUI.client.getPaymentCTRL().setBillItemsList(null);
			}
		});
		
		router.on("payment", "billItemsList.fail", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getPaymentCTRL().setBillItemsList(null);
		});
		
		
		
		router.on("payment", "complete.ok", msg -> {
            BistroClient.awaitResponse = false;
            BistroClientGUI.client.getPaymentCTRL().clearPaymentController();
			BistroClientGUI.client.getTableCTRL().clearTableController();
			Platform.runLater(()->{
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Payment Successful");
				alert.setHeaderText(null);
				alert.setContentText((String)msg.getData());
				alert.showAndWait();
				BistroClientGUI.switchScreen("clientDashboardScreen", "Failed to load Client Dashboard Screen");
				});
		});
		
		router.on("payment", "complete.fail", msg -> {
            BistroClient.awaitResponse = false;
			BistroClientGUI.client.getPaymentCTRL().setPaymentStatus(PaymentStatus.FAILED.name());
			Platform.runLater(()->{
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Payment Failed");
				alert.setHeaderText(null);
				alert.setContentText("Payment processing failed. Please try again.");
				alert.showAndWait();
				});
		});
		
		router.on("payment", "processManually.ok", msg -> {
            BistroClient.awaitResponse = false;
			// Handle successful manual payment processing
			BistroClientGUI.client.getPaymentCTRL().setIsPaymentManuallySuccessful(true);
			BistroClientGUI.client.getTableCTRL().clearCurrentTable();
		});
		router.on("payment", "processManually.fail", msg -> {
            BistroClient.awaitResponse = false;
		});
		router.on("payment", "getPendingBills.ok", msg -> {
            BistroClient.awaitResponse = false;
			BistroClientGUI.client.getPaymentCTRL().setPendingBills((List<Bill>) msg.getData());
		});
		router.on("payment", "getPendingBills.fail", msg -> {
            BistroClient.awaitResponse = false;
		});
	}
	
}
