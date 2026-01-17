package logic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple utility class for password hashing using SHA-256 with salt.
 * Lightweight implementation suitable for academic projects.
 */
public class PasswordUtil {
	
	private static final String ALGORITHM = "SHA-256";
	private static final int SALT_LENGTH = 8;
	
	/**
	 * Generates a simple hash of the password using SHA-256 with salt.
	 * Format: salt:hash (base64 encoded)
	 * 
	 * @param password The plaintext password to hash
	 * @return A string containing salt and hash separated by colon
	 */
	public static String hashPassword(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password cannot be null or empty");
		}
		
		try {
			// Generate random salt
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[SALT_LENGTH];
			random.nextBytes(salt);
			
			// Hash password with salt
			MessageDigest md = MessageDigest.getInstance(ALGORITHM);
			md.update(salt);
			byte[] hash = md.digest(password.getBytes());
			
			// Encode as salt:hash
			String encodedSalt = Base64.getEncoder().encodeToString(salt);
			String encodedHash = Base64.getEncoder().encodeToString(hash);
			
			return encodedSalt + ":" + encodedHash;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}
	
	/**
	 * Verifies a plaintext password against a hashed password.
	 * 
	 * @param password The plaintext password to verify
	 * @param hashedPassword The previously hashed password (salt:hash format)
	 * @return true if password matches, false otherwise
	 */
	public static boolean verifyPassword(String password, String hashedPassword) {
		if (password == null || password.isEmpty() || hashedPassword == null || hashedPassword.isEmpty()) {
			return false;
		}
		
		try {
			// Split salt and hash
			String[] parts = hashedPassword.split(":");
			if (parts.length != 2) {
				return false;
			}
			
			byte[] salt = Base64.getDecoder().decode(parts[0]);
			byte[] storedHash = Base64.getDecoder().decode(parts[1]);
			
			// Hash the provided password with the same salt
			MessageDigest md = MessageDigest.getInstance(ALGORITHM);
			md.update(salt);
			byte[] computedHash = md.digest(password.getBytes());
			
			// Constant-time comparison
			return constantTimeEquals(storedHash, computedHash);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Constant-time comparison to prevent timing attacks.
	 */
	private static boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		
		int result = 0;
		for (int i = 0; i < a.length; i++) {
			result |= a[i] ^ b[i];
		}
		return result == 0;
	}
	
	// Simple key for XOR encryption (for academic purposes only)
	private static final String ENCRYPTION_KEY = "BistroSecretKey2026";
	
	/**
	 * Encrypts a password using simple XOR cipher with Base64 encoding.
	 * This is reversible encryption for password recovery purposes.
	 * 
	 * @param password The plaintext password to encrypt
	 * @return The encrypted password as a Base64 string
	 */
	public static String encrypt(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Password cannot be null or empty");
		}
		
		byte[] passwordBytes = password.getBytes();
		byte[] keyBytes = ENCRYPTION_KEY.getBytes();
		byte[] encrypted = new byte[passwordBytes.length];
		
		for (int i = 0; i < passwordBytes.length; i++) {
			encrypted[i] = (byte) (passwordBytes[i] ^ keyBytes[i % keyBytes.length]);
		}
		
		return Base64.getEncoder().encodeToString(encrypted);
	}
	
	/**
	 * Decrypts a password that was encrypted using the encrypt method.
	 * 
	 * @param encryptedPassword The encrypted password (Base64 encoded)
	 * @return The original plaintext password
	 */
	public static String decrypt(String encryptedPassword) {
		if (encryptedPassword == null || encryptedPassword.isEmpty()) {
			throw new IllegalArgumentException("Encrypted password cannot be null or empty");
		}
		
		try {
			byte[] encrypted = Base64.getDecoder().decode(encryptedPassword);
			byte[] keyBytes = ENCRYPTION_KEY.getBytes();
			byte[] decrypted = new byte[encrypted.length];
			
			for (int i = 0; i < encrypted.length; i++) {
				decrypted[i] = (byte) (encrypted[i] ^ keyBytes[i % keyBytes.length]);
			}
			
			return new String(decrypted);
		} catch (Exception e) {
			throw new RuntimeException("Failed to decrypt password", e);
		}
	}
}

