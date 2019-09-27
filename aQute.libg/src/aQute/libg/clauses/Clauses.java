package aQute.libg.clauses;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aQute.libg.log.Logger;
import aQute.libg.qtokens.QuotedTokenizer;

public class Clauses extends LinkedHashMap<String, Map<String, String>> {
	private static final long serialVersionUID = 1L;

	/**
	 * Standard OSGi header parser. This parser can handle the format clauses
	 * ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '='
	 * value ) This is mapped to a Map { name => Map { attr|directive => value }
	 * }
	 *
	 * @param value
	 * @return parsed clauses
	 */
	static public Clauses parse(String value, Logger logger) {
		if (value == null || value.trim()
			.length() == 0)
			return new Clauses();

		Clauses result = new Clauses();
		QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
		char del;
		do {
			boolean hadAttribute = false;
			Clause clause = new Clause();
			List<String> aliases = new ArrayList<>();
			aliases.add(qt.nextToken());
			del = qt.getSeparator();
			while (del == ';') {
				String adname = qt.nextToken();
				if ((del = qt.getSeparator()) != '=') {
					if (hadAttribute)
						throw new IllegalArgumentException(
							"Header contains name field after attribute or directive: " + adname + " from " + value);
					aliases.add(adname);
				} else {
					String advalue = qt.nextToken();
					clause.put(adname, advalue);
					del = qt.getSeparator();
					hadAttribute = true;
				}
			}
			for (Iterator<String> i = aliases.iterator(); i.hasNext();) {
				String packageName = i.next();
				if (result.containsKey(packageName)) {
					if (logger != null)
						logger.warning("Duplicate package name in header: " + packageName
							+ ". Multiple package names in one clause not supported in Bnd.");
				} else
					result.put(packageName, clause);
			}
		} while (del == ',');
		return result;
	}

}
