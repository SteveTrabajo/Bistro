package dto;

import java.io.Serializable;

/**
 * WaitListResponse must implement Serializable to be transmitted 
 * between the Server and the Client.
 */
public class WaitListResponse implements Serializable {
    // serialVersionUID ensures that both sides agree on the class version
    private static final long serialVersionUID = 1L;

    private boolean canSeatImmediately;
    private long estimatedWaitTimeMinutes;
    private String message;

    public WaitListResponse(boolean canSeatImmediately, long waitTime, String message) {
        this.canSeatImmediately = canSeatImmediately;
        this.estimatedWaitTimeMinutes = waitTime;
        this.message = message;
    }

    public boolean isCanSeatImmediately() {
        return canSeatImmediately;
    }

    public long getEstimatedWaitTimeMinutes() {
        return estimatedWaitTimeMinutes;
    }

    public String getMessage() {
        return message;
    }
}