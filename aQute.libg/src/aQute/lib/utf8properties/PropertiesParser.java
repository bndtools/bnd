package aQute.lib.utf8properties;

import java.util.Objects;
import java.util.Properties;

import aQute.lib.hex.Hex;
import aQute.lib.strings.Strings;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;

final class PropertiesParser {
	private final char[]		source;
	private final int			length;
	private final Reporter		reporter;
	private final String		file;
	private static final char	MIN_DELIMETER	= '\t';
	private static final char	MAX_DELIMETER	= '\\';
	private final static byte[]	INFO			= new byte[MAX_DELIMETER + 1];
	private final static byte	WS				= 1;
	private final static byte	KEY				= 2;
	private final static byte	LINE			= 4;
	private final static byte	NOKEY			= 8;

	static {
		INFO['\t'] = KEY + WS;
		INFO['\n'] = KEY + LINE;
		INFO['\f'] = KEY + WS;
		INFO[' '] = KEY + WS;
		INFO[','] = NOKEY;
		INFO[';'] = NOKEY;
		INFO['!'] = NOKEY;
		INFO['\''] = NOKEY;
		INFO['"'] = NOKEY;
		INFO['#'] = NOKEY;
		INFO['('] = NOKEY;
		INFO[')'] = NOKEY;
		INFO[':'] = KEY;
		INFO['='] = KEY;
		INFO['\\'] = NOKEY;
	}

	private int			n				= 0;
	private int			line			= 0;
	private int			pos				= -1;
	private int			marker			= 0;
	private char		current;
	private Properties	properties;
	private boolean		validKey;
	private boolean		continuation	= true;
	private String[]	syntaxHeaders	= new String[0];

	PropertiesParser(String source, String file, Reporter reporter, Properties properties) {
		this.source = source.toCharArray();
		this.file = file;
		this.reporter = reporter;
		this.length = this.source.length;
		this.properties = properties;
	}

	boolean hasNext() {
		return n < length;
	}

	char next() {
		if (n >= length)
			return current = '\n';

		current = source[n++];
		try {
			switch (current) {
				case '\\' :
					if (continuation) {
						char p = peek();
						if (p == '\r' || p == '\n') {
							next(); // skip line ending
							next(); // first character on new line
							skipWhitespace();
						}
					}
					return current;

				case '\r' :
					current = '\n';
					if (peek() == '\n') {
						n++;
					}
					line++;
					pos = -1;
					return current;

				case '\n' :
					if (peek() == '\r') {
						// a bit weird, catches \n\r
						n++;
					}
					line++;
					pos = -1;
					return current;

				case '\t' :
				case '\f' :
					return current;

				default :
					if (current < ' ') {
						error("Invalid character in properties: %x at pos %s", Integer.valueOf(current), pos);
						return current = '?';
					}
					return current;
			}
		} finally {
			pos++;
		}
	}

	void skip(byte delimeters) {
		while (isIn(delimeters)) {
			next();
		}
	}

	char peek() {
		if (hasNext())
			return source[n];
		else
			return '\n';
	}

	void parse() {

		while (hasNext()) {
			marker = n;
			next();
			skipWhitespace();

			if (isEmptyOrComment(current)) {
				skipLine();
				continue;
			}

			this.validKey = true;
			String key = key();

			if (!validKey) {
				error("Invalid property key: `%s`", key);
			}

			skipWhitespace();

			if (current == ':' || current == '=') {
				next();
				skipWhitespace();
				if (current == '\n') {
					properties.put(key, "");
					continue;
				}
			}

			if (current != '\n') {

				String value = token(LINE, isSyntaxHeader(key));
				properties.put(key, value);

			} else {
				error("No value specified for key: %s. An empty value should be specified as '%<s:' or '%<s='", key);
				properties.put(key, "");
				continue;
			}
			assert current == '\n';
		}

		int start = n;

	}

	private boolean isSyntaxHeader(String key) {
		if (key == null || key.length() == 0)
			return false;

		if (key.startsWith("-"))
			return true;

		return Strings.in(syntaxHeaders, key);
	}

	private void skipWhitespace() {
		skip(WS);
	}

	public boolean isEmptyOrComment(char c) {
		return c == '\n' || c == '#' || c == '!';
	}

	public void skipLine() {
		continuation = false;
		try {
			while (!isIn(LINE))
				next();
		} finally {
			continuation = true;
		}
	}

