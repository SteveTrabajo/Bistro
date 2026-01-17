package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import comms.Api;
import comms.Message;
import entities.Order;
import enums.OrderStatus;
import gui.logic.staff.WaitingListPanel;
import javafx.application.Platform;

/*
 * This class represents the controller for waiting list operations in the BistroClient.
 */
public class WaitingListController {
	
	// ****************************** Instance variables ******************************
	private final BistroClient client;
	private Order orderWaitListDTO;
	// State variables
	private boolean canSeatImmediately = false;
	private boolean userOnWaitingList = false;
	private boolean leaveWaitingListSuccess = false;
	private long estimatedWaitTimeMinutes = 0;
	// Data Holders
	private ArrayList<Order> waitingList = new ArrayList<>();
	// GUI Reference (for refreshing the table)
	private WaitingListPanel waitingListPanelController; 

	// ******************************** Constructors ***********************************
	public WaitingListController(BistroClient client) {
		this.client = client;
	}

	// ******************************** Getters And Setters ***********************************
	/**
	 * Clears the waiting list controller state.
	 * @return
	 */
	public boolean clearWaitingListController() {
		this.orderWaitListDTO = null;
		this.canSeatImmediately = false;
		this.userOnWaitingList = false;
		this.leaveWaitingListSuccess = false;
		this.estimatedWaitTimeMinutes = 0;
		this.waitingList.clear();
		return true;
	}
	
	/**
	 * Gets the current Order DTO for the waiting list.
	 * @return Order DTO
	 */
	public Order getOrderWaitListDTO() {
		return orderWaitListDTO;
	}
	
	/**
	 * Sets the current Order DTO for the waiting list.
	 * @param orderWaitListDTO Order DTO
	 */
	public void setOrderWaitListDTO(Order orderWaitListDTO) {
		this.orderWaitListDTO = orderWaitListDTO;
	}
	
	/**
	 * Gets the status of the current order in the waiting list.
	 * @return OrderStatus
	 */
	public OrderStatus getOrderStatus() {
		if (this.orderWaitListDTO != null) {
			return this.orderWaitListDTO.getStatus();
		}
		return null;
	}
	
	/**
	 * Gets the current waiting list.
	 * @return ArrayList of Orders
	 */
	public ArrayList<Order> getWaitingList() {
		return waitingList;
	}
	
	/**
	 * Sets the waiting list panel controller for GUI updates.
	 * @param panel WaitingListPanel controller
	 */
	public void setWaitingListPanelController(WaitingListPanel panel) {
	    this.waitingListPanelController = panel;
	}
	
	/**
	 * Sets the current waiting list and updates the GUI if applicable.
	 * @param list ArrayList of Orders
	 */
	public void setWaitingList(ArrayList<Order> list) {
		this.waitingList = list;
		if (this.waitingListPanelController != null) {
	        // Run on UI thread to be safe
	        Platform.runLater(() -> {
	            this.waitingListPanelController.updateListFromServer(list);
	        });
	    }
	}
	
	/**
	 * Gets the estimated wait time in minutes.
	 * @return Estimated wait time in minutes
	 */
	public long getEstimatedWaitTimeMinutes() {
		return estimatedWaitTimeMinutes;
	}
	
	/**
	 * Sets the estimated wait time in minutes.
	 * @param estimatedWaitTimeMinutes Estimated wait time in minutes
	 */
	public void setEstimatedWaitTimeMinutes(long estimatedWaitTimeMinutes) {
		this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
	}
	
	/**
	 * Sets whether the user can be seated immediately.
	 * @param canSeatImmediately boolean indicating if the user can be seated immediately
	 */
	public void setCanSeatImmediately(boolean canSeatImmediately) {
		this.canSeatImmediately = canSeatImmediately;
	}
	
	/**
	 * Gets whether the user can be seated immediately.
	 * @return boolean indicating if the user can be seated immediately
	 */
	public boolean getcanSeatImmediately() {
		return this.canSeatImmediately;
	}
	
	/**
	 * Sets whether the user is on the waiting list.
	 * @param status boolean indicating if the user is on the waiting list
	 */
	public void setUserOnWaitingList(boolean status) {
		this.userOnWaitingList = status;
	}
	
	/**
	 * Gets whether the user is on the waiting list.
	 * @return boolean indicating if the user is on the waiting list
	 */
	public boolean isUserOnWaitingList() {
	    return userOnWaitingList;
	}
	
	/**
	 * Sets whether leaving the waiting list was successful.
	 * @param status boolean indicating if leaving the waiting list was successful
	 */
	public void setLeaveWaitingListSuccess(boolean status) {
		this.leaveWaitingListSuccess = status;
	}
	
	/**
	 * Gets whether leaving the waiting list was successful.
	 * @return boolean indicating if leaving the waiting list was successful
	 */
	public boolean isLeaveWaitingListSuccess() {
		return leaveWaitingListSuccess;
	}
    
    /**
     * Links the Staff GUI controller so it can receive updates.
     */
    public void setGuiController(WaitingListPanel guiController) {
        this.waitingListPanelController = guiController;
    }

	// ******************************** Instance Methods ***********************************
	
    /**
	 * Asks the server if the user is currently on the waiting list.
	 * @param userID ID of the user
	 */
	public void askUserOnWaitingList(int userID) { 
		client.handleMessageFromClientUI(new Message(Api.ASK_IS_IN_WAITLIST, userID));
	}

	/**
	 * Checks availability for the waiting list based on the number of diners.
	 * @param dinersAmount Number of diners
	 */
	public void checkWaitingListAvailability(int dinersAmount) {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_CHECK_AVAILABILITY, dinersAmount));
	}
	
	/**
	 * Joins the waiting list with the specified number of diners and wait time.
	 * @param diners Number of diners
	 * @param waitTimeMinutes Estimated wait time in minutes
	 */
	public void joinWaitingList(int diners, int waitTimeMinutes) {
	    Map<String, Object> details = new HashMap<>();
	    details.put("diners", diners);
	    details.put("waitTime", waitTimeMinutes);
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_JOIN, details));
	}

	/**
	 * Leaves the waiting list using the current order's confirmation code.
	 */
	public void leaveWaitingList() {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_LEAVE, this.orderWaitListDTO.getConfirmationCode()));
	}
	
	/**
	 * Staff Method: Removes a specific customer from the list.
	 */
	public void removeFromWaitingList(String confirmationCode) {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_LEAVE_STAFF,confirmationCode));
	}

    /**
     * Staff Method: Requests the full waiting list from the server.
     */
    public void askWaitingList() {
        client.handleMessageFromClientUI(new Message(Api.ASK_GET_WAITING_LIST, null));
    }

    /**
	 * Staff Method: Adds a walk-in customer to the waiting list.
	 * @param details Map containing walk-in customer details
	 */
    public void addWalkIn(Map<String, Object> details) {
        client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_ADD_WALKIN, details));
    }
}
// End of WaitingListController.java