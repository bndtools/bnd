package aQute.libg.qtokens;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import aQute.lib.regex.PatternConstants;

public class QuotedTokenizer implements Iterable<String> {
	private final static Pattern	TOKEN_P	= Pattern.compile(PatternConstants.TOKEN);
	private final String			string;
	private final String			separators;
	private final boolean			returnTokens;
	private final boolean			retainQuotes;
	private int						index	= 0;
	private String					peek;
	private char					separator;

	public QuotedTokenizer(String string, String separators, boolean returnTokens, boolean retainQuotes) {
		this.string = requireNonNull(string, "string argument must be not null");
		this.separators = requireNonNull(separators, "separators argument must be not null");
		this.returnTokens = returnTokens;
		this.retainQuotes = retainQuotes;
	}

	public QuotedTokenizer(String string, String separators, boolean returnTokens) {
		this(string, separators, returnTokens, false);
	}

	public QuotedTokenizer(String string, String separators) {
		this(string, separators, false);
	}

	private QuotedTokenizer copy() {
		return new QuotedTokenizer(string, separators, returnTokens, retainQuotes);
	}

	@Override
	public String toString() {
		return String.format("\"%s\" - \"%s\" - %s", string, separators, returnTokens);
	}

	public String nextToken(String separators) {
		separator = 0;
		if (peek != null) {
			String tmp = peek;
			peek = null;
			return tmp;
		}

		if (index == string.length()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		boolean hadstring = false; // means no further trimming
		boolean escaped = false; // means previous char was backslash
		int lastNonWhitespace = 0;
		while (index < string.length()) {
			char c = string.charAt(index++);

			if (escaped) {
				sb.append(c);
				escaped = false;
			} else {
				if (separators.indexOf(c) >= 0) {
					if (returnTokens) {
						peek = Character.toString(c);
					} else {
						separator = c;
					}
					break;
				}

				if (Character.isWhitespace(c)) {
					if (index == string.length()) {
						break;
					}
					if (sb.length() > 0) {
						sb.append(c);
					}
					continue;
				}

				switch (c) {
					case '"' :
					case '\'' :
						hadstring = true;
						quotedString(sb, c);
						break;

					case '\\' :
						escaped = true;
						// FALL-THROUGH
					default :
						sb.append(c);
						break;
				}
			}

			lastNonWhitespace = sb.length();
		}
		sb.setLength(lastNonWhitespace);
		String result = sb.toString();
		if (!hadstring) {
			if (result.isEmpty() && (index == string.length())) {
				return null;
			}
		}
		return result;
	}

	public String nextToken() {
		return nextToken(separators);
	}

	private void quotedString(StringBuilder sb, char quote) {
		boolean retain = retainQuotes || (sb.length() != 0);
		if (retain) {
			sb.append(quote);
		}
		while (index < string.length()) {
			char c = string.charAt(index++);
			if (c == quote) {
				if (retain) {
					sb.append(quote);
				}
				break;
			}
			if ((c == '\\') && (index < string.length())) {
				c = string.charAt(index++);
				if (retain || (c != quote)) {
					sb.append('\\');
				}
			}
			sb.append(c);
		}
	}

	public String[] getTokens() {
		return stream(this).toArray(String[]::new);
	}

	public char getSeparator() {
		return separator;
	}

	public List<String> getTokenSet() {
		return stream(this).collect(toList());
	}

	public Stream<String> stream() {
		return stream(copy());
	}

	private static Stream<String> stream(QuotedTokenizer qt) {
		return StreamSupport.stream(new TokenSpliterator(qt), false);
	}

	@Override
	public Spliterator<String> spliterator() {
		return new TokenSpliterator(copy());
	}

	static final class TokenSpliterator extends AbstractSpliterator<String> {
		private final QuotedTokenizer qt;

		TokenSpliterator(QuotedTokenizer qt) {
			super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL);
			this.qt = qt;
		}

		@Override
		public boolean tryAdvance(Consumer<? super String> action) {
			requireNonNull(action);
			String next = qt.nextToken();
			if (next != null) {
				action.accept(next);
				return true;
			}
			return false;
		}

		@Override
		public void forEachRemaining(Consumer<? super String> action) {
			requireNonNull(action);
			for (String next; (next = qt.nextToken()) != null;) {
				action.accept(next);
			}
		}
	}

	@Override
	public Iterator<String> iterator() {
		return new TokenIterator(copy());
	}

	static final class TokenIterator implements Iterator<String> {
		private final QuotedTokenizer	qt;
		private boolean					hasNext	= false;
		private String					next;

		TokenIterator(QuotedTokenizer qt) {
			this.qt = qt;
		}

		@Override
		public boolean hasNext() {
			if (hasNext) {
				return true;
			}
			next = qt.nextToken();
			return hasNext = (next != null);
		}

		@Override
		public String next() {
			if (hasNext()) {
				hasNext = false;
				return next;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void forEachRemaining(Consumer<? super String> action) {
			requireNonNull(action);
			if (hasNext) {
				action.accept(next);
				hasNext = false;
			}
			for (String next; (next = qt.nextToken()) != null;) {
				action.accept(next);
			}
		}
	}

	@Override
	public void forEach(Consumer<? super String> action) {
		requireNonNull(action);
		QuotedTokenizer qt = copy();
		for (String next; (next = qt.nextToken()) != null;) {
			action.accept(next);
		}
	}

	/**
	 * Quote a string when it is not a token (OSGi). If the string is already
	 * quoted (or backslash quoted) then these are removed before inspection to
	 * see if it is a token.
	 *
	 * @param sb the output
	 * @param value the value to quote
	 */
	public static boolean quote(StringBuilder sb, String value) {
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
