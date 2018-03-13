package aQute.lib.mavenpasswordobfuscator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import aQute.lib.base64.Base64;

public class MavenPasswordObfuscator {
	private static final Pattern		DECORATED_PASSWORD_P	= Pattern.compile(
		"\\{\\s*(?<expr>(?:[a-z0-9+/]{4})*(?:[a-z0-9+/]{2}==|[a-z0-9+/]{3}=)?)\\s*\\}", Pattern.CASE_INSENSITIVE);

	private static final int			SALT_SIZE				= 8;
	private static final int			CHUNK_SIZE				= 16;
	private static final String			DIGEST_ALG				= "SHA-256";
	private static final String			KEY_ALG					= "AES";
	private static final String			CIPHER_ALG				= "AES/CBC/PKCS5Padding";
	private final static SecureRandom	secureRandom			= new SecureRandom();

	public static byte[] encrypt(final byte[] payload, final String passPhrase) throws Exception {
		byte[] salt = new byte[SALT_SIZE];
		secureRandom.nextBytes(salt);
		Cipher cipher = createCipher(passPhrase, salt, Cipher.ENCRYPT_MODE);
		byte[] encryptedBytes = cipher.doFinal(payload);
		int len = encryptedBytes.length;
		byte padLen = (byte) (CHUNK_SIZE - (SALT_SIZE + len + 1) % CHUNK_SIZE);
		int totalLen = SALT_SIZE + len + padLen + 1;
		byte[] allEncryptedBytes = new byte[totalLen];
		secureRandom.nextBytes(allEncryptedBytes);
		System.arraycopy(salt, 0, allEncryptedBytes, 0, SALT_SIZE);
		allEncryptedBytes[SALT_SIZE] = padLen;
		System.arraycopy(encryptedBytes, 0, allEncryptedBytes, SALT_SIZE + 1, len);
		return allEncryptedBytes;
	}

	public static byte[] decrypt(byte[] encryptedPayload, final String passPhrase) throws Exception {
		byte[] salt = new byte[SALT_SIZE];
		System.arraycopy(encryptedPayload, 0, salt, 0, SALT_SIZE);
		byte padLen = encryptedPayload[SALT_SIZE];
		byte[] encryptedBytes = new byte[encryptedPayload.length - SALT_SIZE - 1 - padLen];
		System.arraycopy(encryptedPayload, SALT_SIZE + 1, encryptedBytes, 0, encryptedBytes.length);
		Cipher cipher = createCipher(passPhrase, salt, Cipher.DECRYPT_MODE);
		return cipher.doFinal(encryptedBytes);
	}

	public static String encrypt(String clearText, String passPhrase) throws Exception {
		byte[] encrypted = encrypt(clearText.getBytes(StandardCharsets.UTF_8), passPhrase);
		return "{" + Base64.encodeBase64(encrypted) + "}";
	}

	public static String decrypt(String base64Encrypted, String passPhrase) throws Exception {
		Matcher matcher = DECORATED_PASSWORD_P.matcher(base64Encrypted);
		if (!matcher.matches()) {
			return null;
		}

		String expr = matcher.group("expr");

		byte[] encryptedPayload = Base64.decodeBase64(expr);
		byte[] payload = decrypt(encryptedPayload, passPhrase);
		return new String(payload, StandardCharsets.UTF_8);
	}

	private static Cipher createCipher(final String passPhrase, byte[] salt, final int mode) throws Exception {

		final MessageDigest digester = MessageDigest.getInstance(DIGEST_ALG);
		byte[] key = new byte[16];
		byte[] iv = new byte[16];
		// sum lengths must be 32 to match SHA-256 digest length

		digester.update(passPhrase.getBytes(StandardCharsets.UTF_8));
		digester.update(salt, 0, 8);
		byte[] digest = digester.digest(); // 32 bytes
		System.arraycopy(digest, 0, key, 0, 16);
		System.arraycopy(digest, 16, iv, 0, 16);
		Cipher cipher = Cipher.getInstance(CIPHER_ALG);
		cipher.init(mode, new SecretKeySpec(key, KEY_ALG), new IvParameterSpec(iv));
		return cipher;
	}

	public static boolean isObfuscatedPassword(String passphrase) {
		return passphrase != null && DECORATED_PASSWORD_P.matcher(passphrase)
			.matches();
	}

}
