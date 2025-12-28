package gui.logic;

import java.io.IOException;
import entities.User;
import enums.UserType;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

/*
 * This class represents the controller for the Client Dashboard screen in the BistroClientGUI.
 */
public class ClientDashboardScreen {
	
	// ****************************** FXML Variables ******************************
	@FXML
	private VBox loyalpointVbox;

	@FXML
	private VBox discountVbox;

	@FXML
	private VBox statusVbox;

	@FXML
	private VBox becomeMemberVbox;

	@FXML
	private Button btnNewReservation;

	@FXML
	private Button btnJoinWaitingList;

	@FXML
	private Button btnCheckInForTable;

	@FXML
	private Button btnManageBooking;

	@FXML
	private Button btnPayBill;

	@FXML
	private Button btnEditPersonalDetails;

	@FXML
	private Button btnSignOut;

	@FXML
	private Label lblWelcome;

	@FXML
	private Label lblTopSubTitle;

	@FXML
	private Label lblError;

	// ******************************** FXML Methods ***********************************

	/*
	 * Method to initialize the Client Dashboard screen based on the logged-in user
	 * type.
	 */
	@FXML
	public void initialize() {
		UserType type = BistroClientGUI.client.getUserCTRL().getLoggedInUserType();
		switch (type) {
		case GUEST:
			SetDashboardAsGuest();
			//check if guest is on waiting list and change button name to "Waiting List Status":
			editJoinWaitingListButton();
			break;
		case MEMBER:
			SetDashboardAsMember(BistroClientGUI.client.getUserCTRL().getLoggedInUser());
			//check if member is on waiting list and change button name to "Waiting List Status":
			editJoinWaitingListButton();
			break;
		default:
			System.out.println("Error: Unknown user type.");
			break;
		}
	}
	
	/*
	 * Method to edit the Join Waiting List button text based on user's waiting list status.
	 */
	private void editJoinWaitingListButton() {
		String userID = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getID();
		BistroClientGUI.client.getWaitingListCTRL().askUserOnWaitingList(userID);
		if (BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList()) {
			btnJoinWaitingList.setText("Waiting List Status");
		} else {
			btnJoinWaitingList.setText("Join Waiting List");
		}
	}
	
	/*
	 * Method to set up the dashboard for a guest user.
	 */
	@FXML
	public void SetDashboardAsGuest() {
		lblWelcome.setText("Welcome, Guest!");
		lblTopSubTitle.setText("How can we serve you today?");
		btnEditPersonalDetails.setVisible(false);
		btnEditPersonalDetails.setManaged(false);
		loyalpointVbox.setVisible(false);
		loyalpointVbox.setManaged(false);
		discountVbox.setVisible(false);
		discountVbox.setManaged(false);
		statusVbox.setVisible(false);
		statusVbox.setManaged(false);
		becomeMemberVbox.setVisible(true);
		becomeMemberVbox.setManaged(true);
	}

	/*
	 * Method to set up the dashboard for a member user.
	 * 
	 * @param member The member user whose details are to be displayed.
	 */
	@FXML
	public void SetDashboardAsMember(User member) {
		lblWelcome.setText("Welcome, " + member.getFirstName() + " " + member.getLastName() + "!");
		lblTopSubTitle.setText("Member ID: " + member.getID());
		btnEditPersonalDetails.setVisible(true);
		btnEditPersonalDetails.setManaged(true);
		loyalpointVbox.setVisible(true);
		loyalpointVbox.setManaged(true);
		discountVbox.setVisible(true);
		discountVbox.setManaged(true);
		statusVbox.setVisible(true);
		statusVbox.setManaged(true);
		becomeMemberVbox.setVisible(false);
		becomeMemberVbox.setManaged(false);
	}

	/*
	 * Method to handle the action of creating a new reservation.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void NewReservation(Event event) {
		BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "Failed to load Client New Reservation Screen.");
	}

	/*
	 * Method to handle the action of joining the waiting list.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void JoinWaitingList(Event event) {
		String fxmlFileName;
		if(BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList()) {
			fxmlFileName = "clientOnListScreen";
		}
		else if(BistroClientGUI.client.getReservationCTRL().isUserReservationReady()) {
			fxmlFileName = "clienWaitingOverScreen";
		}
		else {
			fxmlFileName = "clientJoinWaitingListScreen";
		}
		BistroClientGUI.switchScreen(event, fxmlFileName, "Failed to load Client Join Waiting List Screen.");
	}

	/*
	 * Method to handle the action of checking in for a table.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void CheckInForTable(Event event) {
		BistroClientGUI.switchScreen(event, "clientCheckInTableScreen", "Failed to load Client Check-In For Table Screen.");
	}

	/*
	 * Method to handle the action of managing bookings.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void ManageBooking(Event event) {
		BistroClientGUI.switchScreen(event, "clientManageBookingScreen", "Failed to load Client Manage Booking Screen.");
	}

	/*
	 * Method to handle the action of paying a bill.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void PayBill(Event event) {
		BistroClientGUI.switchScreen(event, "clientPayBillScreen", "Failed to load Client Pay Bill Screen.");
	}

	/*
	 * Method to handle the action of editing personal details.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void EditPersonalDetails(Event event) {
		BistroClientGUI.switchScreen(event, "clientEditPersonalDetailsScreen", "Failed to load Client Edit Personal Details Screen.");
	}

	/*
	 * Method to handle the action of signing out.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void SignOut(Event event) {
		BistroClientGUI.client.getUserCTRL().signOutUser();
		if (BistroClientGUI.client.getUserCTRL().getLoggedInUser() == null) {
			BistroClientGUI.switchScreen(event, "loginScreen", "Failed to load Login Screen.");
		} else {
			display(lblError, "Failed to sign out. Please try again.", Color.RED);
		}
	}
	
	/*
	 * Method to display an error message in a label with a specified color.
	 * 
	 * @param lbl The label to display the message.
	 * 
	 * @param message The message to display.
	 * 
	 * @param color The color of the message text.
	 */
	public void display(Label lbl, String message, Color color) {
		lbl.setText(message); // Sets the error message in the label
		lbl.setTextFill(color); // Sets the text color for the error message
	}
}
// End of ClientDashboardScreen class