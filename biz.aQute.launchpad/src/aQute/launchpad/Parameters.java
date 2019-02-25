package aQute.launchpad;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import aQute.libg.qtokens.QuotedTokenizer;

public class Parameters extends LinkedHashMap<String, Map<String, String>> {
	private static final long serialVersionUID = 1L;

	public Parameters(String headers) {
		if (headers == null)
			return;
		parse(headers);
	}

	enum Mode {

	}

	private void parse(String headers) {
		QuotedTokenizer qt = new QuotedTokenizer(headers, ";=,", false);
		Set<String> keys = new HashSet<>();

		do {
			keys.clear();
			do {
				String token = qt.nextToken(";=,");

				switch (qt.getSeparator()) {
					case ';' :
						keys.add(token.trim());
						break;
					case ',' :
						keys.forEach(k -> put(k, null));
						break;

					case '=' :
						Map<String, String> attrs = new LinkedHashMap<>();
						do {
							String key = token.trim();
							if (qt.getSeparator() == '=') {
								String value = qt.nextToken(";,");
								attrs.put(key, value.trim());
							}
							if (qt.getSeparator() == ';')
								token = qt.nextToken(";=,");
						} while (qt.getSeparator() == '=');
						keys.forEach(k -> put(k, attrs));
				}
			} while (qt.getSeparator() == ';');
		} while (qt.getSeparator() == ',');

		String token = qt.nextToken();
		if (token != null && !token.trim()
			.isEmpty()) {
			throw new IllegalArgumentException("Parsing header: '" + headers + "' has trailing " + qt);
		}
	}

	public Map<String, String> put(String key, Map<String, String> attrs) {
		while (this.containsKey(key)) {
			key += "~";
		}
		if (attrs == null)
			attrs = new LinkedHashMap<String, String>();
		super.put(key, attrs);
		return null;
	}
}
