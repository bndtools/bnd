package aQute.lib.converter;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import aQute.lib.base64.*;

/**
 * General Java type converter from an object to any type. Supports number
 * conversion
 * 
 * @author aqute
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" }) public class Converter {
	boolean	fatal	= true;

	public <T> T convert(Class<T> type, Object o) throws Exception {
		return (T) convert((Type) type, o);
	}

	public Object convert(Type type, Object o) throws Exception {
		if (o == null)
			return null; // compatible with any

		Class resultType = getRawClass(type);
		Class<?> actualType = o.getClass();
		// Is it a compatible type?
		if (resultType.isAssignableFrom(actualType))
			return o;

		// We can always make a string

		if (resultType == String.class) {
			if (actualType.isArray()) {
				if (actualType == char[].class)
					return new String((char[]) o);
				if (actualType == byte[].class)
					return Base64.encodeBase64((byte[]) o);
				int l = Array.getLength(o);
				StringBuilder sb = new StringBuilder("[");
				String del = "";
				for (int i = 0; i < l; i++) {
					sb.append(del);
					del = ",";
					sb.append(convert(String.class, Array.get(o, i)));
				}
				sb.append("]");
				return sb.toString();
			}
			return o.toString();
		}

		if (Collection.class.isAssignableFrom(resultType))
			return collection(type, resultType, o);

		if (Map.class.isAssignableFrom(resultType))
			return map(type, resultType, o);

		if (type instanceof GenericArrayType) {
			GenericArrayType gType = (GenericArrayType) type;
			return array(gType.getGenericComponentType(), o);
		}

		if (resultType.isArray()) {
			if (actualType == String.class) {
				String s = (String) o;
				if (byte[].class == resultType)
					return Base64.decodeBase64(s);

				if (char[].class == resultType)
					return s.toCharArray();
			}
			if (byte[].class == resultType) {
				// Sometimes classes implement toByteArray
				try {
					Method m = actualType.getMethod("toByteArray");
					if (m.getReturnType() == byte[].class)
						return m.invoke(o);

				} catch (Exception e) {
					// Ignore
				}
			}

			return array(resultType.getComponentType(), o);
		}

		// Simple type coercion

		if (resultType == boolean.class || resultType == Boolean.class) {
			if (actualType == boolean.class || actualType == Boolean.class)
				return o;
			Number n = number(o);
			if (n != null)
				return n.longValue() == 0 ? false : true;

			resultType = Boolean.class;
		} else if (resultType == byte.class || resultType == Byte.class) {
			Number n = number(o);
			if (n != null)
				return n.byteValue();
			resultType = Byte.class;
		} else if (resultType == char.class || resultType == Character.class) {
			Number n = number(o);
			if (n != null)
				return (char) n.shortValue();
			resultType = Character.class;
		} else if (resultType == short.class || resultType == Short.class) {
			Number n = number(o);
			if (n != null)
				return n.shortValue();

			resultType = Short.class;
		} else if (resultType == int.class || resultType == Integer.class) {
			Number n = number(o);
			if (n != null)
				return n.intValue();

			resultType = Integer.class;
		} else if (resultType == long.class || resultType == Long.class) {
			Number n = number(o);
			if (n != null)
				return n.longValue();

			resultType = Long.class;
		} else if (resultType == float.class || resultType == Float.class) {
			Number n = number(o);
			if (n != null)
				return n.floatValue();

			resultType = Float.class;
		} else if (resultType == double.class || resultType == Double.class) {
			Number n = number(o);
			if (n != null)
				return n.doubleValue();

			resultType = Double.class;
		}

		assert !resultType.isPrimitive();

		if (actualType == String.class) {
			String input = (String) o;
			if (resultType == char[].class)
				return input.toCharArray();

			if (resultType == byte[].class)
				return Base64.decodeBase64(input);

			if (Enum.class.isAssignableFrom(resultType)) {
				return Enum.valueOf((Class<Enum>) resultType, input);
			}
			if (resultType == Pattern.class) {
				return Pattern.compile(input);
			}

			try {
				Constructor<?> c = resultType.getConstructor(String.class);
				return c.newInstance(o.toString());
			} catch (Throwable t) {
			}
			try {
				Method m = resultType.getMethod("valueOf", String.class);
				if (Modifier.isStatic(m.getModifiers()))
					return m.invoke(null, o.toString());
			} catch (Throwable t) {
			}

			if (resultType == Character.class && input.length() == 1)
				return input.charAt(0);
		}
		Number n = number(o);
		if (n != null) {
			if (Enum.class.isAssignableFrom(resultType)) {
				try {
					Method values = resultType.getMethod("values");
					Enum[] vs = (Enum[]) values.invoke(null);
					int nn = n.intValue();
					if (nn > 0 && nn < vs.length)
						return vs[nn];
				} catch (Exception e) {
					// Ignore
				}
			}
		}
		return error("No conversion found for " + o.getClass() + " to " + type);
	}

	private Number number(Object o) {
		if (o instanceof Number)
			return (Number) o;

		if (o instanceof Boolean)
			return ((Boolean) o).booleanValue() ? 1 : 0;

		if (o instanceof Character)
			return (int) ((Character) o).charValue();

		if (o instanceof String) {
			String s = (String) o;
			try {
				return Double.parseDouble(s);
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	private Collection collection(Type collectionType, Class<? extends Collection> rawClass,
			Object o) throws Exception {
		Collection collection;
		if (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers())) {
			if (rawClass.isAssignableFrom(ArrayList.class))
				collection = new ArrayList();
			else if (rawClass.isAssignableFrom(HashSet.class))
				collection = new HashSet();
			else if (rawClass.isAssignableFrom(TreeSet.class))
				collection = new TreeSet();
			else if (rawClass.isAssignableFrom(LinkedList.class))
				collection = new LinkedList();
			else if (rawClass.isAssignableFrom(Vector.class))
				collection = new Vector();
			else if (rawClass.isAssignableFrom(Stack.class))
				collection = new Stack();
			else if (rawClass.isAssignableFrom(ConcurrentLinkedQueue.class))
				collection = new ConcurrentLinkedQueue();
			else
				return (Collection) error("Cannot find a suitable collection for the collection interface "
						+ rawClass);
		} else
			collection = rawClass.newInstance();

		Type subType = Object.class;
		if (collectionType instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) collectionType;
			subType = ptype.getActualTypeArguments()[0];
		}

		Collection input = toCollection(o);

		for (Object i : input)
			collection.add(convert(subType, i));

		return collection;
	}

	private Map map(Type mapType, Class<? extends Map<?, ?>> rawClass, Object o) throws Exception {
		Map result;
		if (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers())) {
			if (rawClass.isAssignableFrom(HashMap.class))
				result = new HashMap();
			else if (rawClass.isAssignableFrom(TreeMap.class))
				result = new TreeMap();
			else if (rawClass.isAssignableFrom(ConcurrentHashMap.class))
				result = new ConcurrentHashMap();
			else
				return (Map) error("Cannot find suitable map for map interface " + rawClass);
		} else
			result = rawClass.newInstance();

		Map<?, ?> input = toMap(o);

		Type keyType = Object.class;
		Type valueType = Object.class;
		if (mapType instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) mapType;
			keyType = ptype.getActualTypeArguments()[0];
			valueType = ptype.getActualTypeArguments()[1];
		}

		for (Map.Entry<?, ?> entry : input.entrySet()) {
			Object key = convert(keyType, entry.getKey());
			Object value = convert(valueType, entry.getValue());
			if (value == null)
				return (Map) error("Key for map must not be null");
			result.put(key, value);
		}

		return result;
	}

	public Object array(Type type, Object o) throws Exception {
		Collection<?> input = toCollection(o);
		Class<?> componentClass = getRawClass(type);
		Object array = Array.newInstance(componentClass, input.size());

		int i = 0;
		for (Object next : input) {
			Array.set(array, i++, convert(type, next));
		}
		return array;
	}

	private Class<?> getRawClass(Type type) {
		if (type instanceof Class)
			return (Class<?>) type;

		if (type instanceof ParameterizedType)
			return (Class<?>) ((ParameterizedType) type).getRawType();

		if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			return Array.newInstance(getRawClass(componentType), 0).getClass();
		}

		if (type instanceof TypeVariable) {
			Type componentType = ((TypeVariable) type).getBounds()[0];
			return Array.newInstance(getRawClass(componentType), 0).getClass();
		}

		if (type instanceof WildcardType) {
			Type componentType = ((WildcardType) type).getUpperBounds()[0];
			return Array.newInstance(getRawClass(componentType), 0).getClass();
		}

		return Object.class;
	}

	public Collection<?> toCollection(Object o) {
		if (o instanceof Collection)
			return (Collection<?>) o;

		if (o.getClass().isArray()) {
			if (o.getClass().getComponentType().isPrimitive()) {
				int length = Array.getLength(o);
				List<Object> result = new ArrayList<Object>(length);
				for (int i = 0; i < length; i++) {
					result.add(Array.get(o, i));
				}
				return result;
			}
			return Arrays.asList((Object[]) o);
		}

		return Arrays.asList(o);
	}

	public Map<?, ?> toMap(Object o) throws Exception {
		if (o instanceof Map)
			return (Map<?, ?>) o;
		Map result = new HashMap();
		Field fields[] = o.getClass().getFields();
		for (Field f : fields)
			result.put(f.getName(), f.get(o));
		if (result.isEmpty())
			return null;

		return result;
	}

	private Object error(String string) {
		if (fatal)
			throw new IllegalArgumentException(string);
		return null;
	}

	public void setFatalIsException(boolean b) {
		fatal = b;
	}
}
