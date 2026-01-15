package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import comms.Api;
import comms.Message;
import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;

public class ReservationController {
	
	//****************************** Instance variables ******************************//
	
	private final BistroClient client;
	private Order orderDTO;
	private List<String> availableTimeSlots;
	
	private Consumer<List<String>> uiUpdateCallback;
	
	private Consumer<Order> orderLoadedCallback;
	private Consumer<List<String>> availableSlotsCallback;
	private Consumer<Boolean> updateResultCallback;
	private Consumer<Boolean> cancelResultCallback;
	private Consumer<List<Order>> allReservationsCallback;
    private Consumer<String> onCodeRetrieveResult;
    private Consumer<List<LocalDate>> datesUpdateCallback;
    private BiConsumer<Boolean, String> checkInCallback; //like "consumer" but with two parameters
	
	//******************************** Constructors ***********************************//
	
	public ReservationController(BistroClient client) {
		this.client = client;
		this.availableTimeSlots = new ArrayList<>();
		this.orderDTO = null;
	}
	
	//******************************** Getters, Setters and Listeners ***********************************//
	
	public boolean clearReservationController() {
		this.orderDTO = null;
		this.availableTimeSlots.clear();
		return true;
	}
	
	
	public Order getOrderDTO() {
		return orderDTO;
	}
	
	public void setOrderDTO(Order orderDTO) {
		this.orderDTO = orderDTO;
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
	
	public void setUpdateListener(Consumer<Boolean> callback) {
		this.updateResultCallback = callback;
	}
	
	public void setCancelListener(Consumer<Boolean> callback) {
		this.cancelResultCallback = callback;
	}
	
	public void setCheckInListener(BiConsumer<Boolean, String> callback) {
        this.checkInCallback = callback;
    }
		
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
		List<Object> tempReservationData=new ArrayList<>();
		tempReservationData.clear();
		tempReservationData.add(date);
		tempReservationData.add(diners);
		tempReservationData.add(time);
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION, tempReservationData));
	}
	
	public void createReservationAsStaff(LocalDate date, LocalTime time, int diners, String customerType, String identifier, String customerName) {
		Map<String, Object> bookingData = new HashMap<>();
		bookingData.put("date", date);
		bookingData.put("time", time);
		bookingData.put("diners", diners);
		bookingData.put("customerType", customerType);
		// Identifier is either MemberID or Phone Number depending on type
		bookingData.put("identifier", identifier);
		bookingData.put("customerName", customerName);
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION_AS_STAFF, bookingData));
	}
	
	public void receiveStaffReservations(List<Order> orders) {
	    if (this.allReservationsCallback != null) {
	        this.allReservationsCallback.accept(orders);
	    }
	}
	
	public void updateReservation(Order order) {
		client.handleMessageFromClientUI(new Message(Api.ASK_UPDATE_RESERVATION, order));
	}

			
	/*
	 * Checks if the provided confirmation code is correct by asking the server.
	 */
	public void CheckConfirmationCodeCorrect(String confirmationCode) {
        client.handleMessageFromClientUI(new Message(Api.ASK_CHECK_ORDER_EXISTS, confirmationCode));
    }
	
	public void askClientOrderHistory() {
		client.handleMessageFromClientUI(new Message(Api.ASK_CLIENT_ORDER_HISTORY, null));
	}
	
	public void askOrderDetails(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_ORDER, confirmationCode));
	}
	
	public void cancelReservation(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_CANCEL_RESERVATION, confirmationCode));
	}
	
	public void notifyCancelResult(boolean success) {
		if (cancelResultCallback != null) {
			Platform.runLater(() -> cancelResultCallback.accept(success));
		}
	}
	
	public void seatCustomer(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_SEAT_CUSTOMER, confirmationCode));
	}
	
	/*
	 * Checks if a user's reservation is ready (for waiting list flow).
	 */
	public boolean isUserReservationReady() {
		return orderDTO.getStatus() == OrderStatus.COMPLETED;
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
		if (orderDTO == null) {
			return false;
		}
		return true;
	}
	/**
	 * Registers the listener for when the server returns valid dates.
	 */
	public void setDatesUpdateListener(Consumer<List<LocalDate>> callback) {
	    this.datesUpdateCallback = callback;
	}

	/**
	 * Called by BistroClient when the server sends back the list of dates.
	 * Triggers the UI update.
	 */
	public void setAvailableDates(List<LocalDate> dates) {
	    if (datesUpdateCallback != null) {
	        Platform.runLater(() -> {
	            datesUpdateCallback.accept(dates);
	        });
	    }
	}

	/**
	 * Sends a request to the server to get available dates for a specific number of diners.
	 * Note: You must add Api.ASK_AVAILABLE_DATES to your Api class.
	 */
	public void askAvailableDates(int diners) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_AVAILABLE_DATES, diners));
	}
	
	public void notifyCheckInResult(boolean success, String message) {
        if (checkInCallback != null) {
            Platform.runLater(() -> checkInCallback.accept(success, message));
        }
    }
	
}
