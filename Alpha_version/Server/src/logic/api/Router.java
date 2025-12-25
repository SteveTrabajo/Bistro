package logic.api;

import java.util.HashMap;
import java.util.Map;

import comms.Message;
import ocsf.server.ConnectionToClient;

/**
 * Routes messages based on the format: {@code subject.action}.
 */
public class Router {

    /** Maps subjects to their action handlers */
    private final Map<String, Map<String, ServerHandler>> routes = new HashMap<>();

    /**
     * Registers a handler for a subject and action.
     */
    public void on(String subject, String action, ServerHandler handler) {
        if (!routes.containsKey(subject)) {
            routes.put(subject, new HashMap<>());
        }
        routes.get(subject).put(action, handler);
    }

    /**
     * Dispatches a message to the matching handler.
     *
     * @return true if a handler was found
     */
    public boolean dispatch(Message msg, ConnectionToClient client) throws Exception {
        if (msg == null || msg.getId() == null) return false;

        String[] parts = msg.getId().split("\\.", 2);
        String subject = parts[0];
        String action  = (parts.length == 2) ? parts[1] : "";

        if (!routes.containsKey(subject)) return false;

        Map<String, ServerHandler> handlers = routes.get(subject);
        if (!handlers.containsKey(action)) return false;

        handlers.get(action).handle(msg, client);
        return true;
    }
}
