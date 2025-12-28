package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import comms.Api;
import comms.Message;
import entities.Order;
import enums.OrderStatus;

public class WaitingList_Controller {
	
	//****************************** Instance variables ******************************//
	private final BistroClient client;
	private Map<LocalDate, TreeMap<LocalTime, Order>> waitingList;
	//******************************** Constructors ***********************************//
	public WaitingList_Controller(BistroClient client) {
		this.client = client;
		this.waitingList = new TreeMap<>();
	}
	//******************************** Getter and Setters ***********************************//
	public Map<LocalDate, TreeMap<LocalTime, Order>> getWaitingList() {
		return waitingList;
	}
	
	public void setWaitingList(Map<LocalDate, TreeMap<LocalTime, Order>> waitingList) {
		this.waitingList = waitingList;
	}
	
	//********************************Instance Methods ***********************************//
	
	public void askUserOnWaitingList(String userID) { // verficationID can be memberID or phone number/email for guest
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITLIST_IS_USER_ON, userID));
		return;
	}
	public boolean isUserOnWaitingList() {
		OrderStatus stat = client.getReservationCTRL().getReayUserReservation().getStatus();
		
	}


	
}
