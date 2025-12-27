package logic.api.subjects;
import java.util.List;

import comms.Api;
import comms.Message;
import entities.Order;
import entities.User;
import logic.BistroDataBase_Controller;
import logic.api.Router;
public  final class UserSubject 
{

	/**
	 * API handlers related to User Subject.
	 */
	 private UserSubject() {}
	 
	 /**
	 * Registers all User-related handlers.
	  */
	 public static void register(Router router)
	 {
		 // Get user information
	        router.on("login", "user", (msg, client) -> {
	            User user = BistroDataBase_Controller.getUserInfo();	//need to put the given phone and email
	            if(user != null) {
	            client.sendToClient(new Message(Api.REPLY_LOGIN_USER_OK, user));
	            }
	            else{client.sendToClient(new Message(Api.REPLY_LOGIN_USER_NOT_FOUND, user));}
	        });
		 
	 }
}
