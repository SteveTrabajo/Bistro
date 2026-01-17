package dto;

import java.io.Serializable;

public class WaitListResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean canSeatImmediately;
	private long estimatedWaitTimeMinutes;
	private String message;

	/*
	 * Creates a WaitListResponse instance.
	 * @param canSeatImmediately      whether seating is available immediately
	 * @param waitTime                the estimated wait time in minutes
	 * @param message                 additional message
	 */
	public WaitListResponse(boolean canSeatImmediately, long waitTime, String message) {
		this.canSeatImmediately = canSeatImmediately;
		this.estimatedWaitTimeMinutes = waitTime;
		this.message = message;
	}

	/*
	 * Checks if seating is available immediately.
	 * @return true if seating is available immediately, false otherwise
	 */
	public boolean isCanSeatImmediately() {
		return canSeatImmediately;
	}

	/*
	 * Gets the estimated wait time in minutes.
	 * @return the estimated wait time
	 */
	public long getEstimatedWaitTimeMinutes() {
		return estimatedWaitTimeMinutes;
	}

	/*
	 * Gets the additional message.
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
}
// end of WaitListResponse.java