package gui.logic;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entities.Item;
import logic.BistroClientGUI;

public class ClientCheckoutScreen {
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
    private TableView<Item> billTable;
    @FXML 
    private TableColumn<Item, String> colItem;
    @FXML 
    private TableColumn<Item, Integer> colQty;
    @FXML 
    private TableColumn<Item, Double> colPrice;
    @FXML 
    private TableColumn<Item, Double> colTotal;
    @FXML
    private Button btnBack;
    private double finalAmount = 0.0;
    private double minValue = 0.0;
    private static final double PRICE_BURGER = 55.00;
    private static final double PRICE_PIZZA = 45.00;
    private static final double PRICE_SALAD = 38.00;
    private static final double PRICE_COLA = 12.00;
    private static final double PRICE_FRIES = 18.00;
    private List<Item> randomItems = new ArrayList<>();
    // ======================== Initialization ========================
	@FXML
	public void initialize() {
		colItem.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity")); 
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
		int DinersAmount = BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable().getDinersAmount();
		Random random = new Random();
        Item[] menu = {
                new Item(1, "Classic Burger", PRICE_BURGER, 0),
                new Item(2, "Margherita Pizza", PRICE_PIZZA, 0),
                new Item(3, "Caesar Salad", PRICE_SALAD, 0),
                new Item(4, "Coca Cola", PRICE_COLA, 0),
                new Item(5, "French Fries", PRICE_FRIES, 0)
            };
        	double subtotal = 0;
     // Each diner is likely to order 1-3 items
        for (Item menuItem : menu) {
            // Logic: Each item has a chance to be ordered based on diner count
            int qty = random.nextInt(DinersAmount + 1); 
            
            if (qty > 0) {
                // Create the final item with the randomized quantity
                Item orderedItem = new Item(
                    menuItem.getItemId(), 
                    menuItem.getName(), 
                    menuItem.getPrice(), 
                    qty
                );
                randomItems.add(orderedItem);
                subtotal += orderedItem.getTotal();
            }
        }
		// Fetch data from Controller
        double discount = 0;
		double tax = BistroClientGUI.client.getPaymentCTRL().calculateTax(subtotal);
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
		billTable.getItems().setAll(randomItems);
		
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
		BistroClientGUI.client.getPaymentCTRL().setPaymentAmount(finalAmount);//TODO
		BistroClientGUI.client.getPaymentCTRL().checkpaymentSuccess(randomItems);
		// Process Payment
		if (BistroClientGUI.client.getPaymentCTRL().processPaymentCompleted()) {
			BistroClientGUI.switchScreen(event, "clientCheckoutSuccessScreen", "Payment Successful");
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR, "Payment failed! Please try again.", null);
			alert.setTitle("Payment Error");
			alert.setHeaderText("Transaction Failed");
			alert.showAndWait();
		}
	}
	public void btnBack(Event event) {
		try {
			BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}