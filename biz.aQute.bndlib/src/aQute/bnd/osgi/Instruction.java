package aQute.bnd.osgi;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.libg.glob.Glob;

public class Instruction {

	public static class Filter implements FileFilter {
		private final static Pattern	DEFAULT_DO_NOT_COPY_P	= Pattern.compile(Constants.DEFAULT_DO_NOT_COPY);
		private Instruction				instruction;
		private boolean					recursive;
		private Pattern					doNotCopy;

		public Filter(Instruction instruction, boolean recursive, Pattern doNotCopy) {
			this.instruction = instruction;
			this.recursive = recursive;
			this.doNotCopy = doNotCopy;
		}

		public Filter(Instruction instruction, boolean recursive) {
			this(instruction, recursive, DEFAULT_DO_NOT_COPY_P);
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

	// Handle up to 4 sequential backslashes in the negative lookbehind.
	private static final String		ESCAPING	= "(?<!(?<!(?<!(?<!\\\\)\\\\)\\\\)\\\\)";
	private static final Pattern	WILDCARD	= Pattern.compile(ESCAPING + "[*?|({\\[]");
	private static final Pattern	BACKSLASH	= Pattern.compile(ESCAPING + "\\\\");
	private static final Pattern	ANY			= Pattern.compile(".*");

	private final String			input;
	private final String			match;
	private final boolean			negated;
	private final boolean			duplicate;
	private final boolean			literal;
	private final boolean			any;
	private final int				matchFlags;
	private Pattern					pattern;
	private boolean					optional;

	public Instruction(String input) {

		if (input == null || input.isEmpty())
			input = "!*";

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

		int flags = 0;
		if (s.endsWith(":i")) {
			flags = Pattern.CASE_INSENSITIVE;
			end -= 2;
		}

		if (s.charAt(start) == '=') {
			match = s.substring(start + 1, end);
			literal = true;
			matchFlags = flags | Pattern.LITERAL;
			return;
		}

		// If we end in a wildcard .* then we need to
		// also include the last full package. I.e.
		// com.foo.* includes com.foo (unlike OSGi)
		if (s.regionMatches(end - 2, ".*", 0, 2)) {
			s = s.substring(start, end - 2) + "(?:.*)?";
			literal = false;
		} else {
			s = s.substring(start, end);
			literal = !WILDCARD.matcher(s)
				.find();
		}

		if (literal) {
			match = (s.indexOf('\\') < 0) ? s
				: BACKSLASH.matcher(s)
					.replaceAll("");
			matchFlags = flags | Pattern.LITERAL;
		} else {
			match = s;
			matchFlags = flags;
			pattern = Glob.toPattern(match, matchFlags);
		}
	}

	public static Instruction legacy(String input) {
		if (input.equals("*")) {
			return new Instruction(input, null, null, false, 0, true, false, false);
		}

		String s = Processor.removeDuplicateMarker(input);
		boolean duplicate = !s.equals(input);

		int start = 0;
		int end = s.length();

		boolean negated = false;
		if (s.charAt(start) == '!') {
			negated = true;
			start++;
		}

		int matchFlags = 0;
		if (s.endsWith(":i")) {
			matchFlags = Pattern.CASE_INSENSITIVE;
			end -= 2;
		}

		if (s.charAt(start) == '=') {
			return new Instruction(input, s.substring(start + 1, end), null, negated, matchFlags, false, true,
				duplicate);
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
			return new Instruction(input, sb.toString(), null, negated, matchFlags, false, false, duplicate);
		} else {
			return new Instruction(input, s.substring(start, end), null, negated, matchFlags, false, true, duplicate);
		}
	}

	public Instruction(Pattern pattern) {
		this(pattern, false);
	}

	public Instruction(Pattern pattern, boolean negated) {
		this(pattern.pattern(), pattern.pattern(), pattern, negated, pattern.flags(), false, false, false);
	}

	private Instruction(String input, String match, Pattern pattern, boolean negated, int matchFlags, boolean any,
		boolean literal, boolean duplicate) {
		this.input = input;
		this.match = match;
		this.pattern = pattern;
		this.negated = negated;
		this.matchFlags = matchFlags;
		this.any = any;
		this.literal = literal;
		this.duplicate = duplicate;
	}

	public boolean matches(String value) {
		if (any)
			return true;

		if (literal) {
			if ((matchFlags & Pattern.CASE_INSENSITIVE) != 0) {
				return match.equalsIgnoreCase(value);
			}
			return match.equals(value);
		}

		return getMatcher(value).matches();
	}

	public boolean isNegated() {
		return negated;
	}

	public String getPattern() {
		return pattern == null ? null : pattern.pattern();
	}

	public String getInput() {
		return input;
	}

	@Override
	public String toString() {
		return input;
	}

	private Pattern pattern() {
		if (pattern == null) {
			if (match == null) {
				pattern = ANY;
			} else {
				pattern = Pattern.compile(match, matchFlags);
			}
		}
		return pattern;
	}

	public Matcher getMatcher(String value) {
		return pattern().matcher(value);
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
