package bndtools;

public class UIConstants {

	private static final char[] AUTO_ACTIVATION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890._" //$NON-NLS-1$
		.toCharArray();

	public static final char[] autoActivationCharacters() {
		char[] result = new char[AUTO_ACTIVATION_CHARS.length];
		System.arraycopy(AUTO_ACTIVATION_CHARS, 0, result, 0, AUTO_ACTIVATION_CHARS.length);
		return result;
	}

}
