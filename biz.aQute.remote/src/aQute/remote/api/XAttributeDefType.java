package aQute.remote.api;

import java.util.Collections;
import java.util.List;

public enum XAttributeDefType {

	STRING,
	STRING_ARRAY,
	STRING_LIST,
	INTEGER,
	INTEGER_ARRAY,
	INTEGER_LIST,
	BOOLEAN,
	BOOLEAN_ARRAY,
	BOOLEAN_LIST,
	DOUBLE,
	DOUBLE_ARRAY,
	DOUBLE_LIST,
	FLOAT,
	FLOAT_ARRAY,
	FLOAT_LIST,
	CHAR,
	CHAR_ARRAY,
	CHAR_LIST,
	LONG,
	LONG_ARRAY,
	LONG_LIST,
	PASSWORD;

	public static XAttributeDefType getType(final Object value) {
		final Class<?> clazz = value.getClass();
		if (clazz.equals(String.class)) {
			return XAttributeDefType.STRING;
		}
		if (clazz.equals(String[].class)) {
			return XAttributeDefType.STRING_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(String.class)) {
					return XAttributeDefType.STRING_LIST;
				}
			}
		}

		if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
			return XAttributeDefType.INTEGER;
		}
		if (clazz.equals(int[].class)) {
			return XAttributeDefType.INTEGER_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Integer.class)) {
					return XAttributeDefType.INTEGER_LIST;
				}
			}
		}

		if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
			return XAttributeDefType.BOOLEAN;
		}
		if (clazz.equals(boolean[].class)) {
			return XAttributeDefType.BOOLEAN_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Boolean.class)) {
					return XAttributeDefType.BOOLEAN_LIST;
				}
			}
		}

		if (clazz.equals(double.class) || clazz.equals(Double.class)) {
			return XAttributeDefType.DOUBLE;
		}
		if (clazz.equals(double[].class)) {
			return XAttributeDefType.DOUBLE_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Double.class)) {
					return XAttributeDefType.DOUBLE_LIST;
				}
			}
		}

		if (clazz.equals(float.class) || clazz.equals(Float.class)) {
			return XAttributeDefType.FLOAT;
		}
		if (clazz.equals(float[].class)) {
			return XAttributeDefType.FLOAT_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Float.class)) {
					return XAttributeDefType.FLOAT_LIST;
				}
			}
		}

		if (clazz.equals(char.class) || clazz.equals(Character.class)) {
			return XAttributeDefType.CHAR;
		}
		if (clazz.equals(char[].class)) {
			return XAttributeDefType.CHAR_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Character.class)) {
					return XAttributeDefType.CHAR_LIST;
				}
			}
		}

		if (clazz.equals(long.class) || clazz.equals(Long.class)) {
			return XAttributeDefType.LONG;
		}
		if (clazz.equals(long[].class)) {
			return XAttributeDefType.LONG_ARRAY;
		}
		if (value instanceof List<?>) {
			final List<?> list = (List<?>) value;
			if (!list.isEmpty()) {
				final Object element = list.get(0);
				if (element.getClass()
					.equals(Long.class)) {
					return XAttributeDefType.LONG_LIST;
				}
			}
		}
		return XAttributeDefType.STRING;
	}

	public static Class<?> clazz(final XAttributeDefType type) {
		switch (type) {
			case STRING :
				return String.class;
			case STRING_ARRAY :
				return String[].class;
			case STRING_LIST :
				return Collections.<String> emptyList()
					.getClass();
			case INTEGER :
				return Integer.class;
			case INTEGER_ARRAY :
				return int[].class;
			case INTEGER_LIST :
				return Collections.<Integer> emptyList()
					.getClass();
			case BOOLEAN :
				return Boolean.class;
			case BOOLEAN_ARRAY :
				return boolean[].class;
			case BOOLEAN_LIST :
				return Collections.<Boolean> emptyList()
					.getClass();
			case DOUBLE :
				return Double.class;
			case DOUBLE_ARRAY :
				return double[].class;
			case DOUBLE_LIST :
				return Collections.<Double> emptyList()
					.getClass();
			case FLOAT :
				return Float.class;
			case FLOAT_ARRAY :
				return float[].class;
			case FLOAT_LIST :
				return Collections.<Float> emptyList()
					.getClass();
			case CHAR :
				return Character.class;
			case CHAR_ARRAY :
				return char[].class;
			case CHAR_LIST :
				return Collections.<Character> emptyList()
					.getClass();
			case LONG :
				return Long.class;
			case LONG_ARRAY :
				return long[].class;
			case LONG_LIST :
				return Collections.<Long> emptyList()
					.getClass();
			default :
				return String.class;
		}
	}

}
