package aQute.bnd.service;

public enum Strategy {
	LOWEST,
	EXACT,
	HIGHEST;

	/**
	 * @param str the enum as String.
	 * @return the parsed {@link Strategy} enum. Returns {@link #HIGHEST} if
	 *         <code>null</code> is passed or <code>null</code> if the passed
	 *         string is not one of the valid strategies.
	 */
	public static Strategy parse(String str) {
		if (str == null) {
			return Strategy.HIGHEST;
		}

		if (str.equalsIgnoreCase("HIGHEST"))
			return Strategy.HIGHEST;
		else if (str.equalsIgnoreCase("LOWEST"))
			return Strategy.LOWEST;
		else if (str.equalsIgnoreCase("EXACT"))
			return Strategy.EXACT;
		else {
			return null;
		}
	}
}
