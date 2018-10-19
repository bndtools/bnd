package aQute.bnd.annotation.metatype;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@SuppressWarnings({
	"unchecked", "rawtypes"
})
public class Configurable<T> {
	public static Pattern		SPLITTER_P					= Pattern.compile("(?<!\\\\)\\|");
	private static final String	BND_ANNOTATION_CLASS_NAME	= "aQute.bnd.osgi.Annotation";
	private static final String	BND_ANNOTATION_METHOD_NAME	= "getAnnotation";

	public static <T> T createConfigurable(Class<T> c, Map<?, ?> properties) {
		Object o = Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] {
			c
		}, new ConfigurableHandler(properties, c.getClassLoader()));
		return c.cast(o);
	}

	public static <T> T createConfigurable(Class<T> c, Dictionary<?, ?> properties) {
		Map<Object, Object> alt = new HashMap<>();
		for (Enumeration<?> e = properties.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			alt.put(key, properties.get(key));
		}
		return createConfigurable(c, alt);
	}

	static class ConfigurableHandler implements InvocationHandler {
		final Map<?, ?>		properties;
		final ClassLoader	loader;

		ConfigurableHandler(Map<?, ?> properties, ClassLoader loader) {
			this.properties = properties;
			this.loader = loader;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Meta.AD ad = method.getAnnotation(Meta.AD.class);
			String id = Configurable.mangleMethodName(method.getName());

			if (ad != null && !ad.id()
				.equals(Meta.NULL))
				id = ad.id();

			Object o = properties.get(id);

			if (o == null) {
				if (ad != null) {
					if (ad.required())
						throw new IllegalStateException("Attribute is required but not set " + method.getName());

					o = ad.deflt();
					if (o.equals(Meta.NULL))
						o = null;
				}
			}
			if (o == null) {
				Class<?> rt = method.getReturnType();
				if (rt == boolean.class)
					return false;

				if (method.getReturnType()
					.isPrimitive()) {

					o = "0";
				} else
					return null;
			}

			return convert(method.getGenericReturnType(), o);
		}

		public Object convert(Type type, Object o) throws Exception {
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				return convert(pType, o);
			}

			if (type instanceof GenericArrayType) {
				GenericArrayType gType = (GenericArrayType) type;
				return convertArray(gType.getGenericComponentType(), o);
			}

			Class<?> resultType = (Class<?>) type;

			if (resultType.isArray()) {
				return convertArray(resultType.getComponentType(), o);
			}

			Class<?> actualType = o.getClass();
			if (actualType.isAssignableFrom(resultType))
				return o;

			if (resultType == boolean.class || resultType == Boolean.class) {
				if (actualType == boolean.class || actualType == Boolean.class)
					return o;

				if (Number.class.isAssignableFrom(actualType)) {
					double b = ((Number) o).doubleValue();
					if (b == 0)
						return false;
					return true;
				}
				if (o instanceof String) {
					return Boolean.parseBoolean((String) o);
				}
				return true;

			} else if (resultType == byte.class || resultType == Byte.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).byteValue();
				resultType = Byte.class;
			} else if (resultType == char.class) {
				resultType = Character.class;
			} else if (resultType == short.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).shortValue();
				resultType = Short.class;
			} else if (resultType == int.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).intValue();
				resultType = Integer.class;
			} else if (resultType == long.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).longValue();
				resultType = Long.class;
			} else if (resultType == float.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).floatValue();
				resultType = Float.class;
			} else if (resultType == double.class) {
				if (Number.class.isAssignableFrom(actualType))
					return ((Number) o).doubleValue();
				resultType = Double.class;
			}

			if (resultType.isPrimitive())
				throw new IllegalArgumentException("Unknown primitive: " + resultType);

			if (Number.class.isAssignableFrom(resultType) && actualType == Boolean.class) {
				Boolean b = (Boolean) o;
				o = b ? "1" : "0";
			} else if (actualType == String.class) {
				String input = (String) o;
				if (Enum.class.isAssignableFrom(resultType)) {
					return Enum.valueOf((Class<Enum>) resultType, input);
				}
				if (resultType == Class.class && loader != null) {
					return loader.loadClass(input);
				}
				if (resultType == Pattern.class) {
					return Pattern.compile(input);
				}
			} else if (resultType.isAnnotation() && actualType.getName()
				.equals(BND_ANNOTATION_CLASS_NAME)) {
				MethodHandle mh = publicLookup().findVirtual(actualType, BND_ANNOTATION_METHOD_NAME,
					methodType(Annotation.class));
				Annotation a;
				try {
					a = (Annotation) mh.invoke(o);
				} catch (Error | Exception e) {
					throw e;
				} catch (Throwable e) {
					throw new InvocationTargetException(e);
				}
				if (resultType.isAssignableFrom(a.getClass())) {
					return a;
				}
				throw new IllegalArgumentException("Annotation " + o + " is not of expected type " + resultType);
			}

			try {
				return newInstance(resultType, o.toString());
			} catch (Throwable t) {
				// handled on next line
			}
			throw new IllegalArgumentException(
				"No conversion to " + resultType + " from " + actualType + " value " + o);
		}

		private Object convert(ParameterizedType pType, Object o)
			throws InstantiationException, IllegalAccessException, Exception {
			Class<?> resultType = (Class<?>) pType.getRawType();
			if (Collection.class.isAssignableFrom(resultType)) {
				Collection<?> input = toCollection(o);
				if (resultType.isInterface()) {
					if (resultType == Collection.class || resultType == List.class)
						resultType = ArrayList.class;
					else if (resultType == Set.class || resultType == SortedSet.class)
						resultType = TreeSet.class;
					else if (resultType == Queue.class /*
														 * || resultType ==
														 * Deque.class
														 */)
						resultType = LinkedList.class;
					else if (resultType == Queue.class /*
														 * || resultType ==
														 * Deque.class
														 */)
						resultType = LinkedList.class;
					else
						throw new IllegalArgumentException(
							"Unknown interface for a collection, no concrete class found: " + resultType);
				}

				Collection<Object> result = (Collection<Object>) newInstance(resultType);
				Type componentType = pType.getActualTypeArguments()[0];

				for (Object i : input) {
					result.add(convert(componentType, i));
				}
				return result;
			} else if (pType.getRawType() == Class.class) {
				return loader.loadClass(o.toString());
			}
			if (Map.class.isAssignableFrom(resultType)) {
				Map<?, ?> input = toMap(o);
				if (resultType.isInterface()) {
					if (resultType == SortedMap.class)
						resultType = TreeMap.class;
					else if (resultType == Map.class)
						resultType = LinkedHashMap.class;
					else
						throw new IllegalArgumentException(
							"Unknown interface for a collection, no concrete class found: " + resultType);
				}
				Map<Object, Object> result = (Map<Object, Object>) resultType.getConstructor()
					.newInstance();
				Type keyType = pType.getActualTypeArguments()[0];
				Type valueType = pType.getActualTypeArguments()[1];

				for (Map.Entry<?, ?> entry : input.entrySet()) {
					result.put(convert(keyType, entry.getKey()), convert(valueType, entry.getValue()));
				}
				return result;
			}
			throw new IllegalArgumentException(
				"cannot convert to " + pType + " because it uses generics and is not a Collection or a map");
		}

		Object convertArray(Type componentType, Object o) throws Exception {
			if (o instanceof String) {
				String s = (String) o;
				if (componentType == Byte.class || componentType == byte.class)
					return s.getBytes(UTF_8);
				if (componentType == Character.class || componentType == char.class)
					return s.toCharArray();
			}
			Collection<?> input = toCollection(o);
			Class<?> componentClass = getRawClass(componentType);
			Object array = Array.newInstance(componentClass, input.size());

			int i = 0;
			for (Object next : input) {
				Array.set(array, i++, convert(componentType, next));
			}
			return array;
		}

		private Class<?> getRawClass(Type type) {
			if (type instanceof Class)
				return (Class<?>) type;

			if (type instanceof ParameterizedType)
				return (Class<?>) ((ParameterizedType) type).getRawType();

			throw new IllegalArgumentException(
				"For the raw type, type must be ParamaterizedType or Class but is " + type);
		}

		private Collection<?> toCollection(Object o) {
			if (o instanceof Collection)
				return (Collection<?>) o;

			if (o.getClass()
				.isArray()) {
				if (o.getClass()
					.getComponentType()
					.isPrimitive()) {
					int length = Array.getLength(o);
					List<Object> result = new ArrayList<>(length);
					for (int i = 0; i < length; i++) {
						result.add(Array.get(o, i));
					}
					return result;
				}
				return Arrays.asList((Object[]) o);
			}

			if (o instanceof String) {
				String s = (String) o;
				if (SPLITTER_P.matcher(s)
					.find())
					return Arrays.asList(s.split("\\|"));
				else
					return unescape(s);

			}
			return Arrays.asList(o);
		}

		private Map<?, ?> toMap(Object o) {
			if (o instanceof Map)
				return (Map<?, ?>) o;

			throw new IllegalArgumentException("Cannot convert " + o + " to a map as requested");
		}

	}

	public static String mangleMethodName(String id) {
		StringBuilder sb = new StringBuilder(id);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			boolean twice = i < sb.length() - 1 && sb.charAt(i + 1) == c;
			if (c == '$' || c == '_') {
				if (twice)
					sb.deleteCharAt(i + 1);
				else if (c == '$')
					sb.deleteCharAt(i--); // Remove dollars
				else
					sb.setCharAt(i, '.'); // Make _ into .
			}
		}
		return sb.toString();
	}

	public static List<String> unescape(String s) {
		// do it the OSGi way
		List<String> tokens = new ArrayList<>();

		String[] parts = s.split("(?<!\\\\),");

		for (String p : parts) {
			p = p.replaceAll("^\\s*", "");
			p = p.replaceAll("(?!<\\\\)\\s*$", "");
			p = p.replaceAll("\\\\([\\s,\\\\|])", "$1");
			tokens.add(p);
		}
		return tokens;
	}

	private static final MethodType defaultConstructor = methodType(void.class);

	static <T> T newInstance(Class<T> rawClass) throws Exception {
		try {
			return (T) publicLookup().findConstructor(rawClass, defaultConstructor)
				.invoke();
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static final MethodType stringConstructor = methodType(void.class, String.class);

	private static <T> T newInstance(Class<T> rawClass, String arg) throws Exception {
		try {
			return (T) publicLookup().findConstructor(rawClass, stringConstructor)
				.invoke(arg);
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
