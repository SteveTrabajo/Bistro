package logic.api;

import comms.Message;

public interface ClientHandler {
	void handle(Message msg) throws Exception;
}
