package logic.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import entities.Table;
import enums.OrderStatus;
import enums.OrderType;
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
	
	public List<Table> getAllTables() {
		return dbController.getAllTablesFromDB();
	}

	public int allocateTable(String ConfimationCode) {
		// TODO Auto-generated method stub
		return 0;
	}

}
