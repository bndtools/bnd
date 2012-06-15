package aQute.lib.converter;

import java.lang.reflect.*;

public class TypeReference<T> implements Type {

	public Type getType() {
		return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
}