	private String token(byte delimeters, boolean check) {
		StringBuilder sb = new StringBuilder();
		char quote = 0;
		boolean expectDelimeter = false;

		while (!isIn(delimeters)) {
			char tmp = current;
			if (tmp == '\\') {
				tmp = backslash();

				if (tmp == 0) // we hit \\n\n
					break;

				if (check && quote != 0 && tmp != quote && isQuote(tmp)) {
					error(
						"Found backslash escaped quote character `\\%s` while quoted-string's quote character  is `%s`. This is not an error but easier to read when not escaped ",
						"" + tmp, "" + (quote));
				}

			} else if (check) {
				switch (current) {
					case '\\' :
						break;

					case '\'' :
					case '"' :
						if (quote == 0) {
							if (expectDelimeter) {
								error(
									"Found a quote '%s' while expecting a delimeter. You should quote the whole values, you can use both single and double quotes",
									tmp);
								expectDelimeter = false;
							}
							quote = tmp;
						} else if (quote == tmp) {
							if (isEven(countBackslashesAtEnd(sb))) {
								quote = 0;
								expectDelimeter = true;
							}
						}
						break;

					case '\u00A0' :
						invalidWhitespace(quote, "NON BREAKING SPACE (\\u00A0)");
						break;

					case '\u2007' :
						invalidWhitespace(quote, "FIGURE SPACE (\\u2007) ILLEGAL IN PROPERTIES");
						break;

					case '\u202F' :
						invalidWhitespace(quote, "NARROW NO-BREAK SPACE (\\u202F)  ILLEGAL IN PROPERTIES");
						break;

					case ' ' :
					case '\t' :
					case '\f' :
						break;

					case ';' :
					case ',' :
					case '=' :
					case ':' :
						expectDelimeter = false;
						break;

					default :
						if (expectDelimeter) {
							error("Expected a delimeter, like comma or semicolon, after a quoted string but found '%s'",
								tmp);
							expectDelimeter = false;
						}
						break;
				}
			}
			sb.append(tmp);
			next();
		}
		return sb.toString();
	}

	private boolean isQuote(char tmp) {
		return tmp == '\'' || tmp == '"';
	}

	private boolean isEven(int count) {
		return (count & 1) == 0;
	}

	private int countBackslashesAtEnd(StringBuilder sb) {
		int n = 0;
		int r = sb.length();
		while (--r >= 0) {
			if (sb.charAt(r) != '\\') {
				return n;
			}
			n++;
		}
		return n;
	}

	private void invalidWhitespace(int quote, String type) {
		if (quote == 0)
			error("Non breaking space found [%s] at (line=%s,pos=%s)", type, line, pos);
	}

	private String key() {
		StringBuilder sb = new StringBuilder();
		while (!isIn(KEY)) {
			if (isIn(NOKEY))
				validKey = false;

			char tmp = current;
			if (tmp == '\\') {
				tmp = backslash();

				if (tmp == 0) // we hit \\n\n
					break;
			}
			sb.append(tmp);
			next();
		}
		return sb.toString();
	}

	private boolean isIn(byte delimeters) {
		if (current < MIN_DELIMETER || current > MAX_DELIMETER)
			return false;
		return (INFO[current] & delimeters) != 0;
	}

	private char backslash() {
		char c;
		c = next();
		switch (c) {
			case '\n' :
				return 0;

			case 'u' :
				StringBuilder sb = new StringBuilder();
				c = 0;
				for (int i = 0; i < 4; i++) {
					sb.append(next());
				}
				String unicode = sb.toString();
				if (!Hex.isHex(unicode)) {
					error("Invalid unicode string \\u%s", sb);
					return '?';
				} else {
					return (char) Integer.parseInt(unicode, 16);
				}

			case ':' :
			case '=' :
				return c;
			case 't' :
				return '\t';
			case 'f' :
				return '\f';
			case 'r' :
				return '\r';
			case 'n' :
				return '\n';
			case '\\' :
				return '\\';

			case '\f' :
			case '\t' :
			case ' ' :
				error("Found \\<whitespace>. This is allowed in a properties file but not in bnd to prevent mistakes");
				return c;

			default :
				return c;
		}
	}

	private void error(String msg, Object... args) {
		if (reporter != null) {
			int line = this.line;
			String context = context();
			SetLocation loc;
			loc = reporter.warning("%s: <<%s>>", Strings.format(msg, args), context);

			loc.line(line);
			loc.context(context);
			if (file != null)
				loc.file(file);
			loc.length(context.length());
		}
	}

	private String context() {
		int loc = n;
		while (loc < length && source[loc] != '\n')
			loc++;
		return new String(source, marker, loc - marker);
	}

	public void setSyntaxHeaders(String[] syntaxHeaders) {
		Objects.requireNonNull(syntaxHeaders);
		this.syntaxHeaders = syntaxHeaders;
	}

}
