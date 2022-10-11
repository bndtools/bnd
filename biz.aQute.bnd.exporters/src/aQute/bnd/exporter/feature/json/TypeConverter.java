/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package aQute.bnd.exporter.feature.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import aQute.lib.converter.TypeReference;

//based on org.apache.felix.cm.json.impl.TypeConverter;
public class TypeConverter {

	public static final String							TYPE_BINARY			= "binary";

	public static final String							TYPE_BINARIES		= "binary[]";

	private static final String							TYPE_COLLECTION		= "Collection";

	public static final String							NO_TYPE_INFO		= "";

	public static final Object							CONVERSION_FAILED	= new Object();

	private static final Map<String, Class<?>>			TYPE_MAP			= new LinkedHashMap<>();
	private static final Map<String, TypeReference<?>>	TYPE_COLLECTION_MAP	= new LinkedHashMap<>();
	static {
		// scalar types and primitive types
		TYPE_MAP.put("String", String.class);
		TYPE_MAP.put("Integer", Integer.class);
		TYPE_MAP.put("int", Integer.class);
		TYPE_MAP.put("Long", Long.class);
		TYPE_MAP.put("long", Long.class);
		TYPE_MAP.put("Float", Float.class);
		TYPE_MAP.put("float", Float.class);
		TYPE_MAP.put("Double", Double.class);
		TYPE_MAP.put("double", Double.class);
		TYPE_MAP.put("Byte", Byte.class);
		TYPE_MAP.put("byte", Byte.class);
		TYPE_MAP.put("Short", Short.class);
		TYPE_MAP.put("short", Short.class);
		TYPE_MAP.put("Character", Character.class);
		TYPE_MAP.put("char", Character.class);
		TYPE_MAP.put("Boolean", Boolean.class);
		TYPE_MAP.put("boolean", Boolean.class);
		// array of scalar types and primitive types
		TYPE_MAP.put("String[]", String[].class);
		TYPE_MAP.put("int[]", int[].class);
		TYPE_MAP.put("Integer[]", Integer[].class);
		TYPE_MAP.put("long[]", long[].class);
		TYPE_MAP.put("Long[]", Long[].class);
		TYPE_MAP.put("float[]", float[].class);
		TYPE_MAP.put("Float[]", Float[].class);
		TYPE_MAP.put("double[]", double[].class);
		TYPE_MAP.put("Double[]", Double[].class);
		TYPE_MAP.put("byte[]", byte[].class);
		TYPE_MAP.put("Byte[]", Byte[].class);
		TYPE_MAP.put("short[]", short[].class);
		TYPE_MAP.put("Short[]", Short[].class);
		TYPE_MAP.put("boolean[]", boolean[].class);
		TYPE_MAP.put("Boolean[]", Boolean[].class);
		TYPE_MAP.put("char[]", char[].class);
		TYPE_MAP.put("Character[]", Character[].class);

		// binaries
		TYPE_MAP.put(TYPE_BINARY, String.class);
		TYPE_MAP.put(TYPE_BINARIES, String[].class);

		// Collections of scalar types
		TYPE_COLLECTION_MAP.put("Collection<String>", new TypeReference<ArrayList<String>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Integer>", new TypeReference<ArrayList<Integer>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Long>", new TypeReference<ArrayList<Long>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Float>", new TypeReference<ArrayList<Float>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Double>", new TypeReference<ArrayList<Double>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Byte>", new TypeReference<ArrayList<Byte>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Short>", new TypeReference<ArrayList<Short>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Character>", new TypeReference<ArrayList<Character>>() {});
		TYPE_COLLECTION_MAP.put("Collection<Boolean>", new TypeReference<ArrayList<Boolean>>() {});
	}

	private static String findType(final Class<?> clazz) {
		for (final Map.Entry<String, Class<?>> entry : TYPE_MAP.entrySet()) {
			if (clazz.isAssignableFrom(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Convert an object to a {@code JsonValue}.
	 *
	 * @param value The object to convert
	 * @return A map entry where the key contains the type info and the value
	 *         the converted JsonValue.
	 */
	public static String convertObjectToTyped(final Object value) {
		// native types
		if (value == null || value instanceof Long || value instanceof Double || value instanceof String
			|| value instanceof Boolean) {
			return NO_TYPE_INFO;
		}

		if (value.getClass()
			.isArray()) {
			// arrays
			String typeInfo = findType(value.getClass());
			if (typeInfo != null) {
				if ("String[]".equals(typeInfo) || "Boolean[]".equals(typeInfo) || "Long[]".equals(typeInfo)
					|| "Double[]".equals(typeInfo)) {
					typeInfo = NO_TYPE_INFO;
				}
				return typeInfo;
			}

		} else if (Collection.class.isAssignableFrom(value.getClass())) {
			// collections
			final Collection<?> collection = (Collection<?>) value;
			// get first object to get the type
			String typeInfo = TypeConverter.TYPE_COLLECTION;
			final Iterator<?> i = collection.iterator();
			if (i.hasNext()) {
				final String colType = findType(i.next()
					.getClass());
				if (colType != null) {
					typeInfo = typeInfo.concat("<")
						.concat(colType)
						.concat(">");
				}
			}

			return typeInfo;
		}

		// scalar types - start with special cases for numbers
		if (value instanceof Integer) {
			return "Integer";
		} else if (value instanceof Float) {
			return "Float";
		} else if (value instanceof Short) {
			return "Short";
		} else if (value instanceof Byte) {
			return "Byte";
		} else if (value instanceof Character) {
			return "Character";
		}
		final String typeInfo = findType(value.getClass());
		if (typeInfo != null) {
			return typeInfo;
		}

		return NO_TYPE_INFO;
	}

}
