package aQute.bnd.component;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.collections.MultiMap;
import aQute.lib.tag.Tag;

public class PropertyDef {
	final static String						MARKER				= new String("|marker");
	private final static Pattern			PROPERTY_PATTERN	= Pattern.compile(
		"\\s*(?<key>[^=\\s:]+)\\s*(?::\\s*(?<type>Boolean|Byte|Character|Short|Integer|Long|Float|Double|String)\\s*)?=(?<value>.*)");

	private final Analyzer					analyzer;
	private final MultiMap<String, String>	property			= new MultiMap<>();
	private final Map<String, String>		propertyType		= new HashMap<>();

	PropertyDef(Analyzer analyzer) {
		this.analyzer = requireNonNull(analyzer);
	}

	boolean isEmpty() {
		return property.isEmpty();
	}

	boolean containsKey(String key) {
		return property.containsKey(key);
	}

	PropertyDef setProperty(String key, String type, List<String> values) {
		requireNonNull(key);
		requireNonNull(type);
		if (!values.isEmpty()) {
			property.put(key, values);
			propertyType.put(key, type);
		}
		return this;
	}

	PropertyDef setProperty(String key, String type, String... values) {
		requireNonNull(key);
		requireNonNull(type);
		if (notEmpty(values)) {
			List<String> list = new ArrayList<>(values.length);
			Collections.addAll(list, values);
			property.put(key, list);
			propertyType.put(key, type);
		}
		return this;
	}

	PropertyDef addProperty(String key, String type, String... values) {
		requireNonNull(key);
		requireNonNull(type);
		if (notEmpty(values)) {
			property.addAll(key, Arrays.asList(values));
			propertyType.put(key, type);
		}
		return this;
	}

	PropertyDef setTypedProperty(TypeRef className, String... props) {
		if (notEmpty(props)) {
			MultiMap<String, String> map = new MultiMap<>();
			for (String p : props) {
				Matcher m = PROPERTY_PATTERN.matcher(p);
				if (m.matches()) {
					String key = m.group("key");
					String type = m.group("type");
					if (type == null) {
						type = "String";
					}
					propertyType.put(key, type);
					String value = m.group("value");
					map.add(key, value);
				} else {
					analyzer.error("Malformed property '%s' on component: %s", p, className);
				}
			}
			property.putAll(map);
		}
		return this;
	}

	private static boolean notEmpty(Object[] array) {
		return (array != null) && (array.length > 0);
	}

	PropertyDef addAll(PropertyDef propertyDef) {
		property.putAll(propertyDef.property);
		propertyType.putAll(propertyDef.propertyType);
		return this;
	}

	PropertyDef addAll(Collection<PropertyDef> propertyDefs) {
		propertyDefs.forEach(this::addAll);
		return this;
	}

	PropertyDef copy(Function<String, String> keyMapper) {
		PropertyDef copy = new PropertyDef(analyzer);
		property.keySet()
			.forEach(key -> copy.setProperty(keyMapper.apply(key), propertyType.get(key), property.get(key)));
		return copy;
	}

	Stream<Tag> propertyTags(String element) {
		return property.keySet()
			.stream()
			.map(name -> {
				Tag tag = new Tag(element).addAttribute("name", name);
				String type = propertyType.get(name);
				tag.addAttribute("type", type);
				List<String> values = property.get(name);
				switch (values.size()) {
					case 0 :
						return null;
					case 1 :
						tag.addAttribute("value", check(type, values.get(0)));
						break;
					default :
						tag.addContent(values.stream()
							.filter(v -> v != MARKER)
							.map(v -> check(type, v))
							.collect(joining("\n")));
						break;
				}
				return tag;
			})
			.filter(Objects::nonNull);
	}

	private String check(String type, String v) {
		// Boolean|Byte|Character|Short|Integer|Long|Float|Double|String
		if ((type == null) || type.equals("String")) {
			return v;
		}
		v = v.trim();
		try {
			switch (type) {
				case "Boolean" :
					Boolean.valueOf(v);
					break;
				case "Byte" :
					Byte.valueOf(v);
					break;
				case "Char" :
				case "Character" :
					// DS spec requires Unicode value for char
					v = Integer.toString(v.charAt(0));
					break;
				case "Short" :
					Short.valueOf(v);
					break;
				case "Integer" :
					Integer.valueOf(v);
					break;
				case "Long" :
					Long.valueOf(v);
					break;
				case "Float" :
					Float.valueOf(v);
					break;
				case "Double" :
					Double.valueOf(v);
					break;
				default :
					analyzer.error("Invalid data type %s", type);
					break;
			}
		} catch (NumberFormatException e) {
			analyzer.error("Not a valid number %s for %s, %s", v, type, e.getMessage());
		} catch (Throwable e) {
			analyzer.error("Cannot convert data %s to type %s", v, type);
		}
		return v;
	}

	@Override
	public String toString() {
		Iterator<Entry<String, List<String>>> i = property.entrySet()
			.iterator();
		if (!i.hasNext()) {
			return "{}";
		}
		StringBuilder b = new StringBuilder().append('{');
		while (true) {
			Entry<String, List<String>> e = i.next();
			String key = e.getKey();
			b.append(key);
			String type = propertyType.get(key);
			if (!type.equals("String")) {
				b.append(':')
					.append(type);
			}
			b.append('=')
				.append(e.getValue());
			if (!i.hasNext()) {
				return b.append('}')
					.toString();
			}
			b.append(',')
				.append(' ');
		}
	}
}
