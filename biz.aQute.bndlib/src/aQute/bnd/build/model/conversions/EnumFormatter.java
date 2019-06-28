package aQute.bnd.build.model.conversions;

/**
 * Formats an enum type. Outputs {@code null} when the value of the enum is
 * equal to a default value.
 *
 * @param <E>
 * @author Neil Bartlett
 */
public class EnumFormatter<E extends Enum<E>> implements Converter<String, E> {

	private final E defaultValue;

	/**
	 * Construct a new formatter with no default value, i.e. any non-null value
	 * of the enum will print that value.
	 *
	 * @param enumType The enum type.
	 */
	public static <E extends Enum<E>> EnumFormatter<E> create(Class<E> enumType) {
		return new EnumFormatter<>(null);
	}

	/**
	 * Construct a new formatter with the specified default value.
	 *
	 * @param enumType The enum type.
	 * @param defaultValue The default value, which will never be output.
	 */
	public static <E extends Enum<E>> EnumFormatter<E> create(Class<E> enumType, E defaultValue) {
		return new EnumFormatter<>(defaultValue);
	}

	private EnumFormatter(E defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public String convert(E input) throws IllegalArgumentException {
		String result;
		if (input == defaultValue || input == null)
			result = null;
		else {
			result = input.toString();
		}
		return result;
	}

	@Override
	public String error(String msg) {
		return msg;
	}

}
