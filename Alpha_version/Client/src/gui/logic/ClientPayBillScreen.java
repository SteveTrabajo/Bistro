package gui.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import logic.BistroClientGUI;

public class ClientPayBillScreen {
	@FXML
	private Button btnBack;
	@FXML
	private Button btnVerify;
	@FXML
	private Hyperlink lnkForgot;
	@FXML
	private TextField txtConfirmCode;
	@FXML
	private Label lblUserStat;
	@FXML
	private Label lblError;

	

	@FXML
	public void initialize() {
        User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
        if (currentUser != null) {
            lblUserStat.setText(currentUser.getUserType().name());
        }
    }
	
	@FXML
	public void btnBack(Event event) {
		BistroClientGUI.switchScreen(event, "clientDashboardScreen", "client Dashboard error messege");
	}
	
	@FXML	
	public void lnkForgot(Event event) {
        User currentUser = BistroClientGUI.client.getUserCTRL().getLoggedInUser();
        
        // 1. MEMBER FLOW
        if (currentUser != null && currentUser.getUserType() == UserType.MEMBER) {
            showMemberSeatedSelectionDialog();
        } 
        // 2. GUEST FLOW
        else {
            showGuestSeatedRecoveryDialog();
        }
    }

	private void showGuestSeatedRecoveryDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Find Payment Code");
        dialog.setHeaderText("Enter the contact details for your current table.");

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
            e.consume(); 

            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();

            if (email.isEmpty() && phone.isEmpty()) {
                statusLabel.setTextFill(Color.RED);
                statusLabel.setText("Please enter Email or Phone.");
                return;
            }

            statusLabel.setTextFill(Color.BLUE);
            statusLabel.setText("Searching active tables...");
            btSearch.setDisable(true);

            // Listener for Guest Seated Code
            BistroClientGUI.client.getReservationCTRL().setOnGuestSeatedCodeListener(result -> {
                Platform.runLater(() -> {
                    btSearch.setDisable(false);
                    if (result == null || "NOT_FOUND".equals(result)) {
                        statusLabel.setTextFill(Color.RED);
                        statusLabel.setText("No active seated order found for today.");
                    } else {
                        txtConfirmCode.setText(result);
                        dialog.setResult(result);
                        dialog.close();
                    }
                });
            });

            // Call the NEW Guest method
            BistroClientGUI.client.getReservationCTRL().retrieveGuestSeatedCode(email, phone);
        });

        dialog.showAndWait();
    }

    private void showMemberSeatedSelectionDialog() {
        // Set Listener for Member List
        BistroClientGUI.client.getReservationCTRL().setOnMemberSeatedListListener(ordersList -> {
            if (ordersList == null || ordersList.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Active Tables");
                alert.setHeaderText("No Seated Orders Found");
                alert.setContentText("You do not appear to be checked in at any table right now.");
                alert.showAndWait();
                return;
            }

            Dialog<Order> dialog = new Dialog<>();
            dialog.setTitle("Select Active Table");
            dialog.setHeaderText("Which meal are you paying for?");

            ButtonType selectType = new ButtonType("Select", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(selectType, ButtonType.CANCEL);

            javafx.scene.control.ListView<Order> listView = new javafx.scene.control.ListView<>();
            listView.getItems().addAll(ordersList);
            listView.setPrefHeight(150);
            
            listView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Order item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        // Display Code + Table Info if available
                        setText(String.format("Code: %s | %d Guests | %s", 
                            item.getConfirmationCode(),
                            item.getDinersAmount(),
                            item.getOrderHour().toString()));
                    }
                }
            });

            dialog.getDialogPane().setContent(listView);

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

        // Call the NEW Member method
        BistroClientGUI.client.getReservationCTRL().askMemberSeatedReservations();
    }

    @FXML
    public void btnVerify(Event event) {
        String checkConfirmCode = txtConfirmCode.getText();
        if (BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable() != null) {
            String correctConfirmCode = BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable().getConfirmationCode();
            if (checkConfirmCode.equals(correctConfirmCode)) {
                BistroClientGUI.switchScreen(event, "clientCheckoutScreen", "cannot load client checkout screen");
            } else {
                BistroClientGUI.display(lblError,"Invalid Confirmation Code", Color.RED);
            }
        } else {
             // Fallback if allocated order is missing (should not happen in normal flow)
             BistroClientGUI.display(lblError,"No active session found. Please check in first.", Color.RED);
        }
    }

}
