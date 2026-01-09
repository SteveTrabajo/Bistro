package gui.logic;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import entities.User;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/**
 * ClientJoinWaitingListScreen class handles the logic for the client join
 * waiting list screen. It allows clients to select the number of diners and
 * join the waiting list.
 */
public class ClientJoinWaitingListScreen {
	
	//**************** FXML Variables ****************//
	@FXML
	private Button btnCheckAvail;
	
	@FXML
	private Button btnPlus;
	
	@FXML
	private Button btnMinus;
	
	@FXML
	private Button btnBack;
	
	@FXML
	private Label lblDinersAmount;
	
	@FXML
	private Label lblUser;
	
	@FXML
	private Label lblError;
	
	//**************** FXML Methods ****************//
	/**
	 * Initializes the screen by setting up user information and button actions.
	 */
	@FXML
	public void initialize() {
		User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
		lblUser.setText(currentUser.getUserType().name());
		lblDinersAmount.setText("1");
	}
	
	/**
	 * Handles the action when the "+" button is clicked.
	 * It increments the number of diners, ensuring it does not exceed 12.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnPlus(Event event) {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if (currentAmount < 12) {
			currentAmount++;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	}
	
	
	/**
	 * Handles the action when the "-" button is clicked.
	 * It decrements the number of diners, ensuring it does not go below 1.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnMinus(Event event) {
		int currentAmount = Integer.parseInt(lblDinersAmount.getText());
		if (currentAmount > 1) {
			currentAmount--;
			lblDinersAmount.setText(String.valueOf(currentAmount));
		}
	}
	
	
	/**
	 * Handles the action when the "Check Availability" button is clicked.
	 * It attempts to join the waiting list with the specified number of diners.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnCheckAvail(Event event) {
		int dinersAmount = Integer.parseInt(lblDinersAmount.getText());
		BistroClientGUI.client.getWaitingListCTRL().checkWaitingListAvailability(dinersAmount);
		if(BistroClientGUI.client.getWaitingListCTRL().getcanSeatImmediately()) {
			BistroClientGUI.client.getWaitingListCTRL().setCanSeatImmediately(false);
			BistroClientGUI.switchScreen(event, "clientWaitingOverScreen", "failed to load waiting over screen");
			return;
		}
		else {
			openAskToJoinWaitingListScreen(event, dinersAmount);
			
		}
	}
	
	private void openAskToJoinWaitingListScreen(LocalTime earliestTime, int dinersAmount) {
	    
	    long minutesToWait = 0;
	    if (earliestTime != null) {
	        minutesToWait = ChronoUnit.MINUTES.between(LocalTime.now(), earliestTime);
	        if (minutesToWait < 0) minutesToWait = 0;
	    } else {
	        showAlert("Error", "No suitable tables found for this group size.");
	        return;
	    }

	    Alert alert = new Alert(AlertType.CONFIRMATION);
	    alert.setTitle("No Tables Available");
	    alert.setHeaderText("The restaurant is currently full.");
	    alert.setContentText("Next table for " + dinersAmount + " diners will be free in approx.\n" 
	                         + minutesToWait + " minutes (" + earliestTime.toString() + ").\n\n"
	                         + "Would you like to join the Waiting List?");

	    ButtonType btnJoin = new ButtonType("Join Waitlist", ButtonData.OK_DONE);
	    ButtonType btnCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
	    
	    alert.getButtonTypes().setAll(btnJoin, btnCancel);

	    Optional<ButtonType> result = alert.showAndWait();
	    
	    if (result.isPresent() && result.get() == btnJoin) {
	        openContactDetailsDialog(dinersAmount); 
	    } else {
	        System.out.println("User declined waiting list.");
	    }
	}
	private void openContactDetailsDialog(int dinersAmount) {
	    TextInputDialog dialog = new TextInputDialog();
	    dialog.setTitle("Contact Details");
	    dialog.setHeaderText("Enter Phone Number or Email");
	    dialog.setContentText("To notify you when the table is ready:");

	    Optional<String> result = dialog.showAndWait();
	    
	    result.ifPresent(contactInfo -> {
	        if (!contactInfo.trim().isEmpty()) {
	            BistroClientGUI.client.getWaitingListCTRL().askJoinWaitingList(dinersAmount, true, contactInfo);
	        }
	    });
	}

	/**
	 * Handles the action when the "Back" button is clicked.
	 * It navigates back to the client dashboard screen.
	 * 
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
