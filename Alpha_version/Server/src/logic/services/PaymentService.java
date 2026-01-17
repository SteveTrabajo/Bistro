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

/**
 * Handles all payment-related operations for the restaurant.
 * Supports credit card and cash payments, applies member discounts,
 * calculates totals with tax, and manages the order completion flow.
 * 
 * Currently uses a mock payment gateway for testing purposes.
 */
public class PaymentService {

	/** Database controller for all DB operations */
    private final BistroDataBase_Controller dbController;
    
    /** Logger for tracking service activity */
    private final ServerLogger logger;
    
    /** Service for table operations (needed to free tables after payment) */
    private final TableService tableService;
    
    /** Payment gateway for processing credit card payments (currently mock) */
    private final PaymentGateway paymentGateway;

    /**
     * Creates a new PaymentService with required dependencies.
     * Initializes with a mock payment gateway (swap for real one in production).
     * 
     * @param dbController database controller for DB access
     * @param logger server logger for logging events
     * @param tableService table service for freeing tables after payment
     */
    public PaymentService(BistroDataBase_Controller dbController, ServerLogger logger,TableService tableService) {
        this.dbController = dbController;
        this.logger = logger;
        // Initialize with the Mock gateway for now. 
        // In the future, this can be swapped for a RealPaymentGateway.
        this.paymentGateway = new MockPaymentGateway();
        this.tableService = tableService;
    }

    /**
     * Calculates the total bill amount with member discount and tax.
     * Members get 10% off, then 18% VAT is added to the total.
     * 
     * @param items list of ordered items
     * @param requester the user requesting the bill (for discount check)
     * @return the final total including tax
     */
    public double calculateTotal(List<Item> items, User requester) {
        double total = 0.0;
        for (Item item : items) {
        	total += item.getPrice() * item.getQuantity();
        }

        // TODO Do we want to calculate member discount before or after tax?
        // Apply 10% discount for MEMBERS
        if (requester.getUserType() == UserType.MEMBER) {
            total = total * 0.90; 
        }

        // Add 18% Tax/VAT
        return total * 1.18;
    }

    /**
     * Processes a credit card payment through the payment gateway.
     * Marks the bill as paid and completes the order if successful.
     * 
     * @param billId the bill ID to pay
     * @param amount the amount to charge
     * @param creditCardToken the payment token from the client
     * @return true if payment succeeded, false otherwise
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
            finalizeOrderPayment(billId);
            logger.log("[SUCCESS] Bill " + billId + " paid. Transaction Ref: " + transactionId);
            return true;
        } else {
            logger.log("[FAILURE] Payment failed for Bill " + billId);
            return false;
        }
    }


    /**
     * Processes a cash payment (no gateway needed, staff action).
     * Marks the bill as paid and completes the order.
     * 
     * @param billId the bill ID to pay
     * @param amount the cash amount received
     * @return true if successful, false otherwise
     */
    public boolean processCashPayment(int billId, double amount) {
        Bill bill = dbController.getBillById(billId);
        if (bill == null) return false;

        if ("PAID".equalsIgnoreCase(bill.getPaymentStatus())) {
            logger.log("[INFO] Bill " + billId + " is already paid.");
            return false;
        }

        dbController.markBillAsPaid(billId, "CASH", null);
        finalizeOrderPayment(billId);
        logger.log("[SUCCESS] Bill " + billId + " paid via CASH.");
        return true;
    }

    /**
	 * Finalizes the order after payment - closes table session and updates status.
	 * Called internally after successful payment processing.
	 * 
	 * @param billId the bill that was paid
	 */
    private void finalizeOrderPayment(int billId) {
        try {
            Integer orderNum = dbController.getOrderNumberByBillId(billId);
            if (orderNum != null) {
                onPaymentCompleted(orderNum);
            } else {
                logger.log("[WARN] Could not find Order Number for Bill ID: " + billId);
            }
        } catch (Exception e) {
            logger.log("[ERROR] Failed to finalize order/session for bill " + billId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Finds the bill ID linked to a specific order.
     * 
     * @param orderNumber the order number
     * @return the bill ID, or null if not found
     */
    public Integer getBillIdByOrderNumber(int orderNumber) {
        return dbController.getBillIdByOrderNumber(orderNumber);
    }

    /**
     * Gets a bill by its ID.
     * 
     * @param billId the bill ID
     * @return the Bill object, or null if not found
     */
    public Bill getBillById(Integer billId) {
        if (billId == null) return null;
        return dbController.getBillById(billId);
    }

    /**
     * Gets all unpaid bills in the system.
     * 
     * @return list of pending bills
     */
    public List<Bill> getPendingBillsForUser() {
        return dbController.getPendingBillsByUserId();
    }
    
    /**
     * Called when payment is completed for an order.
     * Closes the table session, marks order as COMPLETED,
     * and notifies waitlist customers about the freed table.
     * 
     * @param orderNumber the order that was paid
     * @return true if successful
     */
    public boolean onPaymentCompleted(int orderNumber) {
        Integer tableNum = dbController.getActiveTableNumByOrderNumber(orderNumber);
        dbController.closeTableSessionForOrder(orderNumber, EndTableSessionType.PAID);
        dbController.updateOrderStatusByOrderNumber(orderNumber, OrderStatus.COMPLETED); 

        if (tableNum == null) {
            logger.log("[INFO] Payment completed, but table session was already closed or not found for order " + orderNumber);
            return true; 
        }          
        
        if (tableService != null) {
            tableService.tableFreed(tableNum);
        }
        
        return true;
    }
   

	
	//***************************************** Checkout Items *****************************************//
	
	/**
	 * Creates a random list of menu items for demo/testing.
	 * Generates realistic orders based on party size.
	 * 
	 * @param dinersAmount number of people in the party
	 * @return list of randomly selected items
	 */
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