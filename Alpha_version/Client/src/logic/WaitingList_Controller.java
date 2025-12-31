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
	
	public void askUserOnWaitingList(int userID) { // verficationID can be memberID or phone number/email for guest
		client.handleMessageFromClientUI(new Message(Api.ASK_IS_IN_WAITLIST, userID));
		return;
	}
	public boolean isUserOnWaitingList() {
		OrderStatus status = client.getReservationCTRL().getReadyUserReservation().getStatus();
		return status == OrderStatus.WAITING_LIST;
		
	}
	public void joinWaitingList( int dinersAmount) {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_JOIN,dinersAmount));
	}
	public Object setCurrentWaitingOrder(Order waitingOrder) {
		// TODO Auto-generated method stub
		return null;
	}
	public void leaveWaitingList() {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_LEAVE, null));
		
	}


	
}
