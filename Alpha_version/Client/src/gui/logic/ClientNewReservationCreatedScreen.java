package gui.logic;

import java.time.format.DateTimeFormatter;
import entities.Order;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import logic.BistroClientGUI;

/**
 * Controller for the Client New Reservation Created Screen.
 * Displays reservation details and provides navigation options.
 */
public class ClientNewReservationCreatedScreen {
	
	//*********************** FXML Components ***********************//
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
    
    //*********************** Instance Methods ***********************//
    
    /**
     * Better practice: Fetch data inside initialize() to ensure 
     * the Client instance is fully ready.
     */
    @FXML
    public void initialize() {
        try {
            Order reservation = BistroClientGUI.client.getReservationCTRL().getOrderDTO();
            if (reservation != null) {
                if (reservation.getOrderDate() != null) {
                    lblDate.setText(reservation.getOrderDate().toString());
                }
                if (reservation.getOrderHour() != null) {
                    lblHour.setText(reservation.getOrderHour().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
                lblDinersAmount.setText(reservation.getDinersAmount() + " People");
                String code = reservation.getConfirmationCode();
                lblConfirmCode.setText(code != null ? code : "N/A");
            }
        } catch (Exception e) {
            System.err.println("Error loading reservation details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the Back button action.
     * @param event
     */
    @FXML
    public void btnBack(Event event) {
    	cleanAndSwitch(event, "clientDashboardScreen");
    }
    
    /**
	 * Handles the New Reserve button action.
	 * @param event
	 */
    @FXML
    public void btnNewReserve(Event event) {
    	cleanAndSwitch(event, "clientNewReservationScreen");
    }

    /**
     * Helper method to reduce code duplication
     */
    private void cleanAndSwitch(Event event, String screenName) {
        BistroClientGUI.client.getReservationCTRL().setOrderDTO(null);
        String errorMsg = "Error navigating to " + screenName;
        BistroClientGUI.switchScreen(event, screenName, errorMsg);
    }
}
// End of ClientNewReservationCreatedScreen.java