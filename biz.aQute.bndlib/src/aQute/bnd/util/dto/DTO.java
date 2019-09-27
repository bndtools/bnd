package aQute.bnd.util.dto;

/*
 * Copyright (c) OSGi Alliance (2012, 2017). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Super type for Data Transfer Objects.
 * <p>
 * A Data Transfer Object (DTO) is easily serializable having only public fields
 * of primitive types and their wrapper classes, Strings, enums, Version, and
 * DTOs. List, Set, Map, and array aggregates may also be used. The aggregates
 * must only hold objects of the listed types or aggregates.
 * <p>
 * The object graph from a Data Transfer Object must be a tree to simplify
 * serialization and deserialization.
 *
 * @author $Id$
 * @NotThreadSafe
 */
public abstract class DTO {

	/**
	 * Return a string representation of this DTO suitable for use when
	 * debugging.
	 * <p>
	 * The format of the string representation is not specified and subject to
	 * change.
	 *
	 * @return A string representation of this DTO suitable for use when
	 *         debugging.
	 */
	@Override
	public String toString() {
		return appendValue(new StringBuilder(), new IdentityHashMap<Object, String>(), "#", this).toString();
	}

	/**
	 * Append the specified DTO's string representation to the specified
	 * StringBuilder.
	 *
	 * @param result StringBuilder to which the string representation is
	 *            appended.
	 * @param objectRefs References to "seen" objects.
	 * @param refpath The reference path of the specified DTO.
	 * @param dto The DTO whose string representation is to be appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendDTO(final StringBuilder result, final Map<Object, String> objectRefs,
		final String refpath, final DTO dto) {
		result.append("{");
		String delim = "";
		for (Field field : dto.getClass()
			.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			result.append(delim);
			final String name = field.getName();
			appendString(result, name);
			result.append(":");
			Object value = null;
			try {
				value = field.get(dto);
			} catch (IllegalAccessException e) {
				// use null value;
			}
			appendValue(result, objectRefs, refpath + "/" + name, value);
			delim = ", ";
		}
		result.append("}");
		return result;
	}

	/**
	 * Append the specified value's string representation to the specified
	 * StringBuilder.
	 * <p>
	 * This method handles cycles in the object graph, using path-based
	 * references, even though the specification requires the object graph from
	 * a DTO to be a tree.
	 *
	 * @param result StringBuilder to which the string representation is
	 *            appended.
	 * @param objectRefs References to "seen" objects.
	 * @param refpath The reference path of the specified value.
	 * @param value The object whose string representation is to be appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendValue(final StringBuilder result, final Map<Object, String> objectRefs,
		final String refpath, final Object value) {
		if (value == null) {
			return result.append("null");
		}
		// Simple Java types
		if (value instanceof String || value instanceof Character) {
			return appendString(result, compress(value.toString()));
		}
		if (value instanceof Number || value instanceof Boolean) {
			return result.append(value.toString());
		}
		if (value instanceof Enum) {
			return appendString(result, ((Enum<?>) value).name());
		}
		if ("org.osgi.framework.Version".equals(value.getClass()
			.getName())) {
			return appendString(result, value.toString());
		}
		if ("aQute.bnd.version.Version".equals(value.getClass()
			.getName())) {
			return appendString(result, value.toString());
		}

		// Complex types
		final String path = objectRefs.get(value);
		if (path != null) {
			result.append("{\"$ref\":");
			appendString(result, path);
			result.append("}");
			return result;
		}
		objectRefs.put(value, refpath);

		if (value instanceof DTO) {
			return appendDTO(result, objectRefs, refpath, (DTO) value);
		}
		if (value instanceof Map) {
			return appendMap(result, objectRefs, refpath, (Map<?, ?>) value);
		}
		if (value instanceof List || value instanceof Set) {
			return appendIterable(result, objectRefs, refpath, (Iterable<?>) value);
		}
		if (value.getClass()
			.isArray()) {
			return appendArray(result, objectRefs, refpath, value);
		}
		return appendString(result, compress(value.toString()));
	}

	/**
	 * Append the specified array's string representation to the specified
	 * StringBuilder.
	 *
	 * @param result StringBuilder to which the string representation is
	 *            appended.
	 * @param objectRefs References to "seen" objects.
	 * @param refpath The reference path of the specified array.
	 * @param array The array whose string representation is to be appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendArray(final StringBuilder result, final Map<Object, String> objectRefs,
		final String refpath, final Object array) {
		result.append("[");
		final int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				result.append(",");
			}
			appendValue(result, objectRefs, refpath + "/" + i, Array.get(array, i));
		}
		result.append("]");
		return result;
	}

	/**
	 * Append the specified iterable's string representation to the specified
	 * StringBuilder.
	 *
	 * @param result StringBuilder to which the string representation is
	 *            appended.
	 * @param objectRefs References to "seen" objects.
	 * @param refpath The reference path of the specified list.
	 * @param iterable The iterable whose string representation is to be
	 *            appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendIterable(final StringBuilder result, final Map<Object, String> objectRefs,
		final String refpath, final Iterable<?> iterable) {
		result.append("[");
		int i = 0;
		for (Object item : iterable) {
			if (i > 0) {
				result.append(",");
			}
			appendValue(result, objectRefs, refpath + "/" + i, item);
			i++;
		}
		result.append("]");
		return result;
	}

	/**
	 * Append the specified map's string representation to the specified
	 * StringBuilder.
	 *
	 * @param result StringBuilder to which the string representation is
	 *            appended.
	 * @param objectRefs References to "seen" objects.
	 * @param refpath The reference path of the specified map.
	 * @param map The map whose string representation is to be appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendMap(final StringBuilder result, final Map<Object, String> objectRefs,
		final String refpath, final Map<?, ?> map) {
		result.append("{");
		String delim = "";
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			result.append(delim);
			final String name = String.valueOf(entry.getKey());
			appendString(result, name);
			result.append(":");
			final Object value = entry.getValue();
			appendValue(result, objectRefs, refpath + "/" + name, value);
			delim = ", ";
		}
		result.append("}");
		return result;
	}

	/**
	 * Append the specified string to the specified StringBuilder.
	 *
	 * @param result StringBuilder to which the string is appended.
	 * @param string The string to be appended.
	 * @return The specified StringBuilder.
	 */
	private static StringBuilder appendString(final StringBuilder result, final CharSequence string) {
		result.append("\"");
		int i = result.length();
		result.append(string);
		while (i < result.length()) { // escape if necessary
			char c = result.charAt(i);
			if ((c == '"') || (c == '\\')) {
				result.insert(i, '\\');
				i = i + 2;
				continue;
			}
			if (c < 0x20) {
				result.insert(i + 1, Integer.toHexString(c | 0x10000));
				result.replace(i, i + 2, "\\u");
				i = i + 6;
				continue;
			}
			i++;
		}
		result.append("\"");
		return result;
	}

	/**
	 * Compress, in length, the specified string.
	 *
	 * @param in The string to potentially compress.
	 * @return The string compressed, if necessary.
	 */
	private static CharSequence compress(final CharSequence in) {
		final int length = in.length();
		if (length <= 21) {
			return in;
		}
		StringBuilder result = new StringBuilder(21);
		result.append(in, 0, 9);
		result.append("...");
		result.append(in, length - 9, length);
		return result;
	}
}
