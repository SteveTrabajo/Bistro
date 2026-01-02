package gui.logic;

import java.time.LocalTime;
import java.util.List;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * This class represents the controller for the Client New Reservation Created
 * screen in the BistroClientGUI.
 */
public class ClientNewReservationCreatedScreen {
	// ****************************** FXML Variables ******************************
	@FXML
	private Label lblDate;
	@FXML
	private Label lblHour;
	@FXML
	private Label lblDinersAmount;
	@FXML
	private Label lblConfirmCode;
	@FXML
	private Button btnBack;
	@FXML
	private Button btnNewReserve;
	// *************************** FXML Methods ****************************
	/**
	 * Initializes the Client New Reservation Created screen.
	 */
	public void initialize() {
		List<Object> reservationDetails = BistroClientGUI.client.getReservationCTRL().getTempReservationData();
		String date = reservationDetails.get(0).toString();
		LocalTime time = (LocalTime) reservationDetails.get(1);
		int dinersAmount = (Integer) reservationDetails.get(2);
		String confirmCode = BistroClientGUI.client.getReservationCTRL().getConfirmationCode();
		lblDate.setText(date);
		lblHour.setText(time.toString());
		lblDinersAmount.setText(String.valueOf(dinersAmount) + "People");
		lblConfirmCode.setText(confirmCode);
	}
	
	/**
	 * Handles the action when the Back button is clicked.
	 * 
	 * @param event The event triggered by clicking the Back button.
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}

	/**
	 * Handles the action when the New Reserve button is clicked.
	 * 
	 * @param event The event triggered by clicking the New Reserve button.
	 */
	@FXML
	public void btnNewReserve(Event event) {
		BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "client Make Reservation error messege");

	}
}
//End of ClientNewReservationCreatedScreen.java