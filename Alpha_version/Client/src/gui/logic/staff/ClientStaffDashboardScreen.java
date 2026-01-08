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
	
	@FXML
	public void initialize() {
		String role = BistroClientGUI.client.getUserCTRL().getLoggedInUserType().toString();
		lblRole.setText(role);
		btnTableOverview(null);
		if(role.equals("EMPLOYEE")) {
			btnAnalytics.setDisable(true);
		}
	}
	
	@FXML
	public void btnTableOverview(Event event) {
		setActive(btnTableOverview);
		loadPanel("/gui/fxml/staff/TableOverviewPanel.fxml");
	}
	
	@FXML
	public void btnReservations(Event event) {
		setActive(btnReservations);
		loadPanel("/gui/fxml/staff/ReservationsPanel.fxml");
	}
	
	@FXML
	public void btnWaitingList(Event event) {
		setActive(btnWaitingList);
		loadPanel("/gui/fxml/staff/WaitingListPanel.fxml");
	}
	
	@FXML
	public void btnPayment(Event event) {
		setActive(btnPayment);
		loadPanel("/gui/fxml/staff/PaymentPanel.fxml");
	}
	
	@FXML
	public void btnCustomers(Event event) {
		setActive(btnCustomers);
		loadPanel("/gui/fxml/staff/CustomersPanel.fxml");
	}
	
	@FXML
	public void btnMemberRegister(Event event) {
		setActive(btnMemberRegister);
		loadPanel("/gui/fxml/staff/MemberRegistrationPanel.fxml");
	}
	
	@FXML
	public void btnAnalytics(Event event) {
		setActive(btnAnalytics);
		loadPanel("/gui/fxml/staff/AnalyticsPanel.fxml");
	}
	
	@FXML
	public void btnRestaurantManagment(Event event) {
		setActive(btnRestaurantManagment);
		loadPanel("/gui/fxml/staff/RestaurantManagementPanel.fxml");
	}
	
	@FXML
	public void btnLogout(Event event) {
		BistroClientGUI.client.getUserCTRL().signOutUser();
		if (BistroClientGUI.client.getUserCTRL().getLoggedInUser() == null) {
			BistroClientGUI.switchScreen(event, "clientLoginScreen", "Failed to load Login Screen.");
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
