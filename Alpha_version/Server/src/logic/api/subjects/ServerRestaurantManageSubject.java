package logic.api.subjects;

import java.util.List;

import comms.Api;
import comms.Message;
import dto.Holiday;
import dto.WeeklyHour;
import logic.ServerLogger;
import logic.api.ServerRouter;
import logic.services.RestaurantManagmentService;
import logic.services.TableService;

/**
 * ServerRestaurantManageSubject is responsible for handling restaurant
 * management related API routes on the server side.
 */
public class ServerRestaurantManageSubject {

	private ServerRestaurantManageSubject() {
	}

	/**
	 * Registers the restaurant management related routes to the server router.
	 *
	 * @param router            The server router to register the routes to.
	 * @param logger            The server logger for logging actions.
	 * @param restaurantService The restaurant management service for handling
	 *                          restaurant operations.
	 */
	public static void register(ServerRouter router, ServerLogger logger,
			RestaurantManagmentService restaurantService) {
		
		// Route for saving weekly hours
		router.on("hours", "saveWeekly", (msg, client) -> {
			@SuppressWarnings("unchecked")
			List<WeeklyHour> hours = (List<WeeklyHour>) msg.getData();
			if (restaurantService.saveWeeklyHours(hours)) {
				logger.log("Weekly hours updated: " + hours.toString());
				client.sendToClient(new Message(Api.REPLY_SAVE_WEEKLY_HOURS_OK, null));
			} else {
				logger.log("Failed to update weekly hours: " + hours.toString());
				client.sendToClient(new Message(Api.REPLY_SAVE_WEEKLY_HOURS_FAIL, null));
			}
		});

		// Route for adding a holiday
		router.on("hours", "addHoliday", (msg, client) -> {
			Holiday holiday = (Holiday) msg.getData();
			if (restaurantService.addHoliday(holiday)) {
				logger.log("Holiday added: " + holiday.toString());
				client.sendToClient(new Message(Api.REPLY_ADD_HOLIDAY_OK, null));
			} else {
				logger.log("Failed to add holiday: " + holiday.toString());
				client.sendToClient(new Message(Api.REPLY_ADD_HOLIDAY_FAIL, null));
			}
		});

		// Route for removing a holiday
		router.on("hours", "removeHoliday", (msg, client) -> {
			Holiday holiday = (Holiday) msg.getData();
			if (restaurantService.removeHoliday(holiday)) {
				logger.log("Holiday removed: " + holiday.toString());
				client.sendToClient(new Message(Api.REPLY_REMOVE_HOLIDAY_OK, null));
			} else {
				logger.log("Failed to remove holiday: " + holiday.toString());
				client.sendToClient(new Message(Api.REPLY_REMOVE_HOLIDAY_FAIL, null));
			}
		});
		
		// Route for retrieving weekly hours
		router.on("hours", "getWeeklyHours", (msg, client) -> {
			List<WeeklyHour> hours = restaurantService.getWeeklyHours();
			if (hours == null) {
				logger.log("Failed to retrieve weekly hours.");
				client.sendToClient(new Message(Api.REPLY_GET_WEEKLY_HOURS_FAIL, null));
				return;
			}
			logger.log("Weekly hours retrieved: " + hours.toString());
			client.sendToClient(new Message(Api.REPLY_GET_WEEKLY_HOURS_OK, hours));
		});
		
		// Route for retrieving holidays
		router.on("hours", "getHolidays", (msg, client) -> {
		    List<Holiday> holidays = restaurantService.getHolidays();
		    if (holidays == null) {
		        logger.log("Failed to retrieve holidays.");
		        client.sendToClient(new Message(Api.REPLY_GET_HOLIDAYS_FAIL, null));
		        return;
		    }
		    logger.log("Holidays retrieved: " + holidays.toString());
		    client.sendToClient(new Message(Api.REPLY_GET_HOLIDAYS_OK, holidays));
		});
	}
}
// End of ServerRestaurantManageSubject.java