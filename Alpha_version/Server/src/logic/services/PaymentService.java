package logic.services;

import logic.*;

public class PaymentService {
	
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	public PaymentService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}
	
	
}
