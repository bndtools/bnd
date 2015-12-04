package aQute.lib.utf8properties;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.hex.Hex;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;

final class PropertiesParser {
	private final static Pattern	VALID_KEY_P		= Pattern.compile("[^\n,;!#\\()]+");
	private final String			source;
	private final int				length;
	private final Reporter			reporter;
	private final String			file;
	private static final char		MIN_DELIMETER	= '\t';
	private static final char		MAX_DELIMETER	= '=';
	private final static byte[]		INFO			= new byte[MAX_DELIMETER + 1];
	private final static byte		WS				= 1;
	private final static byte		KEY				= 2;
	private final static byte		LINE			= 4;

	static {
		INFO['\t'] = KEY + WS;
		INFO['\n'] = KEY + LINE;
		INFO['\f'] = KEY + WS;
		INFO[' '] = KEY + WS;
		INFO[':'] = KEY;
		INFO['='] = KEY;
	}

	private int			n		= 0;
	private int			line	= 0;
	private int			pos		= -1;
	private int			marker	= 0;
	private char		current;
	private Properties	properties;

	PropertiesParser(String source, String file, Reporter reporter, Properties properties) {
		this.source = source;
		this.file = file;
		this.reporter = reporter;
		this.length = source.length();
		this.properties = properties;
	}

	boolean hasNext() {
		return n < length;
	}

	char next() {
		if (n >= length)
			return current = '\n';

		current = source.charAt(n++);
		try {
			switch (current) {
				case '\\' :
					if (peek() == '\n') {
						next(); // current == newline
						next(); // first character on new line
						skipWhitespace();
						return current;
					}
					return '\\';
				case '\r' :
					if (pos == 0)
						return next();
					if (peek() == '\n')
						return next();
					return '\n';

				case '\n' :
					line++;
					pos = -1;
					return current;

				case '\t' :
				case '\f' :
					return current;

				default :
					if (current < ' ') {
						error("Invalid character in properties: %x at pos %s", current, pos);
						return current = '?';
					} else
						return current;
			}
		}
		finally {
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
			return source.charAt(n);
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

			String key = token(KEY);

			if (!isValidKey(key)) {
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

				String value = token(LINE);
				properties.put(key, value);

			} else {
				error("No value specified for key: %s. An empty value should be specified as '<key>:' or '<key>='",
						key);
				properties.put(key, "");
				continue;
			}
			assert current == '\n';
		}

		int start = n;

	}

	private boolean isValidKey(String key) {
		Matcher matcher = VALID_KEY_P.matcher(key);
		return matcher.matches();
	}

	private void skipWhitespace() {
		skip(WS);
	}

	public boolean isEmptyOrComment(char c) {
		return c == '\n' || c == '#' || c == '!';
	}

	public void skipLine() {
		while (!isIn(LINE))
			next();
	}

	private final String token(byte delimeters) {
		StringBuilder sb = new StringBuilder();
		while (!isIn(delimeters)) {
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

	private final boolean isIn(byte delimeters) {
		return current <= MAX_DELIMETER && current >= MIN_DELIMETER && (INFO[current] & delimeters) != 0;
	}

	private final char backslash() {
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
			SetLocation loc = reporter.error(msg + ": <<" + context + ">>", args);
			loc.line(line);
			loc.context(context);
			if (file != null)
				loc.file(file);
			loc.length(context.length());
		}
	}

	private String context() {
		int loc = n;
		while (loc < length && source.charAt(loc) != '\n')
			loc++;
		return source.substring(marker, loc);
	}

}