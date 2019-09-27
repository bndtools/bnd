package aQute.lib.env;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.lib.collections.SortedList;
import aQute.lib.regex.PatternConstants;
import aQute.libg.generics.Create;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.service.reporter.Reporter;

public class Header implements Map<String, Props> {
	public final static Pattern				TOKEN_P				= Pattern.compile(PatternConstants.TOKEN);

	public static final char				DUPLICATE_MARKER	= '~';

	private LinkedHashMap<String, Props>	map;
	static Map<String, Props>				EMPTY				= Collections.emptyMap();
	String									error;

	public Header() {}

	public Header(String header) {
		Header.parseHeader(header, null, this);
	}

	public Header(String header, Reporter reporter) {
		Header.parseHeader(header, reporter, this);
	}

	@Override
	public void clear() {
		map.clear();
	}

	public void add(String key, Props attrs) {
		while (containsKey(key))
			key += "~";
		put(key, attrs);
	}

	public boolean containsKey(final String name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof String;
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	public boolean containsValue(Props value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Props;
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Props>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public Props get(Object key) {
		assert key instanceof String;
		if (map == null)
			return null;

		return map.get(key);
	}

	public Props get(String key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		if (map == null)
			return EMPTY.keySet();

		return map.keySet();
	}

	@Override
	public Props put(String key, Props value) {
		assert key != null;
		assert value != null;

		if (map == null)
			map = new LinkedHashMap<>();

		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Props> map) {
		if (this.map == null) {
			if (map.isEmpty())
				return;
			this.map = new LinkedHashMap<>();
		}
		this.map.putAll(map);
	}

	public void putAllIfAbsent(Map<String, ? extends Props> map) {
		for (Map.Entry<String, ? extends Props> entry : map.entrySet()) {
			if (!containsKey(entry.getKey()))
				put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	@SuppressWarnings("cast")
	@Deprecated
	public Props remove(Object var0) {
		assert var0 instanceof String;
		if (map == null)
			return null;

		return map.remove(var0);
	}

	public Props remove(String var0) {
		if (map == null)
			return null;
		return map.remove(var0);
	}

	@Override
	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	@Override
	public Collection<Props> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		String del = "";
		for (Map.Entry<String, Props> s : entrySet()) {
			sb.append(del);
			sb.append(Header.removeDuplicateMarker(s.getKey()));
			if (!s.getValue()
				.isEmpty()) {
				sb.append(';');
				s.getValue()
					.append(sb);
			}

			del = ",";
		}
	}

	@Override
	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isEqual(Header other) {
		if (this == other)
			return true;

		if (other == null || size() != other.size())
			return false;

		if (isEmpty())
			return true;

		SortedList<String> l = new SortedList<>(keySet());
		SortedList<String> lo = new SortedList<>(other.keySet());
		if (!l.isEqual(lo))
			return false;

		for (String key : keySet()) {
			Props value = get(key);
			Props valueo = other.get(key);
			if (!(value == valueo || (value != null && value.isEqual(valueo))))
				return false;
		}
		return true;
	}

	public Map<String, ? extends Map<String, String>> asMapMap() {
		return this;
	}

	static public Header parseHeader(String value) {
		return parseHeader(value, null);
	}

	/**
	 * Standard OSGi header parser. This parser can handle the format clauses
	 * ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '='
	 * value ) This is mapped to a Map { name => Map { attr|directive => value }
	 * }
	 *
	 * @param value A string
	 * @return a Map<String,Map<String,String>>
	 */
	static public Header parseHeader(String value, Reporter logger) {
		return parseHeader(value, logger, new Header());
	}

	static public Header parseHeader(String value, Reporter logger, Header result) {
		if (value == null || value.trim()
			.length() == 0)
			return result;

		QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
		char del = 0;
		do {
			boolean hadAttribute = false;
			Props clause = new Props();
			List<String> aliases = Create.list();
			String name = qt.nextToken(",;");

			del = qt.getSeparator();
			if (name == null || name.length() == 0) {
				if (logger != null && logger.isPedantic()) {
					logger.warning(
						"Empty clause, usually caused by repeating a comma without any name field or by having spaces after the backslash of a property file: %s",
						value);
				}
				if (name == null)
					break;
			} else {
				name = name.trim();

				aliases.add(name);
				while (del == ';') {
					String adname = qt.nextToken();
					if ((del = qt.getSeparator()) != '=') {
						if (hadAttribute)
							if (logger != null) {
								logger.error(
									"Header contains name field after attribute or directive: %s from %s. Name fields must be consecutive, separated by a ';' like a;b;c;x=3;y=4",
									adname, value);
							}
						if (adname != null && adname.length() > 0)
							aliases.add(adname.trim());
					} else {
						String advalue = qt.nextToken();
						if (clause.containsKey(adname)) {
							if (logger != null && logger.isPedantic())
								logger.warning(
									"Duplicate attribute/directive name %s in %s. This attribute/directive will be ignored",
									adname, value);
						}
						if (advalue == null) {
							if (logger != null)
								logger.error("No value after '=' sign for attribute %s", adname);
							advalue = "";
						}
						clause.put(adname.trim(), advalue);
						del = qt.getSeparator();
						hadAttribute = true;
					}
				}

				// Check for duplicate names. The aliases list contains
				// the list of nams, for each check if it exists. If so,
				// add a number of "~" to make it unique.
				for (String clauseName : aliases) {
					if (result.containsKey(clauseName)) {
						if (logger != null && logger.isPedantic())
							logger.warning(
								"Duplicate name %s used in header: '%s'. Duplicate names are specially marked in Bnd with a ~ at the end (which is stripped at printing time).",
								clauseName, clauseName);
						while (result.containsKey(clauseName))
							clauseName += "~";
					}
					result.put(clauseName, clause);
				}
			}
		} while (del == ',');
		return result;
	}

	public static Props parseProperties(String input) {
		return parseProperties(input, null);
	}

	public static Props parseProperties(String input, Reporter logger) {
		if (input == null || input.trim()
			.length() == 0)
			return new Props();

		Props result = new Props();
		QuotedTokenizer qt = new QuotedTokenizer(input, "=,");
		char del = ',';

		while (del == ',') {
			String key = qt.nextToken(",=");
			String value = "";
			del = qt.getSeparator();
			if (del == '=') {
				value = qt.nextToken(",=");
				if (value == null)
					value = "";
				del = qt.getSeparator();
			}
			result.put(key.trim(), value);
		}
		if (del != 0) {
			if (logger == null)
				throw new IllegalArgumentException("Invalid syntax for properties: " + input);
			logger.error("Invalid syntax for properties: %s", input);
		}

		return result;
	}

	public static String removeDuplicateMarker(String key) {
		int i = key.length() - 1;
		while (i >= 0 && key.charAt(i) == DUPLICATE_MARKER)
			--i;

		return key.substring(0, i + 1);
	}

	public static boolean isDuplicate(String name) {
		return name.length() > 0 && name.charAt(name.length() - 1) == DUPLICATE_MARKER;
	}

	/**
	 * @param sb
	 * @param value
	 * @return clean
	 * @throws IOException
	 */
	public static boolean quote(Appendable sb, String value) throws IOException {
		if (value.startsWith("\\\""))
			value = value.substring(2);
		if (value.endsWith("\\\""))
			value = value.substring(0, value.length() - 2);
		if (value.startsWith("\"") && value.endsWith("\""))
			value = value.substring(1, value.length() - 1);

		boolean clean = (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
			|| TOKEN_P.matcher(value)
				.matches();
		if (!clean)
			sb.append("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' :
					sb.append('\\')
						.append('"');
					break;

				default :
					sb.append(c);
			}
		}
		if (!clean)
			sb.append("\"");
		return clean;
	}
}
