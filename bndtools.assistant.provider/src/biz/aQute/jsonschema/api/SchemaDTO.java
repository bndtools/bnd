package biz.aQute.jsonschema.api;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public record SchemaDTO(String $ref, String title, String description, DataType type, Map<String, SchemaDTO> properties,
		List<String> required, SchemaDTO additionalProperties, SchemaDTO items, String[] enum__) {

	final static Set<String> keywords = Set.of("abstract", "assert", "boolean",
			"break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else",
			"enum", "exports", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import",
			"instanceof", "int", "interface", "long", "module", "native", "new", "package", "private", "protected",
			"public", "requires", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
			"throw", "throws", "transient", "try", "var", "void", "volatile", "while", "true", "false", "null", "_",
			"record", "sealed", "non-sealed", "permits");
	public static final String KEYWORD_SUFFIX = "__";

	final static Set<Class> number = Set.of(byte.class, Byte.class, short.class, Short.class, int.class, Integer.class,
		long.class, Long.class, float.class, Float.class, double.class, Double.class, BigInteger.class,
		BigDecimal.class);

	public enum DataType {
		BOOLEAN, OBJECT, ARRAY, NUMBER, STRING;

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public static SchemaDTO createSchemaFromType(String path, String title, String description, Type javaType) {
		return createSchemaFromType(path, title, description, javaType, new IdentityHashMap<>());
	}

	public static SchemaDTO createSchemaFromType(String path, String title, String description, Type javaType,
			Map<Type, SchemaDTO> visited) {

		SchemaDTO previous = visited.get(javaType);
		if (previous != null) {
			return previous;
		}
		visited.put(javaType, new SchemaDTO(path, null, null, null, null, null, null, null, null));

		DataType type;
		Map<String, SchemaDTO> properties = null;
		SchemaDTO additionalProperties = null;
		List<String> required = null;
		SchemaDTO items = null;
		String[] enum__ = null;

		if (javaType == String.class) {
			type = DataType.STRING;
		} else if (javaType == Boolean.class || javaType == boolean.class) {
			type = DataType.BOOLEAN;
		} else if (number.contains(javaType)) {
			type = DataType.NUMBER;
		} else if (javaType instanceof Class clazz) {
			if (title == null)
				title = clazz.getSimpleName();

			if (clazz.isEnum()) {
				type = DataType.STRING;
				enum__ = Stream.of(clazz.getEnumConstants()).map(o -> o.toString()).toArray(String[]::new);
			} else if (clazz.isRecord()) {
				type = DataType.OBJECT;
				properties = new LinkedHashMap<>();
				required = new ArrayList<>();
				for (RecordComponent rc : clazz.getRecordComponents()) {
					String name = keyword(rc.getName());
					SchemaDTO propertySchema = createSchemaFromType(path(path, name), name,
							getDescription(rc),
							rc.getGenericType(), visited);
					properties.put(name, propertySchema);
					required.add(name);
				}
			} else if (clazz.getFields().length > 0) {
				type = DataType.OBJECT;
				properties = new LinkedHashMap<>();
				required = new ArrayList<>();
				String propPath = path(path, "properties");
				if (description == null)
					description = clazz.getSimpleName();
				for (Field field : clazz.getDeclaredFields()) {
					int modifiers = field.getModifiers();
					if (Modifier.isStatic(modifiers) || Modifier.isNative(modifiers) || !Modifier.isPublic(modifiers))
						continue;

					String name = keyword(field.getName());
					SchemaDTO propertySchema = createSchemaFromType(path(propPath, name),
							name, getDescription(field),
							field.getGenericType(), visited);
					properties.put(name, propertySchema);
					required.add(name);
				}
			} else if (clazz.isArray()) {
				type = DataType.ARRAY;
				items = createSchemaFromType(path(path, "items"), null, null, clazz.getComponentType(), visited);
			} else if (Enum.class.isAssignableFrom(clazz)) {
				type = DataType.STRING;
				enum__ = Stream.of(clazz.getEnumConstants()).map(e -> e.toString().toLowerCase())
						.toArray(String[]::new);
			} else {
				type = DataType.STRING;
			}
		} else if (javaType instanceof ParameterizedType ptype) {
			Class rawType = (Class) ptype.getRawType();
			Type[] typeArguments = ptype.getActualTypeArguments();

			if (Map.class.isAssignableFrom(rawType)) {
				type = DataType.OBJECT;
				Type k = typeArguments[0];
				assert k == String.class;
				Type v = typeArguments[1];
				additionalProperties = createSchemaFromType(path(path, "additionalProperties"), null, null, v, visited);
			} else if (Iterable.class.isAssignableFrom(rawType)) {
				type = DataType.ARRAY;
				Type componentType = ptype.getActualTypeArguments()[0];
				items = createSchemaFromType(path(path, "items"), null, null, componentType, visited);
			} else {
				throw new UnsupportedOperationException("do not know how to map to JSON: " + javaType);
			}
		} else if (javaType instanceof GenericArrayType arrayType) {
			type = DataType.ARRAY;
			items = createSchemaFromType(path(path, "items"), null, null, arrayType.getGenericComponentType(), visited);
		} else {
			type = DataType.STRING;
		}
		return new SchemaDTO(null, title, description, type, properties, required, additionalProperties, items, enum__);
	}

	private static String path(String path, String name) {
		return path.concat("/").concat(name);
	}

	public static SchemaDTO createSchemaFromMethod(String path, Method method) {
		Map<String, SchemaDTO> properties = new LinkedHashMap<>();
		for (Parameter parameter : method.getParameters()) {
			SchemaDTO s = createSchemaFromType(path, parameter.getName(), getDescription(parameter),
					parameter.getParameterizedType());
			properties.put(parameter.getName(), s);
		}
		return new SchemaDTO(null, method.getName(), getDescription(method), DataType.OBJECT, properties,
				properties.keySet().stream().toList(), null, null, null);
	}

	private static String getDescription(AnnotatedElement field) {
		Description annotation = field.getAnnotation(Description.class);
		if (annotation == null || annotation.value().isBlank())
			return null;

		return annotation.value();
	}

	/**
	 * This maps a name of a Java construct, which cannot contain Java keywords,
	 * to a keyword if it ends with a {@link #KEYWORD_SUFFIX} and the name
	 * without the suffix maps to a Java keyword.
	 *
	 * @param name
	 *            the name
	 * @return either the name when it wasn't a keyword or a keyword
	 */
	public static String keyword(String name) {
		if (name.endsWith(KEYWORD_SUFFIX)) {
			String keyword = name.substring(0, name.length() - KEYWORD_SUFFIX.length());
			if (keywords.contains(keyword))
				return keyword;
		}
		return name;
	}

}
