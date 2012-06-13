package aQute.lib.osgi;

import java.io.*;
import java.util.regex.*;

public class Instruction {

	public static class Filter implements FileFilter {

		private Instruction	instruction;
		private boolean		recursive;
		private Pattern		doNotCopy;

		public Filter(Instruction instruction, boolean recursive, Pattern doNotCopy) {
			this.instruction = instruction;
			this.recursive = recursive;
			this.doNotCopy = doNotCopy;
		}

		public Filter(Instruction instruction, boolean recursive) {
			this(instruction, recursive, Pattern.compile(Constants.DEFAULT_DO_NOT_COPY));
		}

		public boolean isRecursive() {
			return recursive;
		}

		public boolean accept(File pathname) {
			if (doNotCopy != null && doNotCopy.matcher(pathname.getName()).matches()) {
				return false;
			}

			if (pathname.isDirectory() && isRecursive()) {
				return true;
			}

			if (instruction == null) {
				return true;
			}
			return !instruction.isNegated() == instruction.matches(pathname.getName());
		}
	}

	transient Pattern	pattern;
	transient boolean	optional;

	final String		input;
	final String		match;
	final boolean		negated;
	final boolean		duplicate;
	final boolean		literal;
	final boolean		any;

	public Instruction(String input) {
		this.input = input;

		String s = Processor.removeDuplicateMarker(input);
		duplicate = !s.equals(input);

		if (s.startsWith("!")) {
			negated = true;
			s = s.substring(1);
		} else
			negated = false;

		if (input.equals("*")) {
			any = true;
			literal = false;
			match = null;
			return;
		}

		any = false;
		if (s.startsWith("=")) {
			match = s.substring(1);
			literal = true;
		} else {
			boolean wildcards = false;

			StringBuilder sb = new StringBuilder();
			loop: for (int c = 0; c < s.length(); c++) {
				switch (s.charAt(c)) {
					case '.' :
						// If we end in a wildcard .* then we need to
						// also include the last full package. I.e.
						// com.foo.* includes com.foo (unlike OSGi)
						if (c == s.length() - 2 && '*' == s.charAt(c + 1)) {
							sb.append("(\\..*)?");
							wildcards = true;
							break loop;
						} else
							sb.append("\\.");

						break;
					case '*' :
						sb.append(".*");
						wildcards = true;
						break;
					case '$' :
						sb.append("\\$");
						break;
					case '?' :
						sb.append(".?");
						wildcards = true;
						break;
					case '|' :
						sb.append('|');
						wildcards = true;
						break;
					default :
						sb.append(s.charAt(c));
						break;
				}
			}

			if (!wildcards) {
				literal = true;
				match = s;
			} else {
				literal = false;
				match = sb.toString();
			}
		}

	}

	public boolean matches(String value) {
		if (any)
			return true;

		if (literal)
			return match.equals(value);
		else
			return getMatcher(value).matches();
	}

	public boolean isNegated() {
		return negated;
	}

	public String getPattern() {
		return match;
	}

	public String getInput() {
		return input;
	}

	public String toString() {
		return input;
	}

	public Matcher getMatcher(String value) {
		if (pattern == null) {
			pattern = Pattern.compile(match);
		}
		return pattern.matcher(value);
	}

	public void setOptional() {
		optional = true;
	}

	public boolean isOptional() {
		return optional;
	}

	public boolean isLiteral() {
		return literal;
	}

	public String getLiteral() {
		assert literal;
		return match;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public boolean isAny() {
		return any;
	}

}
