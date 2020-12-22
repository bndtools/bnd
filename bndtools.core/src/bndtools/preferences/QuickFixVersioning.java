package bndtools.preferences;

public enum QuickFixVersioning {

	noversion,
	latest;

	public static final String				PREFERENCE_KEY	= "quickfixVersioning";
	public static final QuickFixVersioning	DEFAULT			= noversion;

	public static QuickFixVersioning parse(String string) {
		try {
			return valueOf(string);
		} catch (Exception e) {
			return DEFAULT;
		}
	}

}
