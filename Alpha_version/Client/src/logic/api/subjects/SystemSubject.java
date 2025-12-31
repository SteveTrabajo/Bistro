package logic.api.subjects;

import logic.api.ClientRouter;

public class SystemSubject {

	public static void register(ClientRouter router) {
		
		router.on("system", "unknownCommand", msg -> {
			System.out.println("System shutdown acknowledged by server.");
		});
		
	}

}
