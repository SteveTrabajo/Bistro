package gui.logic;

import java.io.IOException;

import entities.Guest;
import entities.Member;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientDashboardScreen {
	
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
	
	
	@FXML
	public void initialize() {
		Object user = BistroClientGUI.client.getCurrentUser();
		if (user instanceof Guest) {
			SetDashboardAsGuest();
		} else {
			SetDashboardAsMember((Member)user);
		}
		return;
	}
	
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
	
	public void SetDashboardAsMember(Member member) {
		lblWelcome.setText("Welcome, " + member.getFirstName() + " " + member.getLastName() + "!");
		lblTopSubTitle.setText("Member ID: " + member.getMemberID());
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
	
	@FXML
	public void NewReservation(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientNewReservationScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"New Reservation");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client New Reservation Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
	}
	
	@FXML
	public void JoinWaitingList(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientJoinWaitingListScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"Join Waiting List");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Join Waiting List Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
		
	}
	
	@FXML
	public void CheckInForTable(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientCheckInForTableScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"Check In For Table");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Check In For Table Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
		
	}
	
	@FXML
	public void ManageBooking(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientManageBookingScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"Manage Booking");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Manage Booking Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
		
	}
	
	@FXML
	public void PayBill(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientPayBillScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"Pay Bill");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Pay Bill Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
		
	}
	
	@FXML
	public void EditPersonalDetails(Event event) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/clientEditPersonalDetailsScreen.fxml"));
		try {
			loader.load();
			Parent root = loader.getRoot();
			BistroClientGUI.switchScreen(loader,root,event,"Edit Personal Details");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to load Client Edit Personal Details Screen.");
			display(lblError, "Failel loading screen", Color.RED);
		}
		
	}
	
	@FXML
	public void SignOut(Event event) {
		if(BistroClientGUI.client.logoutUser()) {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/screens/loginScreen.fxml"));
			try {
				loader.load();
				Parent root = loader.getRoot();
				BistroClientGUI.switchScreen(loader,root,event,"Login");
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
