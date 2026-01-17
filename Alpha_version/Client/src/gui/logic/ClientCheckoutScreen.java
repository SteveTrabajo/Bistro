package gui.logic;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;
import java.util.ArrayList;
import java.util.List;
import entities.Item;
import logic.BistroClientGUI;

/**
 * Controller class for the Client Checkout Screen.
 * Manages the display and processing of the client's bill and payment.
 */
public class ClientCheckoutScreen {
	//*********************** FXML Components ***********************//
	@FXML
	private StackPane mainPane;
	@FXML 
	private Button btnPay;
	@FXML 
	private Button btnBack;
	@FXML 
	private TextField txtAmountToPay;
    @FXML 
    private Label LabelUserBenefits;
    @FXML 
    private Label summarySubtotal;
    @FXML 
    private Label summarySubTax;
    @FXML 
    private Label summaryDiscount;
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
    
    private double finalAmount = 0.0;
    private double minValue = 0.0;
    private List<Item> billItems = new ArrayList<>();

    //***************************** Instance Methods *****************************//
    
    /**
     * Initializes the controller class. This method is automatically called
     * after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        colItem.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        billTable.setItems(FXCollections.observableArrayList());
        billTable.setPlaceholder(new Label("Loading bill items from server..."));
        btnPay.setDisable(true);
        setupTextFieldListeners();
        int diners= BistroClientGUI.client.getTableCTRL().getUserAllocatedOrderForTable().getDinersAmount();
		TaskRunner.run(mainPane, () -> {
			BistroClientGUI.client.getPaymentCTRL().askBillItemsList(diners);
		}, () -> {
			List<Item> itemsFromServer = BistroClientGUI.client.getPaymentCTRL().getBillItemsList();
			if(itemsFromServer != null) {
				onBillItemsReceived(itemsFromServer);
			}
		});
    }

    /**
     * Handles the reception of bill items from the server.
     * @param itemsFromServer
     */
    private void onBillItemsReceived(List<Item> itemsFromServer) {
        Platform.runLater(() -> {
            if (itemsFromServer == null || itemsFromServer.isEmpty()) {
                billTable.setPlaceholder(new Label("No items found for this bill."));
                btnPay.setDisable(true);
                resetTotalsToZero();
                return;
            }
            this.billItems = new ArrayList<>(itemsFromServer);
            populateBill(this.billItems);
            btnPay.setDisable(false);
        });
    }
    
    /**
     * Populates the bill table and summary labels with the provided items.
     * @param items
     */
	private void populateBill(List<Item> items) {
		// Table
		billTable.setItems(FXCollections.observableArrayList(items));
		// Subtotal
		double subtotal = 0.0;
		for (Item it : items) {
			subtotal += it.getTotal();
		}
		double tax = BistroClientGUI.client.getPaymentCTRL().calculateTax(subtotal);
		// Discount
		double discount = 0.0;
		boolean isMember = BistroClientGUI.client.getUserCTRL().getLoggedInUser().getUserType().name().equals("MEMBER");
		if (isMember) {
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
		// Labels
		summarySubtotal.setText(String.format("%.2f", subtotal));
		summarySubTax.setText(String.format("%.2f", tax));
		subtotalLabel.setText(String.format("%.2f", subtotal));
		taxLabel.setText(String.format("%.2f", tax));
		discountLabel.setText(String.format("-%.2f", discount));
		totalLabel.setText(String.format("%.2f", total));
		// Payment values
		minValue = total;
		finalAmount = total;
		txtAmountToPay.setText(String.format("%.2f", total));
	}

	/**
	 * Resets all total labels and payment values to zero.
	 */
    private void resetTotalsToZero() {
        summarySubtotal.setText("0.00");
        summarySubTax.setText("0.00");
        summaryDiscount.setText("0.00");
        subtotalLabel.setText("0.00");
        taxLabel.setText("0.00");
        discountLabel.setText("0.00");
        totalLabel.setText("0.00");
        minValue = 0.0;
        finalAmount = 0.0;
        txtAmountToPay.setText("0.00");
    }
    
    /**
     * Sets up listeners for the amount to pay text field to ensure valid input.
     */
    private void setupTextFieldListeners() {
        txtAmountToPay.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                txtAmountToPay.setText(oldVal);
                return;
            }
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
        txtAmountToPay.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) enforceMinimumValue();
        });
    }

    /**
	 * Ensures that the final amount to pay is not less than the minimum value.
	 */
    private void enforceMinimumValue() {
        if (finalAmount < minValue) {
            finalAmount = minValue;
        }
        txtAmountToPay.setText(String.format("%.2f", finalAmount));
    }

    /**
	 * Handles the payment button action.
	 * @param event
	 */
    @FXML
    public void btnPay(Event event) {
        enforceMinimumValue();

        BistroClientGUI.client.getPaymentCTRL().setPaymentAmount(finalAmount);

        BistroClientGUI.client.getPaymentCTRL().checkpaymentSuccess(billItems);
        
        if (BistroClientGUI.client.getPaymentCTRL().processPaymentCompleted()) {
            BistroClientGUI.switchScreen(event, "clientCheckoutSuccessScreen", "Payment Successful");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Payment failed! Please try again.", null);
            alert.setTitle("Payment Error");
            alert.setHeaderText("Transaction Failed");
            alert.showAndWait();
        }
    }
    /**
     * Handles the back button action.
     * @param event
     */
    public void btnBack(Event event) {
        try {
            BistroClientGUI.switchScreen(event, "clientDashboardScreen", "Client back error messege");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// End of ClientCheckoutScreen.java