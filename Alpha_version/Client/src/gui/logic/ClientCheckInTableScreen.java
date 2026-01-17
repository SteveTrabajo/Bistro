package gui.logic;

import entities.Order;
import entities.User;
import enums.UserType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
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
	@FXML private Button btnCheckIn;
	@FXML private Button btnBack;
	@FXML private Hyperlink lnkForgot;
	@FXML private TextField txtConfirmCode;
	@FXML private Label lblUser;
	@FXML private Label lblError;
	
	// Modal containers
	@FXML private StackPane modalOverlay;
	
	private ClientForgotConfirmCodeScreen forgotModalsCTRL;
	private Parent ForgotIDModalRoot;

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
		// 2. Length Limiter (UX)
		int maxLength = 8;
		txtConfirmCode.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.length() > maxLength) {
				txtConfirmCode.setText(oldValue);
			}
		});
	}
	
	/**
	 * Handles the Check-In button click event.
	 * Now uses ASYNC listener logic instead of immediate checking.
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
				if (isSuccess) {
					Order confirmedOrder = new Order();
					confirmedOrder.setConfirmationCode(code);
					
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
	 */
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "ClientDashboardScreen", "Error returning to Dashboard");
	}
	
	
    @FXML
    public void lnkForgot(Event event) {
        User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
        
        // 1. MEMBER FLOW
        if (currentUser != null && currentUser.getUserType() == UserType.MEMBER) {
            showMemberSelectionDialog();
        } 
        // 2. GUEST FLOW (or not logged in)
        else {
            showGuestRecoveryDialog();
        }
    }

    // ============================================================================================
    // LOGIC 1: GUEST FLOW (Email/Phone Lookup)
    // ============================================================================================
    private void showGuestRecoveryDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Find Booking Code");
        dialog.setHeaderText("Enter the contact details used for the booking.");

        ButtonType searchType = new ButtonType("Find Code", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(searchType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(statusLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(emailField::requestFocus);

        final Button btSearch = (Button) dialog.getDialogPane().lookupButton(searchType);
        
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

            // Set Listener
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

    // ============================================================================================
    // LOGIC 2: MEMBER FLOW (List Selection)
    // ============================================================================================
    private void showMemberSelectionDialog() {
        // Show loading state first? Or just wait for callback. 
        // For better UX, let's trigger the request and wait for the response.
        
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

            ButtonType selectType = new ButtonType("Select", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(selectType, ButtonType.CANCEL);

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
                        // Format: "12/05 19:00 - 4 Guests (Code: R-1234)"
                        setText(String.format("%s %s - %d Guests (Code: %s)", 
                            item.getOrderDate().toString(),
                            item.getOrderHour().toString(),
                            item.getDinersAmount(),
                            item.getConfirmationCode()));
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
			// Optional: Remove it to save memory or keep it to load faster next time
			// modalOverlay.getChildren().clear(); 
			// ForgotIDModalRoot = null; 
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setContentText("Unable to close the Forgot Code screen (Overlay not found).");
			alert.showAndWait();
		}
	}
}