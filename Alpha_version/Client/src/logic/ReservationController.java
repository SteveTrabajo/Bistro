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
import enums.OrderStatus;
import javafx.application.Platform;

public class ReservationController {
	
	//****************************** Static variables ******************************//
	
	public static final int SLOT_MINUTES = 15;//not used!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!?
	
	//****************************** Instance variables ******************************//
	
	private final BistroClient client;
	private String confirmationCode;
	private List<String> availableTimeSlots;
	private Consumer<List<String>> uiUpdateCallback;
	private List<Object> tempReservationData=new ArrayList<>();
	
	private Consumer<Order> orderLoadedCallback;
	private Consumer<List<String>> availableSlotsCallback;
	private Consumer<Boolean> updateResultCallback;
	private Consumer<Boolean> cancelResultCallback;
	private Order orderReady;
	
	private Consumer<List<Order>> allReservationsCallback;
    private Consumer<String> onCodeRetrieveResult;
	
	//******************************** Constructors ***********************************//
	
	public ReservationController(BistroClient client) {
		this.client = client;
		this.confirmationCode = "";
		this.availableTimeSlots = new ArrayList<>();
	}
	
	//******************************** Getters, Setters and Listeners ***********************************//
		
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
            Platform.runLater(() -> {
                uiUpdateCallback.accept(slots);
            });
        }
    }

	public List<String> getAvailableTimeSlots() {
		return availableTimeSlots;
	}
	
	public void setAvailableTimeSlotsListener(Consumer<List<String>> callback) {
		this.availableSlotsCallback = callback;
	}
		
	public void setLoadedOrder(Order order) {
		if (orderLoadedCallback != null) {
			Platform.runLater(() -> {
				orderLoadedCallback.accept(order);
			});
		}
	}
	
	public void setOrderLoadedListener(Consumer<Order> callback) {
		this.orderLoadedCallback = callback;
	}
	
	public Order getReadyUserReservation() {
		return orderReady;
	}
	
	public void setReadyUserReservation(Order orderReady) {
		this.orderReady = orderReady;
	}
	
	public void setUpdateListener(Consumer<Boolean> callback) {
		this.updateResultCallback = callback;
	}
	
	public void setCancelListener(Consumer<Boolean> callback) {
		this.cancelResultCallback = callback;
	}
	
	// Set by the Client when Server replies with REPLY_GET_ORDER_OK
	
	//******************************** Instance Methods ***********************************//
	/**
	 * Asks the server for available hours based on date and party size.
	 * Matches Api.ASK_ORDER_AVAILABLE_HOURS
	 */
	public void askAvailableHours(LocalDate date, int diners) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("date", date);
        requestData.put("dinersAmount", diners);
        client.handleMessageFromClientUI(new Message(Api.ASK_ORDER_AVAILABLE_HOURS, requestData));
    }
	
	/**
	 * Sends the reservation request to the server.
	 */
	public void createNewReservation(LocalDate date, String selectedTimeSlot, int diners) {
		LocalTime time = LocalTime.parse(selectedTimeSlot);
		tempReservationData.clear();
		tempReservationData.add(date);
		tempReservationData.add(diners);
		tempReservationData.add(time);
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION, tempReservationData));
	}
	
	public void updateReservation(Order order) {
		client.handleMessageFromClientUI(new Message(Api.ASK_UPDATE_RESERVATION, order));
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
		return orderReady.getStatus() == OrderStatus.COMPLETED;
	}
	

	public void askReservationsByDate(LocalDate date) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_RESERVATIONS_BY_DATE, date));
	}

    public void setAllReservationsListener(Consumer<List<Order>> callback) {
        this.allReservationsCallback = callback;
    }

    public void setOnConfirmationCodeRetrieveResult(Consumer<String> listener) {
        this.onCodeRetrieveResult = listener;
    }

    public void handleForgotConfirmationCodeResponse(String result) {
        if (onCodeRetrieveResult != null) {
            Platform.runLater(() -> {
                onCodeRetrieveResult.accept(result);
                onCodeRetrieveResult = null;
            });
        }
    }

	public void retrieveConfirmationCode(String email, String phoneNum) {
			    Map<String, String> requestData = new HashMap<>();
	    requestData.put("email", email);
	    requestData.put("phoneNum", phoneNum);
	    client.handleMessageFromClientUI(new Message(Api.ASK_FORGOT_CONFIRMATION_CODE, requestData));
		
	}

	public boolean hasActiveReservation() {
		Order userOrder = client.getReservationCTRL().getReadyUserReservation();
		if (userOrder != null) {
			return userOrder.getStatus() == OrderStatus.PENDING || userOrder.getStatus() == OrderStatus.NOTIFIED||
					userOrder.getStatus() == OrderStatus.SEATED;
		}
		return false;
	}
}
