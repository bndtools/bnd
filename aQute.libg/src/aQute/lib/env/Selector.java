package aQute.lib.env;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Selector {

	public static class Filter implements FileFilter {

		private Selector	instruction;
		private boolean		recursive;
		private Pattern		doNotCopy;

		public Filter(Selector instruction, boolean recursive, Pattern doNotCopy) {
			this.instruction = instruction;
			this.recursive = recursive;
			this.doNotCopy = doNotCopy;
		}

		public Filter(Selector instruction, boolean recursive) {
			this(instruction, recursive, Pattern.compile("\\..*"));
		}

		public boolean isRecursive() {
			return recursive;
		}

		@Override
		public boolean accept(File pathname) {
			if (doNotCopy != null && doNotCopy.matcher(pathname.getName())
				.matches()) {
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
	final boolean		caseInsensitive;

	public Selector(String input) {
		this.input = input;

		String s = Header.removeDuplicateMarker(input);
		duplicate = !s.equals(input);

		if (s.startsWith("!")) {
			negated = true;
			s = s.substring(1);
		} else
			negated = false;

		if (s.endsWith(":i")) {
			caseInsensitive = true;
			s = s.substring(0, s.length() - 2);
		} else
			caseInsensitive = false;

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
						}
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

	@Override
	public String toString() {
		return input;
	}

	public Matcher getMatcher(String value) {
		if (pattern == null) {
			if (!caseInsensitive)
				pattern = Pattern.compile(match);
			else
				pattern = Pattern.compile(match, Pattern.CASE_INSENSITIVE);
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

	public boolean finds(String value) {
		return getMatcher(value).find();
	}

}
