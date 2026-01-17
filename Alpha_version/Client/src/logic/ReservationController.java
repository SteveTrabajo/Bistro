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
import dto.WeeklyHour;
import entities.Order;
import enums.OrderStatus;
import javafx.application.Platform;

/**
 * ReservationController handles reservation-related operations and communication with the server.
 */
public class ReservationController {
	
	//****************************** Instance variables ******************************//
	
	private final BistroClient client;
	private Order orderDTO;
	private List<String> availableTimeSlots;
	private List<WeeklyHour> weeklyHours;
	private Consumer<List<String>> uiUpdateCallback;
	private Consumer<Order> orderLoadedCallback;
	private Consumer<List<String>> availableSlotsCallback;
	private Consumer<Boolean> updateResultCallback;
	private Consumer<Boolean> cancelResultCallback;
	private Consumer<List<Order>> allReservationsCallback;
    private Consumer<String> onCodeRetrieveResult;
    private Consumer<List<LocalDate>> datesUpdateCallback;
    private BiConsumer<Boolean, String> checkInCallback; //like "consumer" but with two parameters
    private Consumer<List<Order>> onMemberReservationsListListener;
    private Consumer<List<Order>> onMemberSeatedListListener;
    private Consumer<String> onGuestSeatedCodeListener;
	
	//******************************** Constructors ***********************************//
	
	public ReservationController(BistroClient client) {
		this.client = client;
		this.availableTimeSlots = new ArrayList<>();
		this.orderDTO = null;
	}
	
	//******************************** Getters, Setters and Listeners ***********************************//
	
	/**
	 * Clears the reservation controller's data.
	 * @return true if cleared successfully
	 */
	public boolean clearReservationController() {
		this.orderDTO = null;
		this.availableTimeSlots.clear();
		return true;
	}
	
	/**
	 * Gets the current orderDTO.
	 * @return The current Order DTO
	 */
	public Order getOrderDTO() {
		return orderDTO;
	}
	
	/**
	 * Sets the current orderDTO.
	 * @param orderDTO The Order DTO to set
	 */
	public void setOrderDTO(Order orderDTO) {
		this.orderDTO = orderDTO;
	}
	
	/** 
	 * Set by the Client when Server replies with REPLY_ORDER_AVAILABLE_HOURS_OK
	 * Registers a callback to update the UI when available time slots are received.
	 * @param callback The callback function to update the UI
	 */
	public void setUIUpdateListener(Consumer<List<String>> callback) {
        this.uiUpdateCallback = callback;
    }
	
    /**
	 * Called by BistroClient when the server sends back available time slots.
	 * Triggers the UI update.
	 * @param slots The list of available time slots
	 */
	public void setAvailableTimeSlots(List<String> slots) {
        this.availableTimeSlots = slots;
        // Trigger the callback to update the screen!
        if (uiUpdateCallback != null) {
            Platform.runLater(() -> {
                uiUpdateCallback.accept(slots);
            });
        }
    }
	
	/**
	 * Gets the available time slots.
	 * @return The list of available time slots
	 */
	public List<String> getAvailableTimeSlots() {
		return availableTimeSlots;
	}
	
	/**
	 * Set by the Client when Server replies with REPLY_ORDER_AVAILABLE_HOURS_OK
	 * Registers a callback to update the UI when available time slots are received.
	 * @param callback The callback function to update the UI
	 */
	public void setAvailableTimeSlotsListener(Consumer<List<String>> callback) {
		this.availableSlotsCallback = callback;
	}
		
	/**
	 * Called by BistroClient when the server sends back available time slots.
	 * Triggers the UI update.
	 * @param slots The list of available time slots
	 */
	public void setLoadedOrder(Order order) {
		if (orderLoadedCallback != null) {
			Platform.runLater(() -> {
				orderLoadedCallback.accept(order);
			});
		}
	}
	
	/**
	 * Registers a callback to update the UI when an order is loaded.
	 * @param callback The callback function to update the UI
	 */
	public void setOrderLoadedListener(Consumer<Order> callback) {
		this.orderLoadedCallback = callback;
	}
	
	/**
	 * Registers a callback to notify the result of an update operation.
	 * @param callback The callback function to notify the result
	 */
	public void setUpdateListener(Consumer<Boolean> callback) {
		this.updateResultCallback = callback;
	}
	
	/**
	 * Registers a callback to notify the result of a cancel operation.
	 * @param callback The callback function to notify the result
	 */
	public void setCancelListener(Consumer<Boolean> callback) {
		this.cancelResultCallback = callback;
	}
	
	/**
	 * Registers a callback to notify the result of a check-in operation.
	 * @param callback The callback function to notify the result
	 */
	public void setCheckInListener(BiConsumer<Boolean, String> callback) {
        this.checkInCallback = callback;
    }
	
	/**
	 * Registers a listener for when the server returns the member's reservations.
	 */
	public void setOnMemberReservationsListListener(Consumer<List<Order>> listener) {
        this.onMemberReservationsListListener = listener;
    }
	
