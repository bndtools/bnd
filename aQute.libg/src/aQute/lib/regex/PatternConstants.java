package aQute.lib.regex;

public class PatternConstants {
	private PatternConstants() {}

	public final static String	TOKEN			= "[-\\w]+";
	public final static String	SYMBOLICNAME	= TOKEN + "(:?\\." + TOKEN + ")*";

	public final static String	SHA1			= "\\p{XDigit}{40}";

}
