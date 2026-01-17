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

public class ServerRestaurantManageSubject {

	private ServerRestaurantManageSubject() {
	}

	public static void register(ServerRouter router, ServerLogger logger,
			RestaurantManagmentService restaurantService) {
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
