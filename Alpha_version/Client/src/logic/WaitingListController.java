package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import comms.Api;
import comms.Message;
import entities.Order;
import enums.OrderStatus;

public class WaitingListController {
	
	//****************************** Instance variables ******************************//
	private final BistroClient client;
	private boolean userOnWaitingList = false;
	private boolean leaveWaitingListSuccess = false;
	private boolean skipWaitingListJoin = false;
	private Map<LocalDate, TreeMap<LocalTime, Order>> waitingList;
	
	private Consumer<Map<String, Map<String, Order>>> waitingListUpdateCallback;
	//******************************** Constructors ***********************************//
	public WaitingListController(BistroClient client) {
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
	public void setskipWaitingListJoin(boolean skipWaitingListJoin) {
		this.skipWaitingListJoin = skipWaitingListJoin;
	}
	public boolean getskipWaitingListJoin() {
		return skipWaitingListJoin;
	}
	//********************************Instance Methods ***********************************//
	
	public void askUserOnWaitingList(int userID) { // verficationID can be memberID or phone number/email for guest
		client.handleMessageFromClientUI(new Message(Api.ASK_IS_IN_WAITLIST, userID));
		return;
	}
	public boolean isUserOnWaitingList() {
		if (client.getReservationCTRL().getReadyUserReservation() == null) {
			return false;
		}
		OrderStatus status = client.getReservationCTRL().getReadyUserReservation().getStatus();
		return status == OrderStatus.WAITING_LIST;
		
	}
	public void joinWaitingList( int dinersAmount) {
		Map<String, Object> req = new HashMap<>();
        req.put("diners", dinersAmount);

		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_JOIN,req));
	}
	public Object setCurrentWaitingOrder(Order waitingOrder) {
		// TODO Auto-generated method stub
		return null;
	}
	public void leaveWaitingList() {
		client.handleMessageFromClientUI(new Message(Api.ASK_WAITING_LIST_LEAVE, null));
		
	}
	public boolean isLeaveWaitingListSuccess() {
		if (client.getReservationCTRL().getReadyUserReservation() == null) {
			return true;
		}
		OrderStatus status = client.getReservationCTRL().getReadyUserReservation().getStatus();
		return status != OrderStatus.WAITING_LIST;
	}

	//******************************** Message Handlers ***********************************//
	
	public void handleMessageFromUI(Message msg) {
        switch (msg.getId()) {
            case Api.REPLY_WAITING_LIST_JOIN_OK:
                this.userOnWaitingList = true;
                break;
            case Api.REPLY_WAITING_LIST_JOIN_FAIL:
                this.userOnWaitingList = false;
                break;
            case Api.REPLY_WAITING_LIST_LEAVE_OK:
                this.leaveWaitingListSuccess = true;
                this.userOnWaitingList = false;
                break;
        }
    }

}
