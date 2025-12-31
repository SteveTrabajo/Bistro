package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import comms.Api;
import comms.Message;
import entities.Order;
import enums.DaysOfWeek;

public class ReservationController {
	
	//****************************** Static variables ******************************//
	
	public static final int SLOT_MINUTES = 15;
	
	//****************************** Instance variables ******************************//
	
	private final BistroClient client;
	private String confirmationCode;
	private List<String> availableTimeSlots;
	private Consumer<List<String>> uiUpdateCallback;
	private List<Object> tempReservationData=new ArrayList<>();
	
	private Consumer<Order> orderLoadedCallback;
	private Order orderReady;
	
	//******************************** Constructors ***********************************//
	
	public ReservationController(BistroClient client) {
		this.client = client;
		this.confirmationCode = "";
		this.availableTimeSlots = new ArrayList<>();
	}
	
	//******************************** Getters And Setters ***********************************//
	
	public String getConfirmationCode() {
		return confirmationCode;
	}
	
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}
	
	public void setUIUpdateListener(Consumer<List<String>> callback) {
        this.uiUpdateCallback = callback;
    }
	
    // Set by the Client when Server replies with REPLY_ORDER_AVAILABLE_HOURS_OK
	public void setAvailableTimeSlots(List<String> slots) {
        this.availableTimeSlots = slots;
        
        // Trigger the callback to update the screen!
        if (uiUpdateCallback != null) {
            javafx.application.Platform.runLater(() -> {
                uiUpdateCallback.accept(slots);
            });
        }
    }

	public List<String> getAvailableTimeSlots() {
		return availableTimeSlots;
	}
	
	public void setOrderLoadedListener(Consumer<Order> callback) {
		this.orderLoadedCallback = callback;
	}
	
	public void setLoadedOrder(Order order) {
		if (orderLoadedCallback != null) {
			javafx.application.Platform.runLater(() -> {
				orderLoadedCallback.accept(order);
			});
		}
	}
	
	public Order getReadyUserReservation() {
		return orderReady;
	}
	
	public void setReadyUserReservation(Order orderReady) {
		this.orderReady = orderReady;
	}
	
	// Set by the Client when Server replies with REPLY_GET_ORDER_OK
	
	//******************************** Instance Methods ***********************************//
	
	/*
	 * Asks the server for available hours based on date and party size.
	 * Matches Api.ASK_ORDER_AVAILABLE_HOURS
	 */
	public void askAvailableHours(LocalDate date, int diners) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("date", date);
        requestData.put("diners", diners);
        client.handleMessageFromClientUI(new Message(Api.ASK_ORDER_AVAILABLE_HOURS, requestData));
    }
	
	/*
	 * Sends the reservation request to the server.
	 */
	public void createNewReservation(LocalDate date, String selectedTimeSlot, int diners) {
		
		
		// Parse the time string (e.g., "17:00") to LocalTime for the entity
		LocalTime time = LocalTime.parse(selectedTimeSlot);
		
		tempReservationData.add(date);
		tempReservationData.add(time);
		tempReservationData.add(diners);
		
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION, tempReservationData));
	}
	public List<Object> getTempReservationData() {
		return tempReservationData;
	}
	public void deleteTempReservationData(List<Object> tempReservationData) {
		this.tempReservationData.removeAll(tempReservationData);
	}
			
	/*
	 * Checks if the provided confirmation code is correct by asking the server.
	 */
	public void CheckConfirmationCodeCorrect(String confirmationCode) {
        client.handleMessageFromClientUI(new Message(Api.ASK_CHECK_ORDER_EXISTS, confirmationCode));
    }
	
	public void askOrderDetails(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_ORDER, confirmationCode));
	}
	
	public void cancelReservation(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_CANCEL_RESERVATION, confirmationCode));
	}
	
	/*
	 * Checks if a user's reservation is ready (for waiting list flow).
	 */
	public boolean isUserReservationReady() {
		// Logic to check if a table is ready (for waiting list flow)
		return false;
	}
	
}