	/**
	 * Gets the weekly hours.
	 * @return
	 */
	public List<WeeklyHour> getWeeklyHours() {
		return weeklyHours;
	}
	
	/**
	 * Sets the weekly hours.
	 * @param weeklyHours
	 */
	public void setWeeklyHours(List<WeeklyHour> weeklyHours) {
		this.weeklyHours = weeklyHours;
	}
	
	/**
	 * Registers a listener for when the server returns the member's seated reservations.
	 */
	public void setOnMemberSeatedListListener(Consumer<List<Order>> listener) {
        this.onMemberSeatedListListener = listener;
    }
	
	/**
	 * Registers a listener for when the server returns the guest's seated confirmation code.
	 * @param listener
	 */
	public void setOnGuestSeatedCodeListener(Consumer<String> listener) {
        this.onGuestSeatedCodeListener = listener;
    }
		
	//******************************** Instance Methods ***********************************//
	
	
	/**
	 * Sends a request to the server to get available hours for a specific date and number of diners.
	 * @param date The date to check availability for
	 * @param diners The number of diners
	 */
	public void askAvailableHours(LocalDate date, int diners) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("date", date);
        requestData.put("dinersAmount", diners);
        client.handleMessageFromClientUI(new Message(Api.ASK_ORDER_AVAILABLE_HOURS, requestData));
    }
	
	/**
	 * Sends a request to the server to get the member's active reservations.
	 */
	public void askMemberActiveReservations() {
        client.handleMessageFromClientUI(new Message(Api.ASK_MEMBER_ACTIVE_RESERVATIONS, null));
    }
	
	/**
	 * Sends the reservation request to the server.
	 * @param date The date of the reservation
	 * @param selectedTimeSlot The selected time slot
	 * @param diners The number of diners
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
	
	/**
	 * Sends the reservation request to the server as staff on behalf of a customer.
	 * @param date The date of the reservation
	 * @param time The time of the reservation
	 * @param diners The number of diners
	 * @param customerType The type of customer ("Member" or "Guest")
	 * @param identifier The member ID (for members) or phone number (for guests)
	 * @param customerName The name of the customer
	 */
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
	
	/**
	 * Called by BistroClient when the server sends back the list of all reservations (for staff).
	 * Triggers the UI update.
	 * @param orders The list of all reservations
	 */
	public void receiveStaffReservations(List<Order> orders) {
		System.out.println("[DEBUG] receiveStaffReservations called with " + (orders == null ? "null" : orders.size() + " orders"));
		System.out.println("[DEBUG] allReservationsCallback is " + (this.allReservationsCallback == null ? "null" : "set"));
	    if (this.allReservationsCallback != null) {
	        this.allReservationsCallback.accept(orders);
	    }
	}
	
	/**
	 * Sends an update reservation request to the server.
	 * @param order The order to update
	 */
	public void updateReservation(Order order) {
		client.handleMessageFromClientUI(new Message(Api.ASK_UPDATE_RESERVATION, order));
	}
	
	/**
	 * Checks if the provided confirmation code is correct by asking the server.
	 * @param confirmationCode The confirmation code to check
	 */
	public void CheckConfirmationCodeCorrect(String confirmationCode) {
        client.handleMessageFromClientUI(new Message(Api.ASK_CHECK_ORDER_EXISTS, confirmationCode));
    }
	
	/**
	 * Requests the order history for the currently logged-in client.
	 */
	public void askClientOrderHistory() {
		client.handleMessageFromClientUI(new Message(Api.ASK_CLIENT_ORDER_HISTORY, null));
	}
	
	/**
	 * Requests order history for a specific member by member code.
	 * Only accessible by staff members (Employee/Manager).
	 * @param memberCode The member code to look up
	 */
	public void askMemberHistory(int memberCode) {
		System.out.println("[DEBUG] askMemberHistory sending request for memberCode: " + memberCode);
		client.handleMessageFromClientUI(new Message(Api.ASK_GET_MEMBER_HISTORY, memberCode));
	}
	
	/**
	 * Requests order details for a specific confirmation code.
	 * @param confirmationCode The confirmation code to look up
	 */
	public void askOrderDetails(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_ORDER, confirmationCode));
	}
	
	/**
	 * Sends a cancel reservation request to the server.
	 * @param confirmationCode The confirmation code of the reservation to cancel
	 */
	public void cancelReservation(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_CANCEL_RESERVATION, confirmationCode));
	}
	
	/**
	 * Notifies the result of an update operation to the UI.
	 * @param success true if the update was successful, false otherwise
	 */
	public void notifyCancelResult(boolean success) {
		if (cancelResultCallback != null) {
			Platform.runLater(() -> cancelResultCallback.accept(success));
		}
	}
	
	/**
	 * Notifies the result of an update operation to the UI.
	 * @param success true if the update was successful, false otherwise
	 */
	public void seatCustomer(String confirmationCode) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_SEAT_CUSTOMER, confirmationCode));
	}
	
	/**
	 * Checks if a user's reservation is ready (for waiting list flow).
	 */
	public boolean isUserReservationReady() {
		return orderDTO.getStatus() == OrderStatus.COMPLETED;
	}
	
	/**
	 * Sends a request to the server to get reservations for a specific date (for staff).
	 * @param date The date to get reservations for
	 */
	public void askReservationsByDate(LocalDate date) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_GET_RESERVATIONS_BY_DATE, date));
	}

	/**
	 * Registers a listener for when the server returns all reservations (for staff).
	 * @param callback
	 */
    public void setAllReservationsListener(Consumer<List<Order>> callback) {
        this.allReservationsCallback = callback;
    }

    /**
	 * Registers a listener for when the server returns the confirmation code for a forgot password request.
	 * @param listener The listener to notify when the code is retrieved
	 */
    public void setOnConfirmationCodeRetrieveResult(Consumer<String> listener) {
        this.onCodeRetrieveResult = listener;
    }

    /**
	 * Called by BistroClient when the server sends back the confirmation code for a forgot password request
	 * @param result The confirmation code retrieved
	 */
    public void handleForgotConfirmationCodeResponse(String result) {
        if (onCodeRetrieveResult != null) {
            Platform.runLater(() -> {
                onCodeRetrieveResult.accept(result);
                onCodeRetrieveResult = null;
            });
        }
    }

    /**
     * Sends a request to the server to retrieve a confirmation code for password recovery.
     * @param email
     * @param phoneNum
     */
	public void retrieveConfirmationCode(String email, String phoneNum) {
	    Map<String, String> requestData = new HashMap<>();
	    requestData.put("email", email);
	    requestData.put("phoneNum", phoneNum);
	    client.handleMessageFromClientUI(new Message(Api.ASK_FORGOT_CONFIRMATION_CODE, requestData));
		
	}

	/**
	 * Checks if there is an active reservation.
	 * @return true if there is an active reservation, false otherwise
	 */
	public boolean hasActiveReservation() {
		if (orderDTO == null) {
			return false;
		}
		return true;
	}
	
	/**
	 * Registers the listener for when the server returns valid dates.
	 * @param callback The callback function to update the UI
	 */
	public void setDatesUpdateListener(Consumer<List<LocalDate>> callback) {
	    this.datesUpdateCallback = callback;
	}

	/**
	 * Called by BistroClient when the server sends back the list of dates.
	 * Triggers the UI update.
	 * @param dates The list of available dates
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
	 * @param diners The number of diners
	 */
	public void askAvailableDates(int diners) {
	    client.handleMessageFromClientUI(new Message(Api.ASK_AVAILABLE_DATES, diners));
	}
	
	/**
	 * Notifies the result of a check-in operation to the UI.
	 * @param success true if the check-in was successful, false otherwise
	 * @param message Additional message regarding the check-in result
	 */
	public void notifyCheckInResult(boolean success, String message) {
        if (checkInCallback != null) {
            Platform.runLater(() -> checkInCallback.accept(success, message));
        }
    }
	
	/**
	 * Checks if a check-in listener is registered.
	 * @return
	 */
	public boolean hasCheckInListener() {
		return this.checkInCallback != null;
	}
	
	/**
	 * Sends a request to the server to get the member's reservations.
	 * @param listener The listener to notify when the reservations are received
	 */
	public void handleMemberReservationsListResponse(List<Order> orders) {
        if (onMemberReservationsListListener != null) {
            Platform.runLater(() -> {
                onMemberReservationsListListener.accept(orders);
                onMemberReservationsListListener = null;
            });
        }
    }

	/**
	 * Sends a request to the server to get the weekly hours.
	 */
	public void askWeeklyHours() {
		client.handleMessageFromClientUI(new Message(Api.ASK_GET_WEEKLY_HOURS, null));
	}
	
	/**
	 * Sends a request to the server to get the member's seated reservations.
	 */
	public void askMemberSeatedReservations() {
        client.handleMessageFromClientUI(new Message(Api.ASK_MEMBER_SEATED_RESERVATIONS, null));
    }
	
	/**
	 * Sends the member's seated reservations to the registered listener.
	 * @param orders
	 */
	public void handleMemberSeatedListResponse(List<Order> orders) {
        if (onMemberSeatedListListener != null) {
            Platform.runLater(() -> {
                onMemberSeatedListListener.accept(orders);
                onMemberSeatedListListener = null;
            });
        }
    }
	
	/**
	 * Sends the guest's seated confirmation code to the registered listener.
	 * @param code
	 */
	public void handleGuestSeatedCodeResponse(String code) {
        if (onGuestSeatedCodeListener != null) {
            Platform.runLater(() -> {
                onGuestSeatedCodeListener.accept(code);
                onGuestSeatedCodeListener = null;
            });
        }
    }
	
	/**
	 * Sends a request to the server to get the guest's seated confirmation code.
	 * @param email
	 * @param phone
	 */
	public void retrieveGuestSeatedCode(String email, String phone) {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("phone", phone);
        client.handleMessageFromClientUI(new Message(Api.ASK_GUEST_SEATED_CODE, data));
    }
}
// End of ReservationController.java