package aQute.bnd.build.model.conversions;

public class EnumConverter<E extends Enum<E>> implements Converter<E,String> {

	private final Class<E>	enumType;
	private final E			defaultValue;

	public static <E extends Enum<E>> EnumConverter<E> create(Class<E> enumType) {
		return new EnumConverter<E>(enumType, null);
	}

	public static <E extends Enum<E>> EnumConverter<E> create(Class<E> enumType, E defaultValue) {
		return new EnumConverter<E>(enumType, defaultValue);
	}

	private EnumConverter(Class<E> enumType, E defaultValue) {
		this.enumType = enumType;
		this.defaultValue = defaultValue;
	}

	public E convert(String input) throws IllegalArgumentException {
		if (input == null)
			return defaultValue;
		return Enum.valueOf(enumType, input);
	}

}
