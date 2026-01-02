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
		logger.log("[LOGIN] Received login data=" + loginData);
		User userfound=null;
		switch (String.valueOf(loginData.get("userType"))) {
		case "GUEST":
			userfound= dbController.findOrCreateGuestUser((String) loginData.get("phoneNumber"),(String) loginData.get("email"));
			break;
		case "MEMBER": {
		    Object raw = loginData.get("memberCode");
		    if (raw == null) {
		        logger.log("[LOGIN] MEMBER missing key 'memberCode'. Keys=" + loginData.keySet());
		        return null;
		    }

		    int memberCode;
		    try {
		        memberCode = (raw instanceof Integer)
		                ? (Integer) raw
		                : Integer.parseInt(raw.toString().trim());
		    } catch (NumberFormatException ex) {
		        logger.log("[LOGIN] MEMBER invalid memberCode value: " + raw);
		        return null;
		    }

		    userfound = dbController.findMemberUserByCode(memberCode);
		    break;
		}


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

	public boolean updateMebmerInfo(User updatedUser) {
			
		return dbController.setUpdatedMemberData(updatedUser);	
	}
}

