package aQute.bnd.header;

import static aQute.bnd.osgi.Constants.DUPLICATE_MARKER;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import aQute.lib.regex.PatternConstants;
import aQute.libg.generics.Create;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.service.reporter.Reporter;

public class OSGiHeader {
	public final static Pattern TOKEN_P = Pattern.compile(PatternConstants.TOKEN);

	static public Parameters parseHeader(String value) {
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
	static public Parameters parseHeader(String value, Reporter logger) {
		return parseHeader(value, logger, new Parameters());
	}

	static public Parameters parseHeader(String value, Reporter logger, Parameters result) {
		if (value == null || value.trim()
			.length() == 0)
			return result;

		Map<String, String> duplicates = new HashMap<>();
		QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
		char del = 0;
		do {
			boolean hadAttribute = false;
			Attrs clause = new Attrs();
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
							if (result.allowDuplicateAttributes()) {
								while (clause.containsKey(adname)) {
									adname += DUPLICATE_MARKER;
								}
							} else {
								if (logger != null && logger.isPedantic())
									logger.warning(
										"Duplicate attribute/directive name %s in %s. This attribute/directive will be ignored",
										adname, value);
							}
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
				// the list of names, for each check if it exists. If so,
				// add a number of "~" to make it unique.
				for (String clauseName : aliases) {
					String key = duplicates.compute(clauseName, (k, v) -> {
						v = (v == null) ? k : v + DUPLICATE_MARKER;
						while (result.containsKey(v)) {
							v += DUPLICATE_MARKER;
						}
						return v;
					});
					if ((logger != null) && logger.isPedantic() && !result.allowDuplicateAttributes()
						&& (key.indexOf(DUPLICATE_MARKER, key.length() - 1) >= 0)) {
						logger.warning(
							"Duplicate name %s used in header: '%s'. Duplicate names are specially marked in Bnd with a "
								+ DUPLICATE_MARKER + " at the end (which is stripped at printing time).",
							clauseName, value);
					}
					result.put(key, clause);
				}
			}
		} while (del == ',');
		return result;
	}

	public static Attrs parseProperties(String input) {
		return parseProperties(input, null);
	}

	public static Attrs parseProperties(String input, Reporter logger) {
		if (input == null || input.trim()
			.length() == 0)
			return new Attrs();

		Attrs result = new Attrs();
		QuotedTokenizer qt = new QuotedTokenizer(input, "=,");
		char del = ',';

		while (del == ',') {
			String key = qt.nextToken(",=");
			if (key == null) {
				// happens at a trailing ',' without a followup
				if (logger == null)
					throw new IllegalArgumentException(
						"Trailing comma found, forgot to escape the newline? Input=" + input);
				logger.error("Trailing comma found, forgot to escape the newline? Input=", input);
				break;
			}
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

	/**
	 * @param sb
	 * @param value
	 */
	public static boolean quote(StringBuilder sb, String value) {
		try {
			return quote((Appendable) sb, value);
		} catch (IOException e) {
			// this wont happen
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param sb
	 * @param value
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
