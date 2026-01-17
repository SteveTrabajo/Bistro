package logic.services;

import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

/**
 * Service for managing restaurant operations such as weekly hours and holidays.
 */
public class RestaurantManagmentService {
	
	// ******************************** Instance Variables ********************************//
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;

	// ******************************** Constructor ********************************//
	public RestaurantManagmentService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	// ********************************Instance Methods ********************************//
	/**
	 * Saves the provided weekly hours to the database.
	 * 
	 * @param hours A list of WeeklyHour objects representing the weekly hours to save.
	 * @return true if the operation was successful, false otherwise.
	 */
	public boolean saveWeeklyHours(List<WeeklyHour> hours) {
		return dbController.updateWeeklyHours(hours);
	}

	/**
	 * Adds a holiday to the database.
	 * 
	 * @param holiday The Holiday object to add.
	 * @return true if the operation was successful, false otherwise.
	 */
	public boolean addHoliday(Holiday holiday) {
		return dbController.addHoliday(holiday);
	}

	/**
	 * Removes a holiday from the database.
	 * 
	 * @param holiday The Holiday object to remove.
	 * @return true if the operation was successful, false otherwise.
	 */
	public boolean removeHoliday(Holiday holiday) {
		return dbController.removeHoliday(holiday);
	}

	/**
	 * Retrieves the list of weekly hours from the database.
	 * 
	 * @return A list of WeeklyHour objects representing the weekly hours.
	 */
	public List<WeeklyHour> getWeeklyHours() {
		return dbController.getWeeklyHours();
	}

	/**
	 * Retrieves the list of holidays from the database.
	 * 
	 * @return A list of Holiday objects representing the holidays.
	 */
	public List<Holiday> getHolidays() {
		return dbController.getHolidays();
	}
}
// End of RestaurantManagmentService.java