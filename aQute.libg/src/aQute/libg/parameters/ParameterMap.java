package aQute.libg.parameters;

import java.util.LinkedHashMap;
import java.util.Map;

import aQute.libg.qtokens.QuotedTokenizer;

public class ParameterMap extends LinkedHashMap<String, Attributes> {
	private static final char	DUPLICATE_MARKER	= '~';
	private static final long serialVersionUID = 1L;

	public ParameterMap(String headers) {
		if (headers == null)
			return;
		parse(headers);
	}

	public ParameterMap(Map<String, Map<String, String>> maps) {
		maps.forEach((k, v) -> put(k, new Attributes(v)));
	}

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
	private void parse(String parameters) {
		QuotedTokenizer qt = new QuotedTokenizer(parameters, ";=,", false);

		do { // parameters ::= clause ( ',' clause ) *
			clause(qt);
		} while (qt.getSeparator() == ',');

		String token = qt.nextToken();
		error(token != null && !token.isEmpty(), qt, "Parsing header: '" + parameters + "' has trailing " + qt);
	}

	/*
	 * clause = key ( ';' keyOrAttribute )* ( '=' attributeValue ) attributes *
	 */
	private void clause(QuotedTokenizer qt) {
		Attributes attrs = new Attributes();

		// key

		String key = qt.nextToken(";,");
		if (key == null)
			return;

		put(uniqueKey(key), attrs);

		while (qt.getSeparator() == ';') {

			String keyOrAttribute = qt.nextToken(";=,");
			error(keyOrAttribute == null, qt, "expected a clause key or attribute key");

			switch (qt.getSeparator()) {
				case '=' :
					String attributeValue = qt.nextToken(";,");
					error(attributeValue == null, qt, "expected an attribute value");

					attrs.put(keyOrAttribute, attributeValue);

					attributes(qt, attrs);
					break;

				case ';' :
				case ',' :
					put(uniqueKey(keyOrAttribute), attrs);
					break;

				default :
					error(true, qt, "unrecognized separator ");
			}
		}
	}

	// attributes = attribute *
	private void attributes(QuotedTokenizer qt, Map<String, String> attrs) {
		while (qt.getSeparator() == ';') {
			attribute(qt, attrs);
		}
	}

	private void attribute(QuotedTokenizer qt, Map<String, String> attrs) {
		String attributeKey = qt.nextToken("=,;");
		error(attributeKey == null, qt, "expected an attribute key");
		error(qt.getSeparator() != '=', qt, "expected an equal sign after an attribute key");
		String attributeValue = qt.nextToken(";,");
		error(attributeValue == null, qt, "expected an attribute value");
		attrs.put(attributeKey, attributeValue);
	}

	private void error(boolean b, QuotedTokenizer qt, String string) {
		if (b)
			throw new IllegalArgumentException(string + " : " + qt);
	}

	public Map<String, String> put(String key, Map<String, String> attrs) {
		key = uniqueKey(key);
		if (attrs == null)
			attrs = new LinkedHashMap<String, String>();
		return super.put(key, new Attributes(attrs));
	}

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
			del = ", ";
		}
	}

	public static boolean isDuplicate(String name) {
		return name.length() > 0 && name.charAt(name.length() - 1) == DUPLICATE_MARKER;
	}

	private String uniqueKey(String key) {
		while (this.containsKey(key)) {
			key += DUPLICATE_MARKER;
		}
		return key;
	}

	public String removeDuplicateMarker(String key) {
		while (isDuplicate(key))
			key = key.substring(0, key.length() - 1);
		return key;
	}

}
