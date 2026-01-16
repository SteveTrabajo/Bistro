package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import comms.Api;
import comms.Message;
import entities.Order;
import enums.OrderStatus;
import enums.OrderType;
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
	public boolean clearWaitingListController() {
		System.out.println("Clearing Waiting List Controller");
		this.orderWaitListDTO = null;
		System.out.println("Order DTO cleared");
		this.canSeatImmediately = false;
		System.out.println("Can Seat Immediately cleared");
		this.userOnWaitingList = false;
		System.out.println("User On Waiting List cleared");
		this.leaveWaitingListSuccess = false;
		System.out.println("Leave Waiting List Success cleared");
		this.estimatedWaitTimeMinutes = 0;
		System.out.println("Estimated Wait Time cleared");
		this.waitingList.clear();
		System.out.println("Waiting List Controller cleared");
		return true;
	}
	
	public Order getOrderWaitListDTO() {
		return orderWaitListDTO;
	}
	
	public void setOrderWaitListDTO(Order orderWaitListDTO) {
		this.orderWaitListDTO = orderWaitListDTO;
	}
	
	public OrderStatus getOrderStatus() {
		if (this.orderWaitListDTO != null) {
			return this.orderWaitListDTO.getStatus();
		}
		return null;
	}
	
	
	public ArrayList<Order> getWaitingList() {
		return waitingList;
	}
	
	public void setWaitingListPanelController(WaitingListPanel panel) {
	    this.waitingListPanelController = panel;
	}
	
	public void setWaitingList(ArrayList<Order> list) {
		this.waitingList = list;
		if (this.waitingListPanelController != null) {
	        // Run on UI thread to be safe
	        Platform.runLater(() -> {
	            this.waitingListPanelController.updateListFromServer(list);
	        });
	    }
	}
	
	public long getEstimatedWaitTimeMinutes() {
		return estimatedWaitTimeMinutes;
	}
	
	public void setEstimatedWaitTimeMinutes(long estimatedWaitTimeMinutes) {
		this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
	}
	
	
	public void setCanSeatImmediately(boolean canSeatImmediately) {
		this.canSeatImmediately = canSeatImmediately;
	}
	
	public boolean getcanSeatImmediately() {
		return this.canSeatImmediately;
	}
	
	public void setUserOnWaitingList(boolean status) {
		this.userOnWaitingList = status;
	}
	
	public boolean isUserOnWaitingList() {
	    return userOnWaitingList;
	}
	
	public void setLeaveWaitingListSuccess(boolean status) {
		this.leaveWaitingListSuccess = status;
	}
	
	public boolean isLeaveWaitingListSuccess() {
		return leaveWaitingListSuccess;
	}
    
    /**
     * Links the Staff GUI controller so it can receive updates.
     */
    public void setGuiController(WaitingListPanel guiController) {
        this.waitingListPanelController = guiController;
    }

	// ******************************** Instance Methods (Requests) ***********************************
	
	public void askUserOnWaitingList(int userID) { 
		client.handleMessageFromClientUI(new Message(Api.ASK_IS_IN_WAITLIST, userID));
	}

	public void checkWaitingListAvailability(int dinersAmount) {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_CHECK_AVAILABILITY, dinersAmount));
	}
	
	public void joinWaitingList(int diners, int waitTimeMinutes) {
	    Map<String, Object> details = new HashMap<>();
	    details.put("diners", diners);
	    details.put("waitTime", waitTimeMinutes);
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_JOIN, details));
	}

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

    
    public void addWalkIn(Map<String, Object> details) {
        client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_ADD_WALKIN, details));
    }
}