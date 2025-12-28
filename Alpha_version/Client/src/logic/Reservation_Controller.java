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
	private Order reayUserReservation;
	private String confirmationCode;
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
	
	public Order getReayUserReservation() {
		return reayUserReservation;
	}
	
	public void setReayUserReservation(Order reayUserReservation) {
		this.reayUserReservation = reayUserReservation;
	}
	
	//******************************** Instance Methods ***********************************//
	public void askReservationsByDate(LocalDate date) {
		client.handleMessageFromClientUI(new Message("ASK_ORDER_BY_DATE", date));
	}
	
	public void createNewReservation(LocalDate date, String selectedTimeSlot, int diners) {
		List<Object> reservationData = new ArrayList<>();
		reservationData.add(date);
		reservationData.add(selectedTimeSlot);
		reservationData.add(diners);
		client.handleMessageFromClientUI(new Message(Api.ASK_CREATE_RESERVATION,reservationData));
		
	}

	public boolean isUserReservationReady() {
		// TODO Auto-generated method stub
		return false;
	}

	

	
	
}
