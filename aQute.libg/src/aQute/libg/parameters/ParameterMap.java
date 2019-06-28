package aQute.libg.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.libg.glob.Glob;
import aQute.libg.qtokens.QuotedTokenizer;

public class ParameterMap extends LinkedHashMap<String, Attributes> {
	private static final char	DUPLICATE_MARKER	= '~';
	private static final long	serialVersionUID	= 1L;

	public ParameterMap() {}

	/**
	 * <pre>
	 * parameters ::= clause ( ',' clause ) *
	 * clause ::=  key ( ';' key )* ( '=' value ) ( ';' key '=' value )*
	 * key    ::= NAME ( ':' type )
	 * type   ::= List<String>, ...
	 * </pre>
	 *
	 * @param parameters
	 */
	public ParameterMap(String parameters) {
		this();
		if (parameters != null) {
			new Parser(parameters).parse();
		}
	}

	public ParameterMap(Map<String, Map<String, String>> maps) {
		this();
		maps.forEach((k, v) -> put(k, new Attributes(v)));
	}

	class Parser {
		private final String				parameters;
		private final QuotedTokenizer		qt;
		private final Map<String, String>	duplicates	= new HashMap<>();

		Parser(String parameters) {
			this.parameters = parameters;
			qt = new QuotedTokenizer(parameters, ";=,", false);
		}

		void parse() {
			do { // parameters ::= clause ( ',' clause ) *
				clause();
			} while (qt.getSeparator() == ',');

			String token = qt.nextToken();
			error(token != null && !token.isEmpty(), "Parsing header: '" + parameters + "' has trailing " + qt);
		}

		/*
		 * clause = key ( ';' keyOrAttribute )* ( '=' attributeValue )
		 * attributes *
		 */
		private void clause() {
			Attributes attrs = new Attributes();

			// key

			String key = qt.nextToken(";,");
			if (key == null)
				return;

			put(uniqueKey(key), attrs);

			while (qt.getSeparator() == ';') {

				String keyOrAttribute = qt.nextToken(";=,");
				error(keyOrAttribute == null, "expected a clause key or attribute key");

				switch (qt.getSeparator()) {
					case '=' :
						String attributeValue = qt.nextToken(";,");
						error(attributeValue == null, "expected an attribute value");

						attrs.put(keyOrAttribute, attributeValue);

						attributes(attrs);
						break;

					case ';' :
					case ',' :
						put(uniqueKey(keyOrAttribute), attrs);
						break;

					default :
						error(true, "unrecognized separator ");
				}
			}
		}

		private String uniqueKey(String key) {
			return duplicates.compute(key,
				(k, v) -> ParameterMap.this.uniqueKey((v == null) ? k : v + DUPLICATE_MARKER));
		}

		// attributes = attribute *
		private void attributes(Map<String, String> attrs) {
			while (qt.getSeparator() == ';') {
				attribute(attrs);
			}
		}

		private void attribute(Map<String, String> attrs) {
			String attributeKey = qt.nextToken("=,;");
			error(attributeKey == null, "expected an attribute key");
			error(qt.getSeparator() != '=', "expected an equal sign after an attribute key");
			String attributeValue = qt.nextToken(";,");
			error(attributeValue == null, "expected an attribute value");
			attrs.put(attributeKey, attributeValue);
		}

		private void error(boolean b, String string) {
			if (b)
				throw new IllegalArgumentException(string + " : " + qt);
		}
	}

	public Map<String, String> put(String key, Map<String, String> attrs) {
		key = uniqueKey(key);
		return super.put(key, (attrs == null) ? new Attributes() : new Attributes(attrs));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	private void append(StringBuilder appendable) {
		String del = "";
		for (Map.Entry<String, Attributes> e : entrySet()) {

			String key = e.getKey();
			key = removeDuplicateMarker(key);

			appendable.append(del);
			appendable.append(key);
			e.getValue()
				.append(appendable);
			del = ",";
		}
	}

	public static boolean isDuplicate(String key) {
		return key.indexOf(DUPLICATE_MARKER, key.length() - 1) >= 0;
	}

	String uniqueKey(String key) {
		while (containsKey(key)) {
			key += DUPLICATE_MARKER;
		}
		return key;
	}

	public static String removeDuplicateMarker(String key) {
		int i = key.length() - 1;
		while ((i >= 0) && (key.charAt(i) == DUPLICATE_MARKER)) {
			--i;
		}
		return key.substring(0, i + 1);
	}

	public ParameterMap restrict(Collection<String> matchers) {
		ParameterMap result = new ParameterMap();
		Set<Glob> negated = new HashSet<>();
		List<Glob> sequence = new ArrayList<>();

		for (String s : matchers) {
			Glob g;
			if (s.startsWith("!")) {
				g = new Glob(s.substring(1));
				negated.add(g);
			} else {
				g = new Glob(s);
			}
			sequence.add(g);
		}

		nextEntry: for (java.util.Map.Entry<String, Attributes> e : this.entrySet()) {
			for (Glob g : sequence) {
				if (g.matches(e.getKey())) {
					if (!negated.contains(g))
						result.put(e.getKey(), e.getValue());
					continue nextEntry;
				}
			}
		}

		return result;

	}
}
