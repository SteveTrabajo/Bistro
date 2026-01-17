package logic.api.subjects;

import java.util.List;

import entities.Bill;
import entities.Item;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import logic.BistroClient;
import logic.BistroClientGUI;
import enums.PaymentStatus;
import logic.api.ClientRouter;

/**
 * ClientPaymentSubject is responsible for handling payment-related messages
 * received from the server and updating the client GUI accordingly.
 */
public class ClientPaymentSubject {
	
	/**
	 * Registers payment-related message handlers with the provided ClientRouter.
	 *
	 * @param router The ClientRouter to register message handlers with.
	 */ 
	public static void register(ClientRouter router) {
		
		// Handler for successful retrieval of bill items list
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
		
		// Handler for failed retrieval of bill items list
		router.on("payment", "billItemsList.fail", msg -> {
			BistroClient.awaitResponse = false;
			BistroClientGUI.client.getPaymentCTRL().setBillItemsList(null);
		});
		
		// Handler for successful payment completion
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
		
		// Handler for failed payment completion
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
		
		// Handler for successful manual payment processing
		router.on("payment", "processManually.ok", msg -> {
            BistroClient.awaitResponse = false;
			// Handle successful manual payment processing
			BistroClientGUI.client.getPaymentCTRL().setIsPaymentManuallySuccessful(true);
			BistroClientGUI.client.getTableCTRL().clearCurrentTable();
		});
		
		// Handler for failed manual payment processing
		router.on("payment", "processManually.fail", msg -> {
            BistroClient.awaitResponse = false;
		});
		
		// Handler for successful loading of pending bills
		router.on("payment", "loadPendingBills.ok", msg -> {
            BistroClient.awaitResponse = false;
			BistroClientGUI.client.getPaymentCTRL().setPendingBills((List<Bill>) msg.getData());
		});
		
		// Handler for failed loading of pending bills
		router.on("payment", "loadPendingBills.fail", msg -> {
			BistroClient.awaitResponse = false;
			Platform.runLater(() -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText("Failed to load pending bills. Please try again later.");
				alert.showAndWait();
			});
		});
	}
}
// End of ClientPaymentSubject.java