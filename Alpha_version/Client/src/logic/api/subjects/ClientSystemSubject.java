package logic.api.subjects;

import logic.BistroClient;
import logic.api.ClientRouter;
/**
 * ClientSystemSubject is responsible for handling system-related messages from the server.
 */
public class ClientSystemSubject {
	/**
	 * Registers system-related message handlers with the provided ClientRouter.
	 * @param router
	 */
	public static void register(ClientRouter router) {
		//
		router.on("system", "unknownCommand", msg -> {
			System.out.println("System shutdown acknowledged by server.");
            BistroClient.awaitResponse = false;
		});
		
	}

}
