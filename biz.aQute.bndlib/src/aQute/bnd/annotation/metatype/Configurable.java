package aQute.bnd.annotation.metatype;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

public class Configurable<T> {

	
	
	
	public static <T> T createConfigurable(Class<T> c, Map<?, ?> properties) {
		Object o = Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c },
				new ConfigurableHandler(properties, c.getClassLoader()));
		return c.cast(o);
	}

	static class ConfigurableHandler implements InvocationHandler {
		final Map<?, ?>	properties;
		final ClassLoader			loader;

		ConfigurableHandler(Map<?, ?> properties, ClassLoader loader) {
			this.properties = properties;
			this.loader = loader;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Metadata.AD ad = method.getAnnotation(Metadata.AD.class);
			String id = method.getName();

			if (ad != null) {
				if (id == Metadata.NULL)
					id = method.getName();
			}
			Object o = properties.get(id);
			if (o == null)
				o = properties.get(id.replace('_', '.'));

			if (o == null && id.startsWith("_"))
				o = properties.get(id.substring(1));

			if (o == null) {
				if (ad != null) {
					if (ad.required())
						throw new IllegalStateException("Attribute is required but not set "
								+ method.getName());

					o = ad.deflt();
					if (o.equals(Metadata.NULL))
						o = null;
				}
			}
			if (o == null) {
				if (method.getReturnType().isPrimitive()
						|| Number.class.isAssignableFrom(method.getReturnType())) {

					o = "0";
				} else
					return null;
			}

			return convert(method.getGenericReturnType(), o);
		}

		@SuppressWarnings( { "unchecked" }) public Object convert(Type type, Object o)
				throws Exception {
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
				if (Number.class.isAssignableFrom(actualType)) {
					double b = ((Number) o).doubleValue();
					if (b == 0)
						return false;
					else
						return true;
				}
				resultType = Boolean.class;
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
			}

			try {
				Constructor<?> c = resultType.getConstructor(String.class);
				return c.newInstance(o.toString());
			} catch (Throwable t) {
				// handled on next line
			}
			throw new IllegalArgumentException("No conversion to " + resultType + " from "
					+ actualType + " value " + o);
		}

		private Object convert(ParameterizedType pType, Object o) throws InstantiationException,
				IllegalAccessException, Exception {
			Class<?> resultType = (Class<?>) pType.getRawType();
			if (Collection.class.isAssignableFrom(resultType)) {
				Collection<?> input = toCollection(o);
				if (resultType.isInterface()) {
					if (resultType == Collection.class || resultType == List.class)
						resultType = ArrayList.class;
					else if (resultType == Set.class || resultType == SortedSet.class)
						resultType = TreeSet.class;
					else if (resultType == Queue.class || resultType == Deque.class)
						resultType = LinkedList.class;
					else if (resultType == Queue.class || resultType == Deque.class)
						resultType = LinkedList.class;
					else
						throw new IllegalArgumentException(
								"Unknown interface for a collection, no concrete class found: "
										+ resultType);
				}
				@SuppressWarnings("unchecked") Collection<Object> result = (Collection<Object>) resultType
						.newInstance();
				Type componentType = pType.getActualTypeArguments()[0];

				for (Object i : input) {
					result.add(convert(componentType, i));
				}
				return result;
			}
			throw new IllegalArgumentException("cannot convert to " + pType
					+ " because it uses generics and is not a Collection");
		}

		Object convertArray(Type componentType, Object o) throws Exception {
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

			if (o.getClass().isArray())
				return Arrays.asList((Object[]) o);

			if ( o instanceof String) {
				String s = (String)o;
				if (s.indexOf('|')>0)
					return Arrays.asList(s.split("\\|"));					
			}
			return Arrays.asList(o);
		}

	}
}
