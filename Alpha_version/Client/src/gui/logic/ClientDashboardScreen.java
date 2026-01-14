package gui.logic;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import entities.User;
import enums.OrderStatus;
import enums.UserType;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
	private GridPane gridPane;

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
	private Label lblClient;

	@FXML
	private Label lblError;
	
	@FXML
	private Label LblButtonDescrip;
	
	@FXML 
	private StackPane rootPane;
	
	
	// ******************************** FXML Methods ***********************************

	/**
	 * Method to initialize the Client Dashboard screen based on the logged-in user
	 * type.
	 */
	@FXML
	public void initialize() {
	    User loggedInUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
	    int userID = loggedInUser.getUserId();
	    TaskRunner.run (rootPane,()->{
	    // Fetch all necessary states at once
	    	BistroClientGUI.client.getWaitingListCTRL().askUserOnWaitingList(userID);
	    	BistroClientGUI.client.getTableCTRL().askUserAllocatedSeatedOrder(userID);
	    },()->{
	    	boolean isOnWaitingList = BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList();
	    	boolean hasActiveSeatedReservation = BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable() != null;
	    	// Set user-type specific layout
	    	if (BistroClientGUI.client.getUserCTRL().getLoggedInUserType() == UserType.MEMBER) {
	    		SetDashboardAsMember(loggedInUser);
	    	} else {
	    		SetDashboardAsGuest();
	    	}
	    	//Centralized UI State Management
	    	applyBusinessRules(isOnWaitingList, hasActiveSeatedReservation);
	    	});
	}

	/**
	 * Centralizes the logic for button states and labels to avoid code duplication.
	 */
	private void applyBusinessRules(boolean isOnWaitingList, boolean hasActiveSeatedReservation) {
	    // Handle Waiting List button text
		
		if (isOnWaitingList && BistroClientGUI.client.getWaitingListCTRL().getOrderStatus()==OrderStatus.NOTIFIED) {
			 BistroClientGUI.switchScreen("clientCheckInTableScreen", "Failed to load Client Check-In For Table Screen.");
		}
	    LblButtonDescrip.setText(isOnWaitingList ? "Waiting List Status" : "Add to queue");
	    btnJoinWaitingList.setText(isOnWaitingList ? "View Status": "Join Waiting List");
	    // Disable actions if an active reservation exists
	    if (hasActiveSeatedReservation) {
	        btnJoinWaitingList.setDisable(true);
	        btnCheckInForTable.setDisable(true);
	        display(lblError, "Active reservation detected. Some options are disabled.", Color.ORANGE);
	    } else {
	        btnJoinWaitingList.setDisable(false);
	        btnCheckInForTable.setDisable(false);
	    }
	}
	
	/**
	 * Method to edit the Join Waiting List button text based on user's waiting list status.
	 */
	private void editJoinWaitingListButton() {
		int userID = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserId();
		BistroClientGUI.client.getWaitingListCTRL().askUserOnWaitingList(userID);
		if (BistroClientGUI.client.getWaitingListCTRL().isUserOnWaitingList()) {
			LblButtonDescrip.setText("Waiting List Status");
		} else {
			LblButtonDescrip.setText("Add to queue");
		}
	}
	
	/**
	 * Method to set up the dashboard for a guest user.
	 */
	@FXML
	public void SetDashboardAsGuest() {
	    lblWelcome.setText("Welcome, Guest!");
	    lblClient.setText("How can we serve you today?");
	    // Disable member-only features
	    btnEditPersonalDetails.setVisible(false);
	    btnEditPersonalDetails.setManaged(false);
	    btnManageBooking.setVisible(false);
	    btnManageBooking.setManaged(false);
	    loyalpointVbox.setVisible(false);
	    loyalpointVbox.setManaged(false);
	    discountVbox.setVisible(false);
	    discountVbox.setManaged(false);
	    statusVbox.setVisible(false);
	    statusVbox.setManaged(false);
	    becomeMemberVbox.setVisible(true);
	    becomeMemberVbox.setManaged(true);
	    gridPane.setAlignment(Pos.CENTER);
	}


	/**
	 * Method to set up the dashboard for a member user.
	 * 
	 * @param member The member user whose details are to be displayed.
	 */
	@FXML
	public void SetDashboardAsMember(User member) {
		lblWelcome.setText("Welcome, " + member.getFirstName() + " " + member.getLastName() + "!");
		lblClient.setText("Member ID: " + member.getMemberCode());
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

	/**
	 * Method to handle the action of creating a new reservation.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void NewReservation(Event event) {
		BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "Failed to load Client New Reservation Screen.");
	}

	/**
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
		else {
			fxmlFileName = "clientJoinWaitingListScreen";
		}
		BistroClientGUI.switchScreen(event, fxmlFileName, "Failed to load Client Join Waiting List Screen.");
	}
	

	/**
	 * Method to handle the action of checking in for a table.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void CheckInForTable(Event event) {
		BistroClientGUI.switchScreen(event, "clientCheckInTableScreen", "Failed to load Client Check-In For Table Screen.");
	}

	/**
	 * Method to handle the action of managing bookings.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void ManageBooking(Event event) {
		BistroClientGUI.switchScreen(event, "clientManageBookingScreen", "Failed to load Client Manage Booking Screen.");
	}

	/**
	 * Method to handle the action of paying a bill.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void PayBill(Event event) {
		BistroClientGUI.switchScreen(event, "clientPayBillScreen", "Failed to load Client Pay Bill Screen.");
	}

	/**
	 * Method to handle the action of editing personal details.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void EditPersonalDetails(Event event) {
		BistroClientGUI.switchScreen(event, "clientEditPersonalDetailsScreen", "Failed to load Client Edit Personal Details Screen.");
	}

	/**
	 * Method to handle the action of signing out.
	 * 
	 * @param event The event that triggered this action.
	 */
	@FXML
	public void btnSignOut(Event event) {
		boolean clearPayment = BistroClientGUI.client.getPaymentCTRL().clearPaymentController();
		boolean clearReservation = BistroClientGUI.client.getReservationCTRL().clearReservationController();
		boolean clearTable = BistroClientGUI.client.getTableCTRL().clearTableController();
		boolean clearWaitingList = BistroClientGUI.client.getWaitingListCTRL().clearWaitingListController();
		if(!clearPayment || !clearReservation || !clearTable || !clearWaitingList) {
			display(lblError, "Error clearing user data. Please try again.", Color.RED);
			return;
		}
		BistroClientGUI.client.getUserCTRL().signOutUser();
		if (BistroClientGUI.client.getUserCTRL().getLoggedInUser()== null) {
			boolean clearUser = BistroClientGUI.client.getUserCTRL().clearUserController();
			if(!clearUser) {
				display(lblError, "Error clearing user data. Please try again.", Color.RED);
				return;
			}
			BistroClientGUI.switchScreen(event, "clientLoginScreen", "Failed to load Login Screen.");
		} else {
			display(lblError, "Failed to sign out. Please try again.", Color.RED);
		}
	}
	
	
	/**
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