package logic.services;

import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class TableService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;

	public TableService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	public int getTableNumberByReservationConfirmationCode(String confirmationCode) {
		return dbController.getTableNumberByConfirmationCode(confirmationCode);
	}

}
