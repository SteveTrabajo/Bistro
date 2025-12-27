package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import comms.Message;
import entities.Order;

public class WaitingList_Controller {
	
	//****************************** Instance variables ******************************//
	private final BistroClient client;
	private Map<LocalDate, TreeMap<LocalTime, Order>> waitingList;
	private Order currentUserWaitingOrder;
	//******************************** Constructors ***********************************//
	public WaitingList_Controller(BistroClient client) {
		this.client = client;
		this.waitingList = new TreeMap<>();
		this.currentUserWaitingOrder = null;
	}
	//******************************** Getter and Setters ***********************************//
	
	public void setCurrentWaitingOrder(Order order) {
		this.currentUserWaitingOrder = order;
	}
	
	public Order getCurrentWaitingOrder() {
		return this.currentUserWaitingOrder;
	}
	
	
	//********************************Instance Methods ***********************************//
	
	public void askUserOnWaitingList(Object userVerificationID) { // verficationID can be memberID or phone number/email for guest
		client.handleMessageFromClientUI(new Message("ASK_USER_ON_WAITING", userVerificationID));
		return;
	}

	public boolean isUserOnWaitingList() {
		if (this.currentUserWaitingOrder == null) {
			return false;
		}
		return true;
	}
	
}
