package aQute.lib.converter;

import java.lang.reflect.*;

public class TypeReference<T> implements Type {

	protected TypeReference() {
		// Make sure it cannot be directly instantiated
	}
	public Type getType() {
		return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
}
