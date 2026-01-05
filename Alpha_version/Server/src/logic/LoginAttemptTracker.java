package logic;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Utility class to track failed login attempts for employee accounts.
 * Implements account lockout mechanism after a configurable number of failed attempts.
 * 
 * Thread-safe implementation for tracking login attempts across multiple concurrent connections.
 */
public class LoginAttemptTracker {
	
	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final long LOCKOUT_DURATION_MINUTES = 1;
	
	private static class AttemptRecord {
		int failedAttempts;
		LocalDateTime lastFailedAttempt;
		LocalDateTime lockedUntil;
		
		AttemptRecord() {
			this.failedAttempts = 0;
			this.lastFailedAttempt = null;
			this.lockedUntil = null;
		}
	}
	
	private static final Map<String, AttemptRecord> attemptMap = new HashMap<>();
	private static final Object lock = new Object();
	
	/**
	 * Records a failed employee login attempt for a username.
	 * 
	 * @param username The employee username that failed to authenticate
	 * @return true if employee account is now locked, false otherwise
	 */
	public static boolean recordFailedAttempt(String username) {
		if (username == null || username.isEmpty()) {
			return false;
		}
		
		synchronized (lock) {
			AttemptRecord record = attemptMap.computeIfAbsent(username, k -> new AttemptRecord());
			record.failedAttempts++;
			record.lastFailedAttempt = LocalDateTime.now();
			
			if (record.failedAttempts >= MAX_FAILED_ATTEMPTS) {
				record.lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
				return true; // Account is now locked
			}
			return false;
		}
	}
	
	/**
	 * Records a successful employee login, clearing failed attempts.
	 * 
	 * @param username The employee username that successfully authenticated
	 */
	public static void recordSuccessfulLogin(String username) {
		if (username == null || username.isEmpty()) {
			return;
		}
		
		synchronized (lock) {
			AttemptRecord record = attemptMap.get(username);
			if (record != null) {
				record.failedAttempts = 0;
				record.lockedUntil = null;
			}
		}
	}
	
	/**
	 * Checks if an employee account is currently locked due to too many failed attempts.
	 * 
	 * @param username The employee username to check
	 * @return true if employee account is locked, false otherwise
	 */
	public static boolean isAccountLocked(String username) {
		if (username == null || username.isEmpty()) {
			return false;
		}
		
		synchronized (lock) {
			AttemptRecord record = attemptMap.get(username);
			if (record == null || record.lockedUntil == null) {
				return false;
			}
			
			LocalDateTime now = LocalDateTime.now();
			if (now.isBefore(record.lockedUntil)) {
				return true; // Still locked
			}
			
			// Lockout period has expired, reset the record
			record.failedAttempts = 0;
			record.lockedUntil = null;
			return false;
		}
	}
	
	/**
	 * Gets the number of remaining failed attempts before employee account lockout.
	 * 
	 * @param username The employee username to check
	 * @return Number of remaining attempts, or 0 if employee account is locked
	 */
	public static int getRemainingAttempts(String username) {
		if (username == null || username.isEmpty()) {
			return MAX_FAILED_ATTEMPTS;
		}
		
		synchronized (lock) {
			if (isAccountLocked(username)) {
				return 0;
			}
			
			AttemptRecord record = attemptMap.get(username);
			if (record == null) {
				return MAX_FAILED_ATTEMPTS;
			}
			
			return Math.max(0, MAX_FAILED_ATTEMPTS - record.failedAttempts);
		}
	}
	
	/**
	 * Gets the lockout end time for an employee account.
	 * 
	 * @param username The employee username to check
	 * @return LocalDateTime when the employee account lockout will end, or null if not locked
	 */
	public static LocalDateTime getLockoutEndTime(String username) {
		if (username == null || username.isEmpty()) {
			return null;
		}
		
		synchronized (lock) {
			AttemptRecord record = attemptMap.get(username);
			return (record != null) ? record.lockedUntil : null;
		}
	}
	
	/**
	 * Manually unlock an employee account (e.g., by admin).
	 * 
	 * @param username The employee username to unlock
	 */
	public static void unlockAccount(String username) {
		if (username == null || username.isEmpty()) {
			return;
		}
		
		synchronized (lock) {
			AttemptRecord record = attemptMap.get(username);
			if (record != null) {
				record.failedAttempts = 0;
				record.lockedUntil = null;
			}
		}
	}
	
	/**
	 * Clears all tracking records (useful for testing or maintenance).
	 */
	public static void clearAll() {
		synchronized (lock) {
			attemptMap.clear();
		}
	}
}
