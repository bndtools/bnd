package aQute.libg.clauses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Selector {
	Pattern	pattern;
	String	instruction;
	boolean	negated;
	Clause	clause;

	public Selector(String instruction, boolean negated) {
		this.instruction = instruction;
		this.negated = negated;
	}

	public boolean matches(String value) {
		if (pattern == null) {
			pattern = Pattern.compile(instruction);
		}
		Matcher m = pattern.matcher(value);
		return m.matches();
	}

	public boolean isNegated() {
		return negated;
	}

	public String getPattern() {
		return instruction;
	}

	/**
	 * Convert a string based pattern to a regular expression based pattern.
	 * This is called an instruction, this object makes it easier to handle the
	 * different cases
	 *
	 * @param string
	 * @return new selector
	 */
	public static Selector getPattern(String string) {
		boolean negated = false;
		if (string.startsWith("!")) {
			negated = true;
			string = string.substring(1);
		}
		StringBuilder sb = new StringBuilder();
		for (int c = 0; c < string.length(); c++) {
			switch (string.charAt(c)) {
				case '.' :
					sb.append("\\.");
					break;
				case '*' :
					sb.append(".*");
					break;
				case '?' :
					sb.append(".?");
					break;
				default :
					sb.append(string.charAt(c));
					break;
			}
		}
		string = sb.toString();
		if (string.endsWith("\\..*")) {
			sb.append("|");
			sb.append(string.substring(0, string.length() - 4));
		}
		return new Selector(sb.toString(), negated);
	}

	@Override
	public String toString() {
		return getPattern();
	}

	public Clause getClause() {
		return clause;
	}

	public void setClause(Clause clause) {
		this.clause = clause;
	}

	public static List<Selector> getInstructions(Clauses clauses) {
		List<Selector> result = new ArrayList<>();
		for (Map.Entry<String, Map<String, String>> entry : clauses.entrySet()) {
			Selector instruction = getPattern(entry.getKey());
			result.add(instruction);
		}
		return result;
	}

	public static <T> List<T> select(Collection<T> domain, List<Selector> instructions) {
		List<T> result = new ArrayList<>();
		Iterator<T> iterator = domain.iterator();
		value: while (iterator.hasNext()) {
			T value = iterator.next();
			for (Selector instruction : instructions) {
				if (instruction.matches(value.toString())) {
					if (!instruction.isNegated())
						result.add(value);
					continue value;
				}
			}
		}
		return result;
	}

}
