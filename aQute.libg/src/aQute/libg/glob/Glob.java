package aQute.libg.glob;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glob {

	enum State {
		SIMPLE,
		CURLIES,
		BRACKETS,
		QUOTED
	}

	public static final Glob	ALL	= new Glob("*");
	private final String		glob;
	private final Pattern		pattern;

	public Glob(String globString) {
		this(globString, 0);
	}

	public Glob(String globString, int flags) {
		this(globString, toPattern(globString, flags));
	}

	protected Glob(String globString, Pattern pattern) {
		this.glob = globString;
		this.pattern = pattern;
	}

	public String glob() {
		return glob;
	}

	public Pattern pattern() {
		return pattern;
	}

	public Matcher matcher(CharSequence input) {
		return pattern.matcher(input);
	}

	@Override
	public String toString() {
		return glob;
	}

	public static Pattern toPattern(String line) {
		return toPattern(line, 0);
	}

	public static Pattern toPattern(String line, int flags) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen << 2);
		int curlyLevel = 0;
		State state = State.SIMPLE;

		char previousChar = 0;
		for (int i = 0; i < strLen; i++) {
			char currentChar = line.charAt(i);
			switch (currentChar) {
				case '*' :
					if ((state == State.SIMPLE || state == State.CURLIES) && !isEnd(previousChar)) {
						sb.append('.');
					}
					sb.append(currentChar);
					break;
				case '?' :
					if ((state == State.SIMPLE || state == State.CURLIES) && !isStart(previousChar)
						&& !isEnd(previousChar)) {
						sb.append('.');
					} else {
						sb.append(currentChar);
					}
					break;
				case '+' :
					if ((state == State.SIMPLE || state == State.CURLIES) && !isEnd(previousChar)) {
						sb.append('\\');
					}
					sb.append(currentChar);
					break;
				case '\\' :
					sb.append(currentChar);
					if (i + 1 < strLen) {
						char nextChar = line.charAt(++i);
						if (state == State.SIMPLE && nextChar == 'Q') {
							state = State.QUOTED;
						} else if (state == State.QUOTED && nextChar == 'E') {
							state = State.SIMPLE;
						}
						sb.append(nextChar);
					}
					break;
				case '[' :
					if (state == State.SIMPLE) {
						state = State.BRACKETS;
					}
					sb.append(currentChar);
					break;
				case ']' :
					if (state == State.BRACKETS) {
						state = State.SIMPLE;
					}
					sb.append(currentChar);
					break;

				case '{' :
					if ((state == State.SIMPLE || state == State.CURLIES) && !isEnd(previousChar)) {
						state = State.CURLIES;
						sb.append("(?:");
						curlyLevel++;
					} else {
						sb.append(currentChar);
					}
					break;
				case '}' :
					if (state == State.CURLIES && curlyLevel > 0) {
						sb.append(')');
						currentChar = ')';
						curlyLevel--;
						if (curlyLevel == 0)
							state = State.SIMPLE;
					} else {
						sb.append(currentChar);
					}
					break;
				case ',' :
					if (state == State.CURLIES) {
						sb.append('|');
					} else {
						sb.append(currentChar);
					}
					break;
				case '^' :
				case '.' :
				case '$' :
				case '@' :
				case '%' :
					if (state == State.SIMPLE || state == State.CURLIES)
						sb.append('\\');

					// FALL THROUGH
				default :
					sb.append(currentChar);
					break;
			}
			previousChar = currentChar;
		}
		return Pattern.compile(sb.toString(), flags);
	}

	private static boolean isStart(char c) {
		return c == '(';
	}

	private static boolean isEnd(char c) {
		return c == ')' || c == ']';
	}

	public void select(Collection<?> objects) {
		for (Iterator<?> i = objects.iterator(); i.hasNext();) {
			String s = i.next()
				.toString();
			if (!matches(s))
				i.remove();
		}
	}

	public void select(List<?> objects) {
		this.select((Collection<?>) objects);
	}

	/**
	 * Get a list of files that match the glob expression
	 *
	 * @param root the directory to get the files from
	 * @param recursive to traverse the dirs recursive
	 * @return file list
	 */
	public List<File> getFiles(File root, boolean recursive, boolean usePath) {
		List<File> result = new ArrayList<>();
		getFiles(root, result, recursive, usePath);
		return result;
	}

	public void getFiles(File root, List<File> result, boolean recursive, boolean usePath) {
		if (root == null || !root.isDirectory())
			return;

		for (File sub : root.listFiles()) {
			if (sub.isFile()) {
				String s = usePath ? sub.getAbsolutePath() : sub.getName();
				if (matcher(s).matches())
					result.add(sub);
			} else if (recursive && sub.isDirectory())
				getFiles(sub, result, recursive, usePath);
		}
	}

	public static boolean in(Glob[] globs, String key) {
		for (Glob g : globs) {
			if (g.matcher(key)
				.matches())
				return true;
		}
		return false;
	}

	public static boolean in(Collection<? extends Glob> globs, String key) {
		for (Glob g : globs) {
			if (g.matcher(key)
				.matches())
				return true;
		}
		return false;
	}

	public int finds(CharSequence s) {
		Matcher matcher = matcher(s);
		if (matcher.find()) {
			return matcher.start();
		}
		return -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((glob == null) ? 0 : glob.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Glob other = (Glob) obj;
		if (glob == null) {
			if (other.glob != null)
				return false;
		} else if (!glob.equals(other.glob))
			return false;
		return true;
	}

	public boolean matches(String s) {
		return matches((CharSequence) s);
	}

	public boolean matches(CharSequence s) {
		return matcher(s).matches();
	}
}
