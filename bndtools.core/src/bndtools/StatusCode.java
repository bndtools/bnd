package bndtools;

/**
 * Status codes for IStatus objects. Centralized to avoid reuse of codes.
 */
public enum StatusCode {

	/**
	 * A general code for everything that doesn't need a specific handler.
	 */
	General(0),

	/**
	 * Failure to match a JVM installation to a requested Execution Environment.
	 */
	NoVMForEE(101);

	private final int code;

	private StatusCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
