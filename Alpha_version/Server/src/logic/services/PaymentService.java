package logic.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entities.Bill;
import entities.Item;
import entities.User;
import enums.EndTableSessionType;
import enums.OrderStatus;
import enums.UserType;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;
import logic.services.payment_simulator.MockPaymentGateway;
import logic.services.payment_simulator.PaymentGateway;

public class PaymentService {

    private final BistroDataBase_Controller dbController;
    private final ServerLogger logger;
    private final TableService tableService;
    private final PaymentGateway paymentGateway;

    public PaymentService(BistroDataBase_Controller dbController, ServerLogger logger,TableService tableService) {
        this.dbController = dbController;
        this.logger = logger;
        // Initialize with the Mock gateway for now. 
        // In the future, this can be swapped for a RealPaymentGateway.
        this.paymentGateway = new MockPaymentGateway();
        this.tableService = tableService;
    }

    /**
     * Calculates the total bill amount including tax and discounts.
     * @param items The list of items ordered.
     * @param requester The user requesting the bill (for member discounts).
     * @return The final total amount as a double.
     */
    public double calculateTotal(List<Item> items, User requester) {
        double total = 0.0;
        for (Item item : items) {
        	total += item.getPrice() * item.getQuantity();
        }

        // Apply 10% discount for MEMBERS
        if (requester.getUserType() == UserType.MEMBER) {
            total = total * 0.90; 
        }

        // Add 18% Tax/VAT
        return total * 1.18;
    }

    /**
     * Processes a credit card payment for a specific bill.
     * @param billId The ID of the bill to pay.
     * @param amount The amount to charge.
     * @param creditCardToken The dummy token (or real token in the future).
     * @return true if payment succeeded, false otherwise.
     */
    public boolean processCreditCardPayment(int billId, double amount, String creditCardToken) {
        Bill bill = dbController.getBillById(billId);
        if (bill == null) {
            logger.log("[ERROR] Bill not found: " + billId);
            return false;
        }

        if ("PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }

        String transactionId = paymentGateway.processPayment(amount, creditCardToken);

        if (transactionId != null) {
            dbController.markBillAsPaid(billId, "CREDIT", transactionId);
            logger.log("[SUCCESS] Bill " + billId + " paid. Transaction Ref: " + transactionId);
            return true;
        } else {
            logger.log("[FAILURE] Payment failed for Bill " + billId);
            return false;
        }
    }


    /**
     * Processes a manual payment (Cash).
     */
    public boolean processCashPayment(int billId, double amount) {
        Bill bill = dbController.getBillById(billId);
        if (bill == null) return false;

        if ("PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }

        dbController.markBillAsPaid(billId, "CASH", null);
        logger.log("[SUCCESS] Bill " + billId + " paid via CASH.");
        return true;
    }

    
    /**
     * Finds the bill ID linked to a specific Order Number.
     */
    public Integer getBillIdByOrderNumber(int orderNumber) {
        return dbController.getBillIdByOrderNumber(orderNumber);
    }

    /**
     * Retrieves the full Bill object by its ID.
     */
    public Bill getBillById(Integer billId) {
        if (billId == null) return null;
        return dbController.getBillById(billId);
    }

    /**
     * Gets all UNPAID bills associated with a specific user.
     */
    public List<Bill> getPendingBillsForUser() {
        return dbController.getPendingBillsByUserId();
    }

    
	/**
	 * method to be called when payment is completed for an order
	 * 
	 * @param orderNumber
	 */
    public boolean onPaymentCompleted(int orderNumber) {
    	//close table session when payment is completed
        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
        dbController.closeTableSessionForOrder(orderNumber, EndTableSessionType.PAID);
        dbController.updateOrderStatusByOrderNumber(orderNumber, OrderStatus.COMPLETED); //set order status to null after payment completion
        //case no active table session found
        if (tableNum == null) {
            logger.log("[WARN] Payment completed but no active table session found for order " + orderNumber);
            return false;
        }	       
        return tableService.tableFreed(tableNum); //when table is freed, try to seat WAITLIST/RESERVATION order if possible
    }
    
   

	
	//***************************************** Checkout Items List for Bill ID *****************************************//
	public List<Item> createRandomBillItems(int dinersAmount) {

	    List<Item> items = new ArrayList<>();
	    Random random = new Random();

	    if (dinersAmount <= 0) {
	        dinersAmount = 1;
	    }

	    Item[] menu = {
	        new Item(1, "Classic Burger", 55.0, 0),
	        new Item(2, "Margherita Pizza", 45.0, 0),
	        new Item(3, "Caesar Salad", 38.0, 0),
	        new Item(4, "Coca Cola", 12.0, 0),
	        new Item(5, "French Fries", 18.0, 0)
	    };
	    // Randomly select items and quantities
	    for (int i = 0; i < menu.length; i++) {
	        int quantity = random.nextInt(dinersAmount + 1);
	        if (quantity > 0) {
	            Item orderedItem = new Item(
	                menu[i].getItemId(),
	                menu[i].getName(),
	                menu[i].getPrice(),
	                quantity
	            );
	            items.add(orderedItem);
	        }
	    }
	    if (items.isEmpty()) { // Ensure at least one item is ordered
	        Item fallback = menu[random.nextInt(menu.length)];
	        items.add(new Item(
	            fallback.getItemId(),
	            fallback.getName(),
	            fallback.getPrice(),
	            1
	        ));
	    }
	    return items;
	}


	
	
	
}