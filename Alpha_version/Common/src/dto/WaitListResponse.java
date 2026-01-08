package dto;

public class WaitListResponse {
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