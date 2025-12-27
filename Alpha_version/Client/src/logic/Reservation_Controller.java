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

import comms.Api;
import comms.Message;
import entities.Order;
import enums.DaysOfWeek;

public class Reservation_Controller {
	
	//****************************** Static variables ******************************//
	
	public static final int SLOT_MINUTES = 15;
	public static final int DURATION_MINUTES = 120;
	private static final int SLOTS_PER_RESERVATION = DURATION_MINUTES / SLOT_MINUTES;
	public static final int MAX_CAPACITY = 80;
	
	//****************************** Instance variables ******************************//
	
	private  final BistroClient client;
	private Map<LocalTime,List<Order>> reservationsByDate;
	private String confirmationCode; 
	private boolean userReservationReady;
	//******************************** Constructors ***********************************//
	
	public Reservation_Controller(BistroClient client) {
		this.client = client;
		this.reservationsByDate = new TreeMap<>();
		this.confirmationCode = "";
	}
	
	//******************************** Getters And Setters ***********************************//
	
	public Map<LocalTime, List<Order>> getReservationsByDate() {
		return reservationsByDate;
	}
	
	public void setReservationsByDate(Map<LocalTime, List<Order>> reservationsByDate) {
		this.reservationsByDate = reservationsByDate;
	}
	
	public String getConfirmationCode() {
		return confirmationCode;
	}
	
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}
	
	public void setUserOnWaitingList(boolean userOnWaitingList) {
		this.userOnWaitingList = userOnWaitingList;
	}
	
	public boolean getUserOnWaitingList() {
		return this.userOnWaitingList;
	}
	
	public boolean getUserReservationReady() {
		return this.userReservationReady;
	}
	
	public void setUserReservationReady(boolean userReservationReady) {
		this.userReservationReady = userReservationReady;
	}
	
	//******************************** Instance Methods ***********************************//
	public void askReservationsByDate(LocalDate date) {
		client.handleMessageFromClientUI(new Message("ASK_ORDER_BY_DATE", date));
	}
	
	//TODO: complete method with a new logic that we collect available time slots from server
	public List<String> receiveAvailableTimeSlots(LocalTime startTime ,int dinersAmount , boolean isToday) {
		LocalTime requestTime = startTime; // The time requested by the user
		List<String> timeSlots = new ArrayList<>(); // List to hold available time slots
		if(isToday) { // If the reservation is for today, round up the time to the next slot
			requestTime = roundUpTimeToSlot(startTime);
		}
		for (int i=0; i< SLOTS_PER_RESERVATION;i++) {
			LocalTime slotTime = requestTime.plusMinutes(i * SLOT_MINUTES); // Calculate the current slot time
			
		}
		return timeSlots;
	}
	
	
	public LocalTime roundUpTimeToSlot(LocalTime time) {
		int minute = time.getMinute(); // Get the minute part of the time
		int mod = minute % SLOT_MINUTES; // Calculate the remainder when divided by SLOT_MINUTES
		if (mod == 0) { // If already aligned to a slot
			return time.withSecond(0).withNano(0); // Return time with seconds and nanoseconds set to 0
		} else {
			int minutesToAdd = SLOT_MINUTES - mod; // Calculate minutes to add to reach the next slot
			return time.plusMinutes(minutesToAdd).withSecond(0).withNano(0); // Return the rounded-up time
		}
	}

	public void createNewReservation(LocalDate date, String selectedTimeSlot, int diners) {
		List<Object> reservationData = new ArrayList<>();
		reservationData.add(date);
		reservationData.add(selectedTimeSlot);
		reservationData.add(diners);
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION,reservationData));
		
	}

	

	
	
}
