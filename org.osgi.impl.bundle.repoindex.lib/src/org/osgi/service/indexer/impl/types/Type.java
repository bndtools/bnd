package org.osgi.service.indexer.impl.types;

import java.util.Collection;

import org.osgi.framework.Version;

class Type {

	private final ScalarType type;
	private final boolean list;

	public static Type scalar(ScalarType type) {
		return new Type(type, false);
	}

	public static Type list(ScalarType type) {
		return new Type(type, true);
	}

	public static Type typeOf(Object value) throws IllegalArgumentException {
		Type result;
		if (value == null) {
			throw new NullPointerException("Null values not supported.");
		} else if (value instanceof Version) {
			result = scalar(ScalarType.Version);
		} else if (value instanceof Double || value instanceof Float) {
			result = scalar(ScalarType.Double);
		} else if (value instanceof Number) {
			result = scalar(ScalarType.Long);
		} else if (value instanceof String) {
			result = scalar(ScalarType.String);
		} else if (value instanceof Boolean) {
			result = scalar(ScalarType.String);
		} else if (value instanceof Collection<?>) {
			Collection<?> coll = (Collection<?>) value;
			if (coll.isEmpty())
				throw new IllegalArgumentException("Cannot determine scalar type of empty collection.");
			Type elemType = typeOf(coll.iterator().next());
			result = list(elemType.type);
		} else {
			throw new IllegalArgumentException("Unsupported type: " + value.getClass());
		}
		return result;
	}

	private Type(ScalarType type, boolean list) {
		this.type = type;
		this.list = list;
	}

	public ScalarType getType() {
		return type;
	}

	public boolean isList() {
		return list;
	}

	@Override
	public String toString() {
		return list ? "List<" + type.name() + ">" : type.name();
	}

	public String convertToString(Object value) {
		String result;
		if (list) {
			Collection<?> coll = (Collection<?>) value;
			StringBuilder buf = new StringBuilder();
			int count = 0;
			for (Object obj : coll) {
				if (count++ > 0)
					buf.append(',');
				buf.append(obj);
			}
			result = buf.toString();
		} else {
			result = value.toString();
		}
		return result;
	}

}
