package logic;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import comms.Message;
import entities.Order;
import enums.DaysOfWeek;

public class Reservation_Controller {
	
	//****************************** Static variables ******************************//
	
	public static final int SLOT_MINUTES = 15;
	public static final int DURATION_MINUTES = 120;
	private static final int SLOTS_PER_RESERVATION = DURATION_MINUTES / SLOT_MINUTES;
	public static final int CAPACITY = 80;
	
	//****************************** Instance variables ******************************//
	
	private  final BistroClient client;
	private Map <LocalDate,TreeMap<LocalTime,List <Order>>> activeReservations;
	
	//******************************** Constructors ***********************************//
	
	public Reservation_Controller(BistroClient client) {
		this.client = client;
		this.activeReservations = new TreeMap<>();
	}
	
	//******************************** Getters And Setters ***********************************//
	public Map<LocalDate, TreeMap<LocalTime,List <Order>>> getActiveReservations() {
		return activeReservations;
	}
	
	public void setActiveReservations(Map<LocalDate, TreeMap<LocalTime,List <Order>>> activeReservations) {
		this.activeReservations = activeReservations;
	}
	
	//******************************** Instance Methods ***********************************//
	
	
}
