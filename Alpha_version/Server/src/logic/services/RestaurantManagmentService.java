package logic.services;

import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

/**
 * Handles restaurant configuration like opening hours and holidays.
 * Used by managers to update the restaurant's operating schedule.
 */
public class RestaurantManagmentService {
	
	/** Database controller for all DB operations */
	private final BistroDataBase_Controller dbController;
	
	/** Logger for tracking service activity */
	private final ServerLogger logger;
	    
	//******************************** Constructor ********************************//
	
	/**
	 * Creates a new RestaurantManagementService with required dependencies.
	 * 
	 * @param dbController database controller
	 * @param logger server logger
	 */
	public RestaurantManagmentService(BistroDataBase_Controller dbController, ServerLogger logger) {
	    this.dbController = dbController;
	    this.logger = logger;
	}
	    
	    
	//********************************Instance  Methods ********************************//
	
	/**
	 * Saves the weekly operating hours for the restaurant.
	 * 
	 * @param hours list of WeeklyHour objects for each day
	 * @return true if saved successfully
	 */
	public boolean saveWeeklyHours(List<WeeklyHour> hours) {
        return dbController.updateWeeklyHours(hours);
    }

	/**
	 * Adds a new holiday to the restaurant's schedule.
	 * 
	 * @param holiday the holiday to add
	 * @return true if added successfully
	 */
    public boolean addHoliday(Holiday holiday) {
        return dbController.addHoliday(holiday);
    }
    
    /**
     * Removes a holiday from the restaurant's schedule.
     * 
     * @param holiday the holiday to remove
     * @return true if removed successfully
     */
    public boolean removeHoliday(Holiday holiday) {
        return dbController.removeHoliday(holiday);
    }
	
	
}