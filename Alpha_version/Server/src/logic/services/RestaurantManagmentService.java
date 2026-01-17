package logic.services;

import java.util.List;

import dto.Holiday;
import dto.WeeklyHour;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class RestaurantManagmentService {
	
	 	private final BistroDataBase_Controller dbController;
	    private final ServerLogger logger;
	    
	    //******************************** Constructor ********************************//
	    public RestaurantManagmentService(BistroDataBase_Controller dbController, ServerLogger logger) {
	        this.dbController = dbController;
	        this.logger = logger;
	    }
	    
	    
	    //********************************Instance  Methods ********************************//
	/** Saves the provided weekly hours to the database.
     * @param hours A list of WeeklyHour objects representing the weekly hours to save.
     * @return true if the operation was successful, false otherwise.
     */
	public boolean saveWeeklyHours(List<WeeklyHour> hours) {
        return dbController.updateWeeklyHours(hours);
    }

	/** Adds a holiday to the database.
     * @param holiday The Holiday object to add.
     * @return true if the operation was successful, false otherwise.
     */
    public boolean addHoliday(Holiday holiday) {
        return dbController.addHoliday(holiday);
    }
    
    /** Removes a holiday from the database.
     * @param holiday The Holiday object to remove.
     * @return true if the operation was successful, false otherwise.
     */
    public boolean removeHoliday(Holiday holiday) {
        return dbController.removeHoliday(holiday);
    }


	public List<WeeklyHour> getWeeklyHours() {
		return dbController.getWeeklyHours();
	}
	
	
}
