package aQute.bnd.annotation;

public final class Constants {

	private Constants() {}

	/**
	 * Not emitted if default is used
	 */
	public static final String	CARDINALITY_MACRO	= "${if;${is;${#cardinality};default};;cardinality:=${#cardinality}}";

	/**
	 * Not emitted if default is used
	 */
	public static final String	EFFECTIVE_MACRO		= "${if;${size;${#effective}};effective:=${#effective}}";

	/**
	 * Not emitted if default is used
	 */
	public static final String	RESOLUTION_MACRO	= "${if;${is;${#resolution};default};;resolution:=${#resolution}}";

	/**
	 * Not emitted if the list is empty
	 */
	public static final String	USES_MACRO			= "${if;${size;${#uses}};uses:='${#uses}'}";

}
