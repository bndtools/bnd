package bndtools.preferences;

public enum CompileErrorAction {

	delete,
	skip,
	build;

	public static final String				PREFERENCE_KEY	= "compileErrorAction";
	private static final CompileErrorAction	DEFAULT			= skip;

	public static CompileErrorAction parse(String string) {
		try {
			return valueOf(string);
		} catch (Exception e) {
			return DEFAULT;
		}
	}

}
