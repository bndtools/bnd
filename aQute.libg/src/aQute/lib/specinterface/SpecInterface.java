package aQute.lib.specinterface;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import aQute.lib.converter.Converter;

/**
 * Uses an interface to provide a _specification_ of a command line. Methods
 * with no or 1 parameter (with the same parameter type as their return type)
 * are _parameter_ methods. If the return type is boolean, they are _option_
 * methods. If a method with the same name appears in the 0 and 1 parameter
 * form, the return type must be identical.
 * <p>
 * Each method name is available in the command line as --name, and -n, where n
 * is the first character of the name. If the same letter is used twice, the
 * names are sorted and the first appearing name gets the lower case, the second
 * gets the upper case, and the third and later will not have a single character
 * identifier.
 * <p>
 * Single character identifiers can be merged together. I.e. -xyz will be parsed
 * as -x -y -z. Except for the last one, the others must be options.
 * <p>
 * The command line may also contains {@code key=value} pairs. These are stored
 * in a map and are available as _properties() if declared on the interface.
 * <p>
 * There are a number of build-in values that, when needed, should be declared
 * on the interface.
 *
 * <pre>
 * _arguments() List&lt;String> the argument list with the parameters removed
 * _properties() Map&lt;String,String> any key=value pairs
 * </pre>
 *
 * @param <T>
 */
public class SpecInterface<T> {
	private final static Pattern			ASSIGNMENT	= Pattern.compile("(\\w++)\\s*=\\s*(\\S+)\\s*");
	private final T							instance;
	private final IllegalArgumentException	failure;

	static class O {
		public O(Method m) {
			this.name = m.getName();
		}

		String			name;
		List<String>	aliases	= new ArrayList<>();
		Type			type;
		Method			noargs;
		Method			withDefault;
		boolean			hasParameter;

		@Override
		public String toString() {
			return aliases.stream()
				.collect(Collectors.joining()) + (hasParameter ? type : "");
		}
	}

	public SpecInterface(T convert) {
		this.instance = convert;
		this.failure = null;
	}

	public SpecInterface(IllegalArgumentException exception) {
		this.instance = null;
		this.failure = exception;
	}

	public T instance() {
		if (instance == null)
			throw this.failure;
		return instance;
	}

	public String failure() {
		if (failure == null)
			return null;
		return failure.getMessage();
	}

	public boolean isFailure() {
		return failure != null;
	}

	/**
	 * Parse the options in a command line and return an interface that provides
	 * the options from this command line. This will parse up to (and including)
	 * -- or an argument that does not start with -
	 */
	public static <T> SpecInterface<T> getOptions(Class<T> specification, List<String> args, File base)
		throws Exception {
		try {
			List<String> arguments = new ArrayList<>(args);
			Map<String, String> properties = new LinkedHashMap<>();
			Map<String, Object> values = new HashMap<>();

			Map<String, O> options = parse(specification);

			int n = 0;
			while (n < arguments.size()) {

				String option = arguments.get(n);

				if (option.startsWith("--")) {

					arguments.remove(n);

					if (option.equals("--")) {
						break;
					}

					O o = options.get(option);
					if (o == null) {
						throw new IllegalArgumentException("No such option " + option);
					}

					if (o.hasParameter) {
						String value = arguments.remove(n);
						add(values, o, value);
					}
				} else if (option.startsWith("-")) {

					arguments.remove(n);

					for (int i = 1; i < option.length(); i++) {
						String opt = "-" + option.charAt(i);
						O o = options.get(opt);
						if (o == null) {
							throw new IllegalArgumentException("No such option " + opt);
						}
						if (o.hasParameter) {
							if (i != option.length() - 1) {
								throw new IllegalArgumentException(
									"Option " + opt + " has a value but it is not the last in a sequence " + option);
							}
							String value = arguments.remove(n);
							add(values, o, value);
						} else {
							values.put(o.name, true);
						}
					}
				} else {
					Matcher m = ASSIGNMENT.matcher(option);
					if (m.matches()) {
						arguments.remove(n);
						properties.put(m.group(1), m.group(2));
					} else {
						n++;
					}
				}
			}

			values.put("_arguments", arguments);
			values.put("_properties", properties);
			Converter c = new Converter();
			if (base != null)
				c.setBase(base);
			return new SpecInterface<T>(c.convert(specification, values));
		} catch (IllegalArgumentException e) {
			return new SpecInterface<T>(e);
		}
	}

