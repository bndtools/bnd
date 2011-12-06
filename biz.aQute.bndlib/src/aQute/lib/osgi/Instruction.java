package aQute.lib.osgi;

import java.util.*;
import java.util.regex.*;

import aQute.libg.generics.*;

public class Instruction {
	Pattern	pattern;
	String	instruction;
	boolean	negated;
	boolean	optional;

	public Instruction(String instruction, boolean negated) {
		this.instruction = instruction;
		this.negated = negated;
	}

	public boolean matches(String value) {
		return getMatcher(value).matches();
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
	 * @return
	 */
	public static Instruction getPattern(String string) {
		boolean negated = false;
		if (string.startsWith("!")) {
			negated = true;
			string = string.substring(1);
		}
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < string.length(); c++) {
			switch (string.charAt(c)) {
			case '.':
				sb.append("\\.");
				break;
			case '*':
				sb.append(".*");
				break;
			case '?':
				sb.append(".?");
				break;
			default:
				sb.append(string.charAt(c));
				break;
			}
		}
		string = sb.toString();
		if (string.endsWith("\\..*")) {
			sb.append("|");
			sb.append(string.substring(0, string.length() - 4));
		}
		return new Instruction(sb.toString(), negated);
	}

	public String toString() {
		return getPattern();
	}

	public Matcher getMatcher(String value) {
		if (pattern == null) {
			pattern = Pattern.compile(instruction);
		}
		return pattern.matcher(value);
	}

	public int hashCode() {
		return instruction.hashCode();
	}

	public boolean equals(Object other) {
		return other != null && (other instanceof Instruction)
				&& instruction.equals(((Instruction) other).instruction);
	}

	public void setOptional() {
		optional = true;
	}

	public boolean isOptional() {
		return optional;
	}

	public static Map<Instruction, Map<String, String>> replaceWithInstruction(
			Map<String, Map<String, String>> header) {
		Map<Instruction, Map<String, String>> map = Processor.newMap();
		for (Iterator<Map.Entry<String, Map<String, String>>> e = header.entrySet().iterator(); e
				.hasNext();) {
			Map.Entry<String, Map<String, String>> entry = e.next();
			String pattern = entry.getKey();
			Instruction instr = getPattern(pattern);
			String presence = entry.getValue().get(Constants.PRESENCE_DIRECTIVE);
			if ("optional".equals(presence))
				instr.setOptional();
			map.put(instr, entry.getValue());
		}
		return map;
	}

	public static <T> Collection<T> select(Collection<Instruction> matchers, Collection<T> targets) {
		Collection<T> result = Create.list();
		outer: for (T t : targets) {
			String s = t.toString();
			for (Instruction i : matchers) {
				if (i.matches(s)) {
					if (!i.isNegated())
						result.add(t);
					continue outer;
				}
			}
		}
		return result;
	}

	public static Collection<Instruction> toInstruction(String commaSeparated) {
		if (commaSeparated == null || commaSeparated.length() == 0)
			return Collections.emptyList();

		String[] list = commaSeparated.split("\\s*,\\s*");
		List<Instruction> result = new ArrayList<Instruction>();
		for (String member : list)
			result.add(Instruction.getPattern(member));

		return result;

	}

	public static boolean matches(Collection<Instruction> x, String value) {
		for ( Instruction i : x ) {
			if ( i.matches(value) )
				return true;
		}
		return false;
	}
}
