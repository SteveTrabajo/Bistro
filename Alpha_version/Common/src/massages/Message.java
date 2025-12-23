package massages;

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

    public Message(String id, Object data) {
        this(id, data, UUID.randomUUID().toString());
    }

    public Message(String id, Object data, String requestId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.data = data;
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
    }

    public String getId() {
        return id;
    }

    public Object getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "Message{id='" + id + "', requestId='" + requestId + "', dataType=" +
                (data == null ? "null" : data.getClass().getSimpleName()) + "}";
    }
}
