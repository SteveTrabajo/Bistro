package logic.api;

import comms.Message;

/**
 * Interface for handling client messages.
 */
public interface ClientHandler {
	/**
	 * Handles an incoming message from a client.
	 *
	 * @param msg The message to handle.
	 * @throws Exception If an error occurs while handling the message.
	 */
	void handle(Message msg) throws Exception;
}
