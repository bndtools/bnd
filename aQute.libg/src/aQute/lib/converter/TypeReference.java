package aQute.lib.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Reference to a type. This class should be used as an extension of an inner
 * class. E.g.
 *
 * <pre>
 * new TypeReference<List<String>>() {}
 * </pre>
 *
 * The type then gets encoded in the generic super class of the instance.
 */
public class TypeReference<T> implements Type {

	protected TypeReference() {
		// Make sure it cannot be directly instantiated
	}

	public Type getType() {
		return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Override
	public String getTypeName() {
		return toString();
	}

	@Override
	public String toString() {
		return "TypeReference<" + getType().getTypeName() + ">";

	}
}
