package aQute.bnd.help;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.stream.MapStream;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;

/**
 * Special handling of the instruction types like Parameters, Attrs, and the
 * type safe interfaces related to this.
 */
class SpecialConverter extends Converter {
	private static final Object[] EMPTY = new Object[0];

	SpecialConverter() {

		hook(null, (type, object) -> {

			if (!(object instanceof String))
				return null;

			if (isSyntaxInterface(type)) {
				Attrs attrs = OSGiHeader.parseProperties((String) object);
				return AttrsHandler.getProperties(attrs, (Class<?>) type);
			}

			String value = (String) object;

			if (type == Attrs.class)
				return OSGiHeader.parseProperties(value);

			if (type == Parameters.class)
				return new Parameters(value);

			// Handle Map and Iterable

			if (type instanceof ParameterizedType) {

				ParameterizedType ptype = (ParameterizedType) type;
				Class<?> rawClass = (Class<?>) ptype.getRawType();

				if (rawClass == Map.class) {

					if (ptype.getActualTypeArguments()[0] == String.class) {

						Type valueType = ptype.getActualTypeArguments()[1];

						Class<?> valueClass = (Class<?>) valueType;
						Parameters parameters = new Parameters(value);

						return parameters.stream()
							.mapValue(v -> AttrsHandler.getProperties(v, valueClass))
							.collect(MapStream.toMap());
					}
				} else if (Iterable.class.isAssignableFrom(rawClass)) {
					List<String> parts = Strings.split(value);
					return convert(type, parts);
				}
			}
			return null;
		});
	}

	/**
	 * Guess if this interface is a map to type interface. This is basically any
	 * interface - interfaces that extend of Map and Iterable.
	 *
	 * @param type the type to guess
	 * @return true if this is likely an interface that can be used for
	 *         converting a map to a type.
	 */
	public static boolean isSyntaxInterface(Type type) {
		if (!(type instanceof Class))
			return false;

		Class<?> clazz = (Class<?>) type;
		return clazz.isInterface() && !(Iterable.class.isAssignableFrom(clazz)) && !(Map.class.isAssignableFrom(clazz));
	}

	Object convertNeverNull(Type type, Object value) throws Exception {
		value = super.convert(type, value);

		if (value == null) {
			if (type instanceof ParameterizedType) {

				ParameterizedType pType = (ParameterizedType) type;
				Type rawType = pType.getRawType();

				if (rawType instanceof Class) {
					Class<?> rawClass = (Class<?>) rawType;
					if (Map.class.isAssignableFrom(rawClass)) {
						return super.convert(type, Collections.emptyMap());
					} else if (Iterable.class.isAssignableFrom(rawClass)) {
						return super.convert(type, EMPTY);
					}
				}
			} else if (isSyntaxInterface(type)) {
				return AttrsHandler.getProperties(new Attrs(), (Class<?>) type);
			} else if (type == Parameters.class)
				return new Parameters();
			else if (type == Attrs.class)
				return new Attrs();
		}
		return value;
	}

}
