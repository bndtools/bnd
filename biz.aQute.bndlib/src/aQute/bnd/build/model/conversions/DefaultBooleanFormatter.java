package aQute.bnd.build.model.conversions;

/**
 * Formatter for booleans with a default value; if the input value matches the
 * default then it is formatted to <code>null</code>.
 *
 * @author Neil Bartlett
 */
public class DefaultBooleanFormatter implements Converter<String, Boolean> {

	private final boolean defaultValue;

	public DefaultBooleanFormatter(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public String convert(Boolean input) throws IllegalArgumentException {
		String result = null;

		if (input != null && input.booleanValue() != defaultValue)
			result = input.toString();

		return result;
	}

	@Override
	public String error(String msg) {
		return msg;
	}

}
