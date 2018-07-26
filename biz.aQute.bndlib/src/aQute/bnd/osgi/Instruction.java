package aQute.bnd.osgi;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			return instruction.matches(pathname.getName()) ^ instruction.isNegated();
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
	private final int	matchFlags;

	public Instruction(String input) {
		this.input = input;

		if (input.equals("*")) {
			any = true;
			literal = false;
			match = null;
			negated = false;
			matchFlags = 0;
			duplicate = false;
			return;
		}
		any = false;

		String s = Processor.removeDuplicateMarker(input);
		duplicate = !s.equals(input);

		int start = 0;
		int end = s.length();

		if (s.charAt(start) == '!') {
			negated = true;
			start++;
		} else {
			negated = false;
		}

		if (s.endsWith(":i")) {
			matchFlags = Pattern.CASE_INSENSITIVE;
			end -= 2;
		} else {
			matchFlags = 0;
		}

		if (s.charAt(start) == '=') {
			match = s.substring(start + 1, end);
			literal = true;
			return;
		}

		boolean wildcards = false;
		StringBuilder sb = new StringBuilder();
		loop: for (int c = start; c < end; c++) {
			switch (s.charAt(c)) {
				case '.' :
					// If we end in a wildcard .* then we need to
					// also include the last full package. I.e.
					// com.foo.* includes com.foo (unlike OSGi)
					if (c == end - 2 && '*' == s.charAt(c + 1)) {
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

		if (wildcards) {
			literal = false;
			match = sb.toString();
		} else {
			literal = true;
			match = s.substring(start, end);
		}
	}

	public Instruction(Pattern pattern) {
		this(pattern, false);
	}

	public Instruction(Pattern pattern, boolean negated) {
		this.pattern = pattern;
		this.negated = negated;
		input = match = pattern.pattern();
		matchFlags = pattern.flags();
		any = false;
		literal = false;
		duplicate = false;
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
			String m = match == null ? ".*" : match;
			pattern = Pattern.compile(m, matchFlags);
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
