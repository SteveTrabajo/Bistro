package gui.logic.staff;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import logic.BistroClientGUI;

public class ClientStaffDashboardScreen {
	@FXML
	private Label lblRole;	
	@FXML
	private Button btnTableOverview;	
	@FXML
	private Button btnReservations;	
	@FXML
	private Button btnWaitingList;	
	@FXML
	private Button btnPayment;	
	@FXML
	private Button btnCustomers;	
	@FXML
	private Button btnMemberRegister;	
	@FXML
	private Button btnAnalytics;	
	@FXML
	private Button btnRestaurantManagment;	
	@FXML
	private Button btnLogout;	
	@FXML
	private StackPane contentPane;
	
	private static final String ACTIVE_CSS_CLASS = "mgmt-menu-button-active";	
	private Button activeButton;
	
	/** Initializes the controller class. This method is automatically called
	 * after the fxml file has been loaded.
	 */
	@FXML
	public void initialize() {
		String role = BistroClientGUI.client.getUserCTRL().getLoggedInUserType().toString();
		lblRole.setText(role);
		btnTableOverview(null);
		if(role.equals("EMPLOYEE")) {
			btnAnalytics.setDisable(true);
		}
	}
	
	/* 
	 * Button Event Handlers 
	 */
	@FXML
	public void btnTableOverview(Event event) {
		setActive(btnTableOverview);
		loadPanel("/gui/fxml/staff/TableOverviewPanel.fxml");
	}
	
	/*
	 * Reservations Button Event Handler
	 */
	@FXML
	public void btnReservations(Event event) {
		setActive(btnReservations);
		loadPanel("/gui/fxml/staff/ReservationsPanel.fxml");
	}
	
	/*
	 * Waiting List Button Event Handler
	 */
	@FXML
	public void btnWaitingList(Event event) {
		setActive(btnWaitingList);
		loadPanel("/gui/fxml/staff/WaitingListPanel.fxml");
	}
	
	/*
	 * Payment Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnPayment(Event event) {
		setActive(btnPayment);
		loadPanel("/gui/fxml/staff/PaymentPanel.fxml");
	}
	
	/*
	 * Customers Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnCustomers(Event event) {
		setActive(btnCustomers);
		loadPanel("/gui/fxml/staff/CustomersPanel.fxml");
	}
	
	/*
	 * Member Registration Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnMemberRegister(Event event) {
		setActive(btnMemberRegister);
		loadPanel("/gui/fxml/staff/MemberRegistrationPanel.fxml");
	}
	
	/*
	 * Analytics Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnAnalytics(Event event) {
		setActive(btnAnalytics);
		loadPanel("/gui/fxml/staff/AnalyticsPanel.fxml");
	}
	
	/*
	 * Restaurant Management Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnRestaurantManagment(Event event) {
		setActive(btnRestaurantManagment);
		loadPanel("/gui/fxml/staff/RestaurantManagementPanel.fxml");
	}
	
	/*
	 * Logout Button Event Handler
	 * @param event
	 */
	@FXML
	public void btnLogout(Event event) {
		boolean clearPayment = BistroClientGUI.client.getPaymentCTRL().clearPaymentController();
		boolean clearReservation = BistroClientGUI.client.getReservationCTRL().clearReservationController();
		boolean clearTable = BistroClientGUI.client.getTableCTRL().clearTableController();
		boolean clearWaitingList = BistroClientGUI.client.getWaitingListCTRL().clearWaitingListController();
		if(!clearPayment || !clearReservation || !clearTable || !clearWaitingList) {
			System.out.println("Error clearing controllers during logout.");
			return;
		}
		BistroClientGUI.client.getUserCTRL().signOutUser();
		if (BistroClientGUI.client.getUserCTRL().getLoggedInUser()== null) {
			boolean clearUser = BistroClientGUI.client.getUserCTRL().clearUserController();
			if(!clearUser) {
				System.out.println("Error clearing user controller during logout.");
				return;
			}
			BistroClientGUI.switchScreen(event, "clientLoginScreen", "Failed to load Login Screen.");
		} else {
			System.out.println("Failed to sign out. Please try again.");
		}
	}
	
	/**
	 * Method to load a panel into the content pane.
	 * @param fxmlPath
	 */
	private void loadPanel(String fxmlPath) {
		try {
			Parent panel = FXMLLoader.load(getClass().getResource(fxmlPath));
			contentPane.getChildren().setAll(panel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the active button and updates styles.
	 * @param newActive The button to set as active.
	 */
	private void setActive(Button newActive) {
	    if (activeButton != null) {
	        activeButton.getStyleClass().remove(ACTIVE_CSS_CLASS);
	    }
	    activeButton = newActive;
	    if (!activeButton.getStyleClass().contains(ACTIVE_CSS_CLASS)) {
	        activeButton.getStyleClass().add(ACTIVE_CSS_CLASS);
	    }
	}
}
// End of ClientStaffDashboardScreen.java