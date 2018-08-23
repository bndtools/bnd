package aQute.bnd.component;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.collections.MultiMap;
import aQute.lib.tag.Tag;

public class PropertiesDef {
	final static String						MARKER				= new String("|marker");
	private final static Pattern			PROPERTY_PATTERN	= Pattern.compile(
		"\\s*([^=\\s:]+)\\s*(?::\\s*(Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(.*)");

	private final Analyzer					analyzer;
	private final MultiMap<String, String>	property			= new MultiMap<>();
	private final Map<String, String>		propertyType		= new HashMap<>();
	private final List<String>				properties			= new ArrayList<>();

	PropertiesDef(Analyzer analyzer) {
		this.analyzer = requireNonNull(analyzer);
	}

	boolean isEmpty() {
		return property.isEmpty() && properties.isEmpty();
	}

	void setProperty(String key, String type, List<String> values) {
		property.put(key, values);
		propertyType.put(key, type);
	}

	void addProperty(String key, String type, String... values) {
		property.addAll(key, Arrays.asList(values));
		propertyType.put(key, type);
	}

	void setTypedProperty(TypeRef className, String... props) {
		if (props == null || props.length == 0) {
			return;
		}
		MultiMap<String, String> map = new MultiMap<>();
		for (String p : props) {
			Matcher m = PROPERTY_PATTERN.matcher(p);
			if (m.matches()) {
				String key = m.group(1);
				String type = m.group(2);
				if (type == null) {
					type = "String";
				}
				propertyType.put(key, type);
				String value = m.group(3);
				map.add(key, value);
			} else {
				analyzer.error("Malformed property '%s' on component: %s", p, className);
			}
		}
		property.putAll(map);
	}

	Stream<Tag> propertyTags(String element) {
		return property.keySet()
			.stream()
			.map(name -> {
				Tag tag = new Tag(element).addAttribute("name", name);
				String type = propertyType.get(name);
				if (type != null) {
					tag.addAttribute("type", type);
				}
				List<String> values = property.get(name);
				if (values.size() == 1) {
					tag.addAttribute("value", check(type, values.get(0)));
				} else {
					tag.addContent(values.stream()
						.filter(v -> v != MARKER)
						.map(v -> check(type, v))
						.collect(joining("\n")));
				}
				return tag;
			});
	}

	void addProperties(String... props) {
		if (props == null) {
			return;
		}
		Collections.addAll(properties, props);
	}

	Stream<Tag> propertiesTags(String element) {
		return properties.stream()
			.map(p -> new Tag(element).addAttribute("entry", p));
	}

	private String check(String type, String v) {
		if (type == null) {
			return v;
		}
		try {
			if (type.equals("Char")) {
				type = "Character";
			}
			Class<?> c = Class.forName("java.lang." + type);
			if (c == String.class) {
				return v;
			}
			v = v.trim();
			if (c == Character.class) {
				c = Integer.class;
			}
			Method m = c.getMethod("valueOf", String.class);
			try {
				m.invoke(null, v);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (ClassNotFoundException e) {
			analyzer.error("Invalid data type %s", type);
		} catch (NoSuchMethodException e) {
			analyzer.error("Cannot convert data %s to type %s", v, type);
		} catch (NumberFormatException e) {
			analyzer.error("Not a valid number %s for %s, %s", v, type, e.getMessage());
		} catch (Throwable e) {
			analyzer.error("Cannot convert data %s to type %s", v, type);
		}
		return v;
	}
}