	/**
	 * A common pattern is to use the {@link SpecInterface} with a parameterized
	 * super class. This is a convenient method that extracts the first type
	 * parameter.
	 *
	 * @param baseType a parameterized type
	 * @return null or the first type parameter
	 */
	public static Class<?> getParameterizedSuperType(Class<?> baseType) {
		assert baseType != null;
		Type type = baseType.getGenericSuperclass();
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			Type p1 = ptype.getActualTypeArguments()[0];
			if (p1 instanceof Class) {
				return (Class<?>) p1;
			}
		}
		return null;
	}

	/**
	 * A common pattern is to use the {@link SpecInterface} with a parameterized
	 * interface. This is a convenient method that extracts the first type
	 * parameter.
	 *
	 * @param baseType a parameterized type
	 * @param interfce a interface type
	 * @return null or the first type parameter
	 */
	public static Class<?> getParameterizedInterfaceType(Class<?> baseType, Class<?> interfce) {

		assert baseType != null;
		assert interfce != null;
		assert interfce.isInterface();

		for (Type type : baseType.getGenericInterfaces()) {
			if (!(type instanceof ParameterizedType))
				continue;

			ParameterizedType ptype = (ParameterizedType) type;
			if (ptype.getRawType() != interfce)
				continue;

			Type p1 = ptype.getActualTypeArguments()[0];
			if (p1 instanceof Class) {
				return (Class<?>) p1;
			}
		}
		return null;
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	static void add(Map<String, Object> map, O o, String value) {
		Object v = map.get(o.name);
		if (v != null) {
			if (Converter.isMultiple(o.type)) {

				if (v instanceof List) {
					((List) v).add(value);
				} else {
					List l = new ArrayList<>();
					l.add(v);
					l.add(value);
					map.put(o.name, l);
				}
			} else {
				throw new IllegalArgumentException("multiple return values for " + o + " " + v + ", " + value);
			}
		} else
			map.put(o.name, value);
	}

	static Map<String, O> parse(Class<?> type) {
		Map<String, O> map = new LinkedHashMap<>();
		List<Method> methods = Arrays.asList(type.getMethods());
		Collections.sort(methods, (a, b) -> a.getName()
			.compareTo(b.getName()));

		for (Method m : methods) {

			if (m.getParameterCount() > 1)
				continue;

			O o = map.get(m.getName());
			if (o == null) {
				o = new O(m);
				map.put(m.getName(), o);
				char c = m.getName()
					.charAt(0);
				String alias = "-" + c;
				O minor = map.putIfAbsent(alias, o);
				if (minor != null) {
					alias = "-" + Character.toUpperCase(c);
					map.putIfAbsent(alias, o);
				} else {
					alias = null;
				}

				if (alias != null) {
					o.aliases.add(alias);
				}
				String longAlias = "--" + m.getName();
				map.put(longAlias, o);
				o.aliases.add(longAlias);
			}

			if (m.getParameterCount() == 0)
				o.noargs = m;
			else
				o.withDefault = m;

			if (o.type == null) {
				o.type = m.getGenericReturnType();
				o.hasParameter = o.type != Boolean.class && o.type != boolean.class;
			} else {
				assert o.type.equals(m.getGenericReturnType()) : "two parameter/option methods have different types "
					+ m.getName();
			}

		}
		return map;
	}

}
