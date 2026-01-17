package gui.logic;

import entities.Order;
import entities.User;
import enums.UserType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;
import java.util.Optional;
import common.InputCheck;

/**
 * This class represents the controller for the Client Check-In Table screen in
 * the BistroClientGUI.
 */
public class ClientCheckInTableScreen {
	
	// ****************************** FXML Elements ******************************
	
	@FXML 
	private Button btnCheckIn;
	@FXML 
	private Button btnBack;
	@FXML 
	private Hyperlink lnkForgot;
	@FXML
	private TextField txtConfirmCode;
	@FXML
	private Label lblUser;
	@FXML
	private Label lblError;
	@FXML
	private StackPane modalOverlay;
	
	// ****************************** Instance Methods ******************************

	/**
	 * Initializes the Client Check-In Table screen.
	 */
	@FXML
	public void initialize() {
		if (BistroClientGUI.client != null && BistroClientGUI.client.getUserCTRL().getLoggedInUser() != null) {
			User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
			if (lblUser != null) {
				lblUser.setText(currentUser.getUserType().name());
			}
		}
		int maxLength = 8; // Example max length
		txtConfirmCode.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > maxLength) {
				txtConfirmCode.setText(oldValue);
			}
		});
	}
	
	/**
	 * Handles the Check-In button click event.
	 * @Param event The event triggered by clicking the Check-In button.
	 */
	@FXML
	public void btnCheckIn(Event event) {
		String code = txtConfirmCode.getText();
		String errorMsg = InputCheck.checkConfirmationCode(code);
		if (errorMsg != null) {
			BistroClientGUI.display(lblError, errorMsg, Color.RED);
			return;
		}
		lblError.setText(""); 
		btnCheckIn.setDisable(true); 
		BistroClientGUI.client.getReservationCTRL().setCheckInListener((isSuccess, message) -> {
			Platform.runLater(() -> {
				btnCheckIn.setDisable(false);
				if (isSuccess) { // case of success
					Order confirmedOrder = new Order();
					confirmedOrder.setConfirmationCode(code);
					// Parse table number from message
					try {
						int tableNum = Integer.parseInt(message);
						confirmedOrder.setTableId(tableNum);
					} catch (NumberFormatException e) {
						System.err.println("Failed to parse table number: " + message);
						confirmedOrder.setTableId(0);
					}
					BistroClientGUI.client.getTableCTRL().setUserAllocatedOrderForTable(confirmedOrder);
					BistroClientGUI.switchScreen(event, "clientCheckInTableSuccessScreen", "Success");
				} else {
					BistroClientGUI.display(lblError, message, Color.RED);
				}
			});
		});
		
		BistroClientGUI.client.getReservationCTRL().seatCustomer(code);
	}
	
	/**
	 * Handles the Back button click event.
	 * @Param event The event triggered by clicking the Back button.
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Dashboard");
	}
	
	/**
	 * Handles the Forgot Confirmation Code link click event.
	 * @Param event The event triggered by clicking the Forgot Confirmation Code link.
	 */
    @FXML
    public void lnkForgot(Event event) {
        User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
        //case of member user
        if (currentUser != null && currentUser.getUserType() == UserType.MEMBER) {
            showMemberSelectionDialog();
        } 
        else { //case of guest user
            showGuestRecoveryDialog();
        }
    }

    /**
     * Displays the guest recovery dialog for finding a booking code.
     */
	private void showGuestRecoveryDialog() {
		// Create Dialog
		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("Find Booking Code");
		dialog.setHeaderText("Enter the contact details used for the booking.");
		// Set the button types
		ButtonType searchType = new ButtonType("Find Code", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(searchType, ButtonType.CANCEL);
		// Create the email and phone labels and fields
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 50, 10, 10));
		// Input Fields
		TextField emailField = new TextField();
		emailField.setPromptText("Email Address");
		TextField phoneField = new TextField();
		phoneField.setPromptText("Phone Number");
		Label statusLabel = new Label();
		statusLabel.setWrapText(true);
		statusLabel.setMaxWidth(300);
		// Add to Grid
		grid.add(new Label("Email:"), 0, 0);
		grid.add(emailField, 1, 0);
		grid.add(new Label("Phone:"), 0, 1);
		grid.add(phoneField, 1, 1);
		grid.add(statusLabel, 0, 2, 2, 1);
		// Enable/Disable search button depending on whether a field was entered.
		dialog.getDialogPane().setContent(grid);
		Platform.runLater(emailField::requestFocus);
		// Handle Search Button Action	
		final Button btSearch = (Button) dialog.getDialogPane().lookupButton(searchType);
		// Custom Action
		btSearch.addEventFilter(ActionEvent.ACTION, e -> {
			e.consume(); // Prevent close
			String email = emailField.getText().trim();
			String phone = phoneField.getText().trim();
			if (email.isEmpty() && phone.isEmpty()) {
				statusLabel.setTextFill(Color.RED);
				statusLabel.setText("Please enter Email or Phone.");
				return;
			}
			statusLabel.setTextFill(Color.BLUE);
			statusLabel.setText("Searching...");
			btSearch.setDisable(true);
			// Set Callback
			BistroClientGUI.client.getReservationCTRL().setOnConfirmationCodeRetrieveResult(result -> {
				Platform.runLater(() -> {
					btSearch.setDisable(false);
					if (result == null || "NOT_FOUND".equals(result)) {
						statusLabel.setTextFill(Color.RED);
						statusLabel.setText("No active reservation found for today.");
					} else {
						// Success! Fill the field and close
						txtConfirmCode.setText(result);
						dialog.setResult(result);
						dialog.close();
					}
				});
			});
			// Send Request
			BistroClientGUI.client.getReservationCTRL().retrieveConfirmationCode(email, phone);
		});
		dialog.showAndWait();
	}

 	/**
	 * Displays a dialog for member users to select from their active reservations.
	 */
	private void showMemberSelectionDialog() {
		BistroClientGUI.client.getReservationCTRL().setOnMemberReservationsListListener(ordersList -> {
			if (ordersList == null || ordersList.isEmpty()) {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("No Reservations");
				alert.setHeaderText("No Active Reservations Found");
				alert.setContentText("You don't have any upcoming reservations.");
				alert.showAndWait();
				return;
			}
			// Create Selection Dialog
			Dialog<Order> dialog = new Dialog<>();
			dialog.setTitle("Select Reservation");
			dialog.setHeaderText("Select the reservation to check in:");
			// Set the button types
			ButtonType selectType = new ButtonType("Select", ButtonData.OK_DONE);
			dialog.getDialogPane().getButtonTypes().addAll(selectType, ButtonType.CANCEL);
			// Create List View
			ListView<Order> listView = new ListView<>();
			listView.getItems().addAll(ordersList);
			listView.setPrefHeight(200);
			// Custom Cell Factory to show pretty text
			listView.setCellFactory(param -> new ListCell<>() {
				@Override
				protected void updateItem(Order item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setText(null);
					} else {
						setText(String.format("%s %s - %d Guests (Code: %s)", item.getOrderDate().toString(),
								item.getOrderHour().toString(), item.getDinersAmount(), item.getConfirmationCode()));
					}
				}
			});
			dialog.getDialogPane().setContent(listView);
			// Handle Selection
			dialog.setResultConverter(dialogButton -> {
				if (dialogButton == selectType) {
					return listView.getSelectionModel().getSelectedItem();
				}
				return null;
			});
			// Show Dialog and Handle Result
			Optional<Order> result = dialog.showAndWait();
			result.ifPresent(order -> {
				txtConfirmCode.setText(order.getConfirmationCode());
			});
		});
		// Send Request
		BistroClientGUI.client.getReservationCTRL().askMemberActiveReservations();
	}
	
	/**
	 * Displays an error message on the screen.
	 * @Param message The error message to display.
	 */
	public void showSuccessMessage(String message) {
		BistroClientGUI.display(lblError, message, Color.GREEN);
	}
	
	/**
	 * Closes the Forgot Code modal screen.
	 * Fixed logic to correctly hide the overlay.
	 */
	public void closeForgotCodeScreen() {
		if (modalOverlay != null) {
			modalOverlay.setVisible(false);
			modalOverlay.setManaged(false);
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setContentText("Unable to close the Forgot Code screen (Overlay not found).");
			alert.showAndWait();
		}
	}
}
//End of ClientCheckInTableScreen.java