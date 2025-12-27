package logic.api;

import java.util.HashMap;
import java.util.Map;

import comms.Message;

/**
 * Routes messages on client based on: subject.action (or
 * subject.action.result).
 */
public class ClientRouter {

	/** Maps subjects to their action handlers */
	private final Map<String, Map<String, ClientHandler>> routes = new HashMap<>();

	/*
	 * Method to register a handler for a specific subject and action.
	 * 
	 * @param subject the message subject
	 * 
	 * @param action the message action
	 * 
	 * @param handler the handler to register
	 */
	public void on(String subject, String action, ClientHandler handler) {
		routes.computeIfAbsent(subject, s -> new HashMap<>()).put(action, handler);
	}

	/**
	 * Dispatches a message to the matching handler.
	 *
	 * @return true if a handler was found
	 */
	public boolean dispatch(Message msg) throws Exception {
		if (msg == null || msg.getId() == null)
			return false;

		String[] parts = msg.getId().split("\\.", 2);
		String subject = parts[0];
		String action = (parts.length == 2) ? parts[1] : "";

		Map<String, ClientHandler> handlers = routes.get(subject);
		if (handlers == null)
			return false;

		ClientHandler h = handlers.get(action);
		if (h == null) {
			return false;
		}
		h.handle(msg);
		return true;
	}
}
