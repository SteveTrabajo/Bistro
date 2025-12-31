package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import java.util.List;
import logic.BistroClientGUI;

public class clientCheckoutScreen {
    @FXML
    private Label LabelUserBenefits;
    @FXML 
    private TextField txtAmountToPay;
    @FXML
    private Label summarySubtotal;
    @FXML
    private Label summarySubTax;
    @FXML
    private Label summaryDiscount;
    @FXML
    private Button btnPay;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label taxLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private TableView<Object> billTable;
    @FXML
    private Button btnBack;
    private double finalAmount = 0.0;
    private double minValue = 0.0;
	
	// ======================== Initialization ========================
	@FXML
	public void initialize() {
		// Fetch data from Controller
		Object orderItems = BistroClientGUI.client.getPaymentCTRL().getOrderItems();
		double subtotal = BistroClientGUI.client.getPaymentCTRL().getPaymentAmount();
		double tax = BistroClientGUI.client.getPaymentCTRL().calculateTax(subtotal);
		double discount = 0;
		
		// Check if user is a MEMBER for discount benefits
		if (BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserType().name().equals("MEMBER")) {
			LabelUserBenefits.setStyle("-fx-text-fill: green;");
			LabelUserBenefits.setText("Discount Applied");
			discount = BistroClientGUI.client.getPaymentCTRL().calculateDiscount(subtotal);
			summaryDiscount.setText(String.format("-%.2f", discount));
		} else {
			LabelUserBenefits.setStyle("-fx-text-fill: red;");
			LabelUserBenefits.setText("Sorry, no benefits for you :(");
			summaryDiscount.setText("0.00");
		}
		
		double total = subtotal + tax - discount;
		
		// Setup UI Labels with formatted prices
		summarySubtotal.setText(String.format("%.2f", subtotal));
		summarySubTax.setText(String.format("%.2f", tax));
		subtotalLabel.setText(String.format("%.2f", subtotal));
		taxLabel.setText(String.format("%.2f", tax));
		discountLabel.setText(String.format("-%.2f", discount));
		totalLabel.setText(String.format("%.2f", total));
		
		// Setup Table with order items
		billTable.getItems().setAll(orderItems);
		
		// Initialize payment values
		minValue = total;
		finalAmount = total;
		txtAmountToPay.setText(String.format("%.2f", total));
		
		// Setup Text Field Listeners
		setupTextFieldListeners();
	}
	
	// ======================== Text Field Listeners ========================
	/**
	 * Sets up listeners for the payment amount text field:
	 * 1. textProperty: Validates format and updates finalAmount in real-time
	 * 2. focusedProperty: Enforces minimum value when user leaves the field
	 */
	private void setupTextFieldListeners() {
		// 1. Text Property Listener: Updates the variable REAL-TIME while typing
		txtAmountToPay.textProperty().addListener((obs, oldVal, newVal) -> {
			// Reject invalid characters (allow only digits and one decimal point with max 2 decimals)
			if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
				txtAmountToPay.setText(oldVal);
				return;
			}
			
			// Update finalAmount variable immediately
			if (!newVal.isEmpty()) {
				try {
					finalAmount = Double.parseDouble(newVal);
				} catch (NumberFormatException e) {
					finalAmount = 0.0;
				}
			} else {
				finalAmount = 0.0;
			}
		});
		
		// 2. Focus Listener: Corrects the UI and enforces minimum when user leaves the field
		txtAmountToPay.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				enforceMinimumValue();
			}
		});
	}

	// ======================== Validation Methods ========================
	/**
	 * Ensures the payment amount is not below the minimum (base price).
	 * If finalAmount is below minValue, it resets to minValue.
	 */
	private void enforceMinimumValue() {
		if (finalAmount < minValue) {
			finalAmount = minValue;
		}
		// Format the text field to show the corrected/final value with 2 decimal places
		txtAmountToPay.setText(String.format("%.2f", finalAmount));
	}
	
	// ======================== Button Actions ========================
	/**
	 * Handles the Pay button click event.
	 * Validates the amount one last time and processes the payment.
	 */
	@FXML
	public void btnPay(Event event) {
		// Force validation one last time (in case user clicked Pay without leaving the text field)
		enforceMinimumValue();
		// Get confirmation code and set payment amount
		BistroClientGUI.client.getPaymentCTRL().setPaymentAmount(finalAmount);//TODO: paymentCTRL create it
		BistroClientGUI.client.getPaymentCTRL().checkpaymentSuccess(finalAmount);
		// Process Payment
		if (BistroClientGUI.client.getPaymentCTRL().processPaymentCompleted()) {
			BistroClientGUI.switchScreen(event, "clientCheckoutSuccessScreen.fxml", "Payment Successful");
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR, "Payment failed! Please try again.", null);
			alert.setTitle("Payment Error");
			alert.setHeaderText("Transaction Failed");
			alert.showAndWait();
		}
	}
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen.fxml", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}