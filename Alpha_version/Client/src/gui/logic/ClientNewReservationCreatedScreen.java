package gui.logic;

import java.time.LocalTime;
import java.util.List;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

public class ClientNewReservationCreatedScreen {
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
public void initialize() {
	
	List<Object> reservationDetails = BistroClientGUI.client.getReservationCTRL().getTempReservationData();
	String date = reservationDetails.get(0).toString();
	LocalTime time = (LocalTime) reservationDetails.get(1);
	int dinersAmount = (Integer) reservationDetails.get(2);
	String confirmCode = BistroClientGUI.client.getReservationCTRL().getConfirmationCode();
	lblDate.setText(date);
	lblHour.setText(time.toString());
	lblDinersAmount.setText(String.valueOf(dinersAmount)+"People");
	lblConfirmCode.setText(confirmCode);
	}
@FXML
public void btnBack(Event event) {
	BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}
@FXML
public void btnNewReserve(Event event) {
	BistroClientGUI.switchScreen(event, "clientNewReservationScreen", "client Make Reservation error messege");

	}
}
