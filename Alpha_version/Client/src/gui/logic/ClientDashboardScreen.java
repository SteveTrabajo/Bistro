package gui.logic;

import java.io.IOException;
import entities.User;
import entities.UserType;
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
			break;
		case MEMBER:
			SetDashboardAsMember(BistroClientGUI.client.getUserCTRL().getLoggedInUser());
			break;
		default:
			System.out.println("Error: Unknown user type.");
			break;
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
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientNewReservationScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "New Reservation", "Failed to load Client New Reservation Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client New Reservation Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
	}

	/*
	 * Method to handle the action of joining the waiting list.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void JoinWaitingList(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientJoinWaitingListScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "Join Waiting List", "Failed to load Client Join Waiting List Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Join Waiting List Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}

	}

	/*
	 * Method to handle the action of checking in for a table.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void CheckInForTable(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientCheckInForTableScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "Check In For Table",
					"Failed to load Client Check In For Table Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Check In For Table Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}

	}

	/*
	 * Method to handle the action of managing bookings.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void ManageBooking(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientManageBookingScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "Manage Booking", "Failed to load Client Manage Booking Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Manage Booking Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}

	}

	/*
	 * Method to handle the action of paying a bill.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void PayBill(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientPayBillScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "Pay Bill", "Failed to load Client Pay Bill Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Pay Bill Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}

	}

	/*
	 * Method to handle the action of editing personal details.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void EditPersonalDetails(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientEditPersonalDetailsScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(event, "Edit Personal Details",
					"Failed to load Client Edit Personal Details Screen.");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Edit Personal Details Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}

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
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/loginScreen.fxml"));
			try {
				loader.load();
				Parent root = loader.getRoot();
				BistroClientGUI.switchScreen(event, "Login", "Failed to load Login Screen.");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Failed to load Login Screen.");
				display(lblError, "Failel loading screen", Color.RED);
			}
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