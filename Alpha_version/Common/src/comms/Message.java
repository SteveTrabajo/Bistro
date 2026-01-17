package comms;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;        
    private final Object data;      
    private final String requestId;

    /*
     * Creates a new Message with a unique requestId.
     * @param id      the message identifier
     * @param data    the message payload
     * @return        a new Message instance
     */
    public Message(String id, Object data) {
        this(id, data, UUID.randomUUID().toString());
    }

    /*
	 * Creates a new Message with the specified requestId.
	 * @param id        the message identifier
	 * @param data      the message payload
	 * @param requestId the request identifier
	 */
    public Message(String id, Object data, String requestId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.data = data;
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
    }

    /*
     * Gets the message identifier.
     * @return the message identifier
     */
    public String getId() {
        return id;
    }

    /*
	 * Gets the message payload.
	 * @return the message payload
	 */
    public Object getData() {
        return data;
    }

    /*
     * Gets the request identifier.
     * @return the request identifier
     */
    public String getRequestId() {
        return requestId;
    }

    /*
     * Returns a string representation of the Message.
     * @return a string representation of the Message
     */
    @Override
    public String toString() {
        return "Message{id='" + id + "', requestId='" + requestId + "', dataType=" +
                (data == null ? "null" : data.getClass().getSimpleName()) + "}";
    }
}
// End of Path: Message.java