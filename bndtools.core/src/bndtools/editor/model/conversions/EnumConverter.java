package bndtools.editor.model.conversions;

public class EnumConverter<E extends Enum<E>> implements Converter<E, String> {

    private final Class<E> enumType;

    public static <E extends Enum<E>> EnumConverter<E> create(Class<E> enumType) {
        return new EnumConverter<E>(enumType);
    }

    private EnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    public E convert(String input) throws IllegalArgumentException {
        return Enum.valueOf(enumType, input);
    }

}
