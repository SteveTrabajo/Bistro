package logic.services;

import java.util.Map;

import entities.User;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class UserService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	
	public UserService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}

	public User getUserInfo(Map<String, Object> loginData) {
		User userfound=null;
		switch (String.valueOf(loginData.get("userType"))) {
		case "GUEST":
			userfound= dbController.findOrCreateGuestUser((String) loginData.get("phoneNumber"),(String) loginData.get("email"));
			break;
		case "MEMBER":
			userfound = dbController.findMemberUser((int) loginData.get("id"));
			break;
		case "EMPLOYEE", "MANAGER":
			String username = String.valueOf(loginData.get("username"));
			String password = String.valueOf(loginData.get("password"));
			userfound = dbController.findStaffUser(username, password);
			break;
		default:
			logger.log("[ERROR] Unknown user type: " + String.valueOf(loginData.get("userType")));
			break;
		}
		return userfound;
	}

	public boolean updateUserInfo(User updatedUser) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
