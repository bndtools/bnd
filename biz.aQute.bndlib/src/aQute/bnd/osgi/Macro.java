package aQute.bnd.osgi;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.base64.Base64;
import aQute.lib.collections.Iterables;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.filter.ExtendedFilter;
import aQute.lib.filter.Get;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * Provide a macro processor. This processor can replace variables in strings
 * based on a properties and a domain. The domain can implement functions that
 * start with a "_" and take args[], the names of these functions are available
 * as functions in the macro processor (without the _). Macros can nest to any
 * depth but may not contain loops. Add POSIX macros: ${#parameter} String
 * length. ${parameter%word} Remove smallest suffix pattern. ${parameter%%word}
 * Remove largest suffix pattern. ${parameter#word} Remove smallest prefix
 * pattern. ${parameter##word} Remove largest prefix pattern.
 */
public class Macro {
	private final static String		NULLVALUE		= "c29e43048791e250dfd5723e7b8aa048df802c9262cfa8fbc4475b2e392a8ad2";
	private final static String		LITERALVALUE	= "017a3ddbfc0fcd27bcdb2590cdb713a379ae59ef";
	private final static Pattern	NUMERIC_P		= Pattern.compile("[-+]?(\\d*\\.?\\d+|\\d+\\.)(e[-+]?[0-9]+)?");
	private final static Pattern	PRINTF_P		= Pattern.compile(
		"%(?:(\\d+)\\$)?(-|\\+|0|\\(|,|\\^|#| )*(\\d*)?(?:\\.(\\d+))?(a|A|b|B|h|H|d|f|c|s|x|X|u|o|z|Z|e|E|g|G|p|n|b|B|%)");
	Processor				domain;
	Reporter				reporter;
	Object					targets[];
	boolean					flattening;
	String					profile;
	private boolean			nosystem;
	ScriptEngine			engine			= null;
	ScriptContext			context			= null;
	Bindings				bindings		= null;
	StringWriter			stdout			= new StringWriter();
	StringWriter			stderr			= new StringWriter();

	public Macro(Processor domain, Object... targets) {
		this.domain = domain;
		this.reporter = domain;
		this.targets = targets;
		if (targets != null) {
			for (Object o : targets) {
				assert o != null;
			}
		}
	}

	public String process(String line, Processor source) {
		return process(line, new Link(source, null, line));
	}

	String process(String line, Link link) {
		StringBuilder sb = new StringBuilder();
		process(line, 0, '\u0000', '\u0000', sb, link);
		return sb.toString();
	}

	int process(CharSequence org, int index, char begin, char end, StringBuilder result, Link link) {
		if (org == null) { // treat null like empty string
			return index;
		}
		StringBuilder line = new StringBuilder(org);
		int nesting = 1;

		StringBuilder variable = new StringBuilder();

		outer: while (index < line.length()) {
			char c1 = line.charAt(index++);
			if (c1 == end) {
				if (--nesting == 0) {
					result.append(replace(variable.toString(), link, begin, end));
					return index;
				}
			} else if (c1 == begin)
				nesting++;
			else if (c1 == '\\' && index < line.length() - 1 && line.charAt(index) == '$') {
				// remove the escape backslash and interpret the dollar
				// as a
				// literal
				index++;
				variable.append('$');
				continue outer;
			} else if (c1 == '$' && index < line.length() - 2) {
				char c2 = line.charAt(index);
				char terminator = getTerminator(c2);
				if (terminator != 0) {
					index = process(line, index + 1, c2, terminator, variable, link);
					continue outer;
				}
			} else if (c1 == '.' && index < line.length() && line.charAt(index) == '/') {
				// Found the sequence ./
				if (index == 1 || Character.isWhitespace(line.charAt(index - 2))) {
					// make sure it is preceded by whitespace or starts at begin
					index++;
					variable.append(IO.absolutePath(domain.getBase()));
					variable.append('/');
					continue outer;
				}
			}
			variable.append(c1);
		}
		result.append(variable);
		return index;
	}

	public static char getTerminator(char c) {
		switch (c) {
			case '(' :
				return ')';
			case '[' :
				return ']';
			case '{' :
				return '}';
			case '<' :
				return '>';
			case '\u00ab' : // Guillemet double << >>
				return '\u00bb';
			case '\u2039' : // Guillemet single
				return '\u203a';
		}
		return 0;
	}

	protected String getMacro(String key, Link link) {
		return getMacro(key, link, '{', '}', null);
	}

	private String getMacro(String key, Link link, char begin, char end, String profile) {
		if (link != null && link.contains(key))
			return "${infinite:" + link.toString() + "}";

		if (key != null) {
			key = key.trim();
			if (!key.isEmpty()) {
				String keyins = key;
				if (profile != null) {
					key = "[" + profile + "]" + key;
					keyins = "\\[" + profile + "\\]" + keyins;
				}
				if (key.indexOf(';') < 0) {
					Instruction ins = new Instruction(keyins);
					if (!ins.isLiteral()) {
						String keyname = key;
						return domain.stream()
							.filter(ins::matches)
							.sorted()
							.map(k -> replace(k, new Link(domain, link, keyname), begin, end))
							.filter(Objects::nonNull)
							.collect(Strings.joining());
					}
					key = ins.getLiteral();
				}

				for (Processor source = domain; source != null; source = source.getParent()) {
					String value = source.getProperties()
						.getProperty(key);
					if (value != null) {
						return process(value, new Link(source, link, key));
					}
				}

				String value = doCommands(key, link);
				if (value != null) {
					if (value == NULLVALUE)
						return null;
					if (value == LITERALVALUE)
						return LITERALVALUE;
					return process(value, new Link(domain, link, key));
				}

				if (key != null && key.trim()
					.length() > 0) {
					value = System.getProperty(key);
					if ((value == null) && key.startsWith("env.")) {
						value = System.getenv(key.substring(4));
					}
					if (value != null)
						return value;
				}

				if (key != null && key.indexOf(';') >= 0) {
					String parts[] = SEMICOLON_P.split(key, 0);
					if (parts.length > 1) {
						if (parts.length >= 16) {
							reporter.error("too many arguments for template: %s, max is 16", key);
						}

						String template = domain.getProperties()
							.getProperty(parts[0]);
						if (template != null) {
							domain = new Processor(domain);
							for (int i = 0; i < 16; i++) {
								domain.setProperty("" + i, i < parts.length ? parts[i] : "null");
							}
							String joined = Arrays.stream(parts, 1, parts.length)
								.collect(Strings.joining());
							domain.setProperty("#", joined);
							try {
								value = process(template, new Link(domain, link, key));
								if (value != null)
									return value;
							} finally {
								domain = domain.getParent();
							}
						}
					}
				}
			} else {
				reporter.warning("Found empty macro key");
			}
		} else {
			reporter.warning("Found null macro key");
		}

		// Prevent recursion, but try to get a profiled variable
		if (key != null && !key.startsWith("[") && !key.equals(Constants.PROFILE)) {
			if (profile == null)
				profile = domain.getProperty(Constants.PROFILE);
			if (profile != null) {
				String replace = getMacro(key, link, begin, end, profile);
				if (replace != null)
					return replace;
			}
		}
		return null;
	}

	public String replace(String key, Link link) {
		return replace(key, link, '{', '}');
	}

	private String replace(String key, Link link, char begin, char end) {
		String value = getMacro(key, link, begin, end, null);
		if (value != LITERALVALUE) {
			if (value != null)
				return value;
			if (!flattening && !key.startsWith("@"))
				reporter.warning("No translation found for macro: %s", key);
		}
		return "$" + begin + key + end;
	}

	/**
	 * Parse the key as a command. A command consist of parameters separated by
	 * ':'.
	 */
	// Handle up to 4 sequential backslashes in the negative lookbehind.
	private static final String		ESCAPING	= "(?<!(?<!(?<!(?<!\\\\)\\\\)\\\\)\\\\)";
	private static final String		SEMICOLON			= ";";
	private static final String		ESCAPED_SEMICOLON	= "\\\\" + SEMICOLON;
	private static final Pattern	SEMICOLON_P			= Pattern.compile(ESCAPING + SEMICOLON);
	private static final Pattern	ESCAPED_SEMICOLON_P	= Pattern.compile(ESCAPING + ESCAPED_SEMICOLON);

	@SuppressWarnings("resource")
	private String doCommands(String key, Link source) {
		String[] args = SEMICOLON_P.split(key, 0);
		if (args == null || args.length == 0)
			return null;

		for (int i = 0; i < args.length; i++)
			if (args[i].indexOf('\\') >= 0) {
				args[i] = ESCAPED_SEMICOLON_P.matcher(args[i])
					.replaceAll(SEMICOLON);
			}

		if (args[0].startsWith("^")) {
			String varname = args[0].substring(1)
				.trim();

			if (source != null) {
				Processor parent = source.start.getParent();
				if (parent != null)
					return parent.getProperty(varname);
			}
			return null;
		}

		Processor rover = domain;
		while (rover != null) {
			String result = doCommand(rover, args[0], args);
			if (result != null)
				return result;

			rover = rover.getParent();
		}

		for (int i = 0; targets != null && i < targets.length; i++) {
			String result = doCommand(targets[i], args[0], args);
			if (result != null)
				return result;
		}

		return doCommand(this, args[0], args);
	}

	private String doCommand(Object target, String method, String[] args) {
		if (target == null)
			; // System.err.println("Huh? Target should never be null " +
		// domain);
		else {
			// Assume macro names do not start with '-'
			if (method.startsWith("-")) {
				return null;
			}

			String part = method.replaceAll("-", "_");
			for (int i = 0; i < part.length(); i++) {
				if (!Character.isJavaIdentifierPart(part.charAt(i)))
					return null;
			}

			String cname = "_" + part;
			Method m;
			try {
				m = target.getClass()
					.getMethod(cname, String[].class);
			} catch (NoSuchMethodException e) {
				return null;
			}
			MethodHandle mh;
			try {
				mh = publicLookup().unreflect(m);
			} catch (Exception e) {
				reporter.warning("Exception in replace: method=%s %s ", method, Exceptions.toString(e));
				return NULLVALUE;
			}
			try {
				Object result = Modifier.isStatic(m.getModifiers()) ? mh.invoke(args) : mh.invoke(target, args);
				return result == null ? NULLVALUE : result.toString();
			} catch (Error e) {
				throw e;
			} catch (WrongMethodTypeException e) {
				reporter.warning("Exception in replace: method=%s %s ", method, Exceptions.toString(e));
				return NULLVALUE;
			} catch (Exception e) {
				reporter.error("%s, for cmd: %s, arguments; %s", e.getMessage(), method, Arrays.toString(args));
				return NULLVALUE;
			} catch (Throwable e) {
				reporter.warning("Exception in replace: method=%s %s ", method, Exceptions.toString(e));
				return NULLVALUE;
			}
		}
		return null;
	}

	/**
	 * Return a unique list where the duplicates are removed.
	 */
	static final String _uniqHelp = "${uniq;<list> ...}";

	public String _uniq(String[] args) {
		verifyCommand(args, _uniqHelp, null, 1, Integer.MAX_VALUE);
		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.distinct()
			.collect(Strings.joining());
		return result;
	}

	/**
	 * Return the first list where items from the second list are removed.
	 */
	static final String _removeallHelp = "${removeall;<list>;<list>}";

	public String _removeall(String[] args) {
		verifyCommand(args, _removeallHelp, null, 1, 3);
		if (args.length < 2) {
			return "";
		}
		List<String> result = Strings.splitQuoted(args[1]);
		if (args.length > 2) {
			result.removeAll(Strings.splitQuoted(args[2]));
		}
		return Strings.join(result);
	}

	/**
	 * Return the first list where items not in the second list are removed.
	 */
	static final String _retainallHelp = "${retainall;<list>;<list>}";

	public String _retainall(String[] args) {
		verifyCommand(args, _retainallHelp, null, 1, 3);
		if (args.length < 3) {
			return "";
		}
		List<String> result = Strings.splitQuoted(args[1]);
		result.retainAll(Strings.splitQuoted(args[2]));
		return Strings.join(result);
	}

	public String _pathseparator(String[] args) {
		return File.pathSeparator;
	}

	public String _separator(String[] args) {
		return File.separator;
	}

	public String _filter(String[] args) {
		return filter(args, false);
	}

	public String _select(String[] args) {
		return filter(args, false);
	}

	public String _filterout(String[] args) {
		return filter(args, true);

	}

	public String _reject(String[] args) {
		return filter(args, true);

	}

	static final String _filterHelp = "${%s;<list>;<regex>}";

	String filter(String[] args, boolean include) {
		verifyCommand(args, String.format(_filterHelp, args[0]), null, 3, 3);

		Pattern pattern = Pattern.compile(args[2]);
		String result = Strings.splitQuotedAsStream(args[1])
			.filter(s -> pattern.matcher(s)
				.matches() != include)
			.collect(Strings.joining());
		return result;
	}

	static final String _sortHelp = "${sort;<list>...}";

	public String _sort(String[] args) {
		verifyCommand(args, _sortHelp, null, 2, Integer.MAX_VALUE);
		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.sorted()
			.collect(Strings.joining());
		return result;
	}

	static final String _nsortHelp = "${nsort;<list>...}";

	public String _nsort(String[] args) {
		verifyCommand(args, _nsortHelp, null, 2, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitAsStream)
			.sorted((a, b) -> {
				while (a.startsWith("0")) {
					a = a.substring(1);
				}
				while (b.startsWith("0")) {
					b = b.substring(1);
				}
				if (a.length() == b.length()) {
					return a.compareTo(b);
				}
				return (a.length() > b.length()) ? 1 : -1;
			})
			.collect(Strings.joining());
		return result;
	}

	static final String _joinHelp = "${join;<list>...}";

	public String _join(String[] args) {
		verifyCommand(args, _joinHelp, null, 1, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitAsStream)
			.collect(Strings.joining());
		return result;
	}

	static final String _sjoinHelp = "${sjoin;<separator>;<list>...}";

	public String _sjoin(String[] args) throws Exception {
		verifyCommand(args, _sjoinHelp, null, 2, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 2, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.collect(Collectors.joining(args[1]));
		return result;
	}

	static final String _ifHelp = "${if;<condition>;<iftrue> [;<iffalse>] } condition is either a filter expression or truthy";

	public String _if(String[] args) throws Exception {
		verifyCommand(args, _ifHelp, null, 2, 4);
		String condition = args[1];
		if (isTruthy(condition))
			return args.length > 2 ? args[2] : "true";

		if (args.length > 3)
			return args[3];
		return "";
	}

	public boolean isTruthy(String condition) throws Exception {
		if (condition == null)
			return false;

		condition = condition.trim();

		if (condition.startsWith("(") && condition.endsWith(")")) {
			return doCondition(condition);
		}

		return !condition.equalsIgnoreCase("false") && !condition.equals("0") && !condition.equals("0.0")
			&& condition.length() != 0;
	}

	public final static String _nowHelp = "${now;pattern|'long'}, returns current time";

	public Object _now(String[] args) {
		verifyCommand(args, _nowHelp, null, 1, 2);
		Date now = new Date();

		if (args.length == 2) {
			if ("long".equals(args[1]))
				return now.getTime();

			DateFormat df = new SimpleDateFormat(args[1], Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			return df.format(now);
		}
		return now;
	}

	public final static String _fmodifiedHelp = "${fmodified;<list of filenames>...}, return latest modification date";

	public String _fmodified(String[] args) throws Exception {
		verifyCommand(args, _fmodifiedHelp, null, 2, Integer.MAX_VALUE);

		long time = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.map(File::new)
			.filter(File::exists)
			.mapToLong(File::lastModified)
			.max()
			.orElse(0);
		return Long.toString(time);
	}

	public String _long2date(String[] args) {
		try {
			return new Date(Long.parseLong(args[1])).toString();
		} catch (Exception e) {
			return "not a valid long";
		}
	}

	public String _literal(String[] args) {
		if (args.length != 2)
			throw new RuntimeException("Need a value for the ${literal;<value>} macro");
		return "${" + args[1] + "}";
	}

	static final String _defHelp = "${def;<name>[;<value>]}, get the property or a default value if unset";

	public String _def(String[] args) {
		verifyCommand(args, _defHelp, null, 2, 3);

		return domain.getProperty(args[1], args.length == 3 ? args[2] : "");
	}

	static final String _listHelp = "${list;[<name>...]}, returns a list of the values of the named properties with escaped semicolons";

	public String _list(String[] args) {
		verifyCommand(args, _listHelp, null, 1, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.map(domain::getProperty)
			.flatMap(Strings::splitQuotedAsStream)
			.map(element -> (element.indexOf(';') < 0) ? element
				: SEMICOLON_P.matcher(element)
					.replaceAll(ESCAPED_SEMICOLON))
			.collect(Strings.joining());
		return result;
	}

	static final String _replaceHelp = "${replace;<list>;<regex>;[<replace>[;delimiter]]}";

	public String _replace(String[] args) {
		return replace0(_replaceHelp, Strings::splitAsStream, args);
	}

	static final String _replacelistHelp = "${replacelist;<list>;<regex>;[<replace>[;delimiter]]}";

	public String _replacelist(String[] args) {
		return replace0(_replacelistHelp, Strings::splitQuotedAsStream, args);
	}

	private String replace0(String help, Function<String, Stream<String>> splitter, String[] args) {
		verifyCommand(args, help, null, 3, 5);
		Pattern regex = Pattern.compile(args[2]);
		String replace = (args.length > 3) ? args[3] : "";
		Collector<CharSequence, ?, String> joining = (args.length > 4) ? Collectors.joining(args[4])
			: Strings.joining();

		String result = splitter.apply(args[1])
			.map(element -> regex.matcher(element)
				.replaceAll(replace))
			.collect(joining);
		return result;
	}

	static final String _replacestringHelp = "${replacesting;<target>;<regex>;[<replace>]}";

	public String _replacestring(String[] args) {
		verifyCommand(args, _replacestringHelp, null, 3, 4);
		Pattern regex = Pattern.compile(args[2]);
		String replace = (args.length > 3) ? args[3] : "";
		String result = regex.matcher(args[1])
			.replaceAll(replace);
		return result;
	}

	private static final Pattern	ANY			= Pattern.compile(".*");
	private static final Pattern	ERROR_P		= Pattern.compile("\\$\\{error;");
	private static final Pattern	WARNING_P	= Pattern.compile("\\$\\{warning;");

	public String _warning(String[] args) throws Exception {
		for (int i = 1; i < args.length; i++) {
			SetLocation warning = reporter.warning("%s", process(args[i]));
			FileLine header = domain.getHeader(ANY, WARNING_P);
			if (header != null)
				header.set(warning);
		}
		return "";
	}

	public String _error(String[] args) throws Exception {
		for (int i = 1; i < args.length; i++) {
			SetLocation error = reporter.error("%s", process(args[i]));
			FileLine header = domain.getHeader(ANY, ERROR_P);
			if (header != null)
				header.set(error);
		}
		return "";
	}

	/**
	 * toclassname ; <path>.class ( , <path>.class ) *
	 */
	static final String _toclassnameHelp = "${toclassname;<list of class paths>}, convert class paths to FQN class names ";

	public String _toclassname(String[] args) {
		verifyCommand(args, _toclassnameHelp, null, 2, 2);
		String result = Strings.splitAsStream(args[1])
			.map(path -> {
				if (path.endsWith(".class")) {
					return path.substring(0, path.length() - 6)
						.replace('/', '.');
				}
				if (path.endsWith(".java")) {
					return path.substring(0, path.length() - 5)
						.replace('/', '.');
				}
				reporter.warning("in toclassname, %s is not a class path because it does not end in .class", path);
				return null;
			})
			.filter(Objects::nonNull)
			.collect(Strings.joining());
		return result;
	}

	/**
	 * toclassname ; <path>.class ( , <path>.class ) *
	 */

	static final String _toclasspathHelp = "${toclasspath;<list>[;boolean]}, convert a list of class names to paths";

	public String _toclasspath(String[] args) {
		verifyCommand(args, _toclasspathHelp, null, 2, 3);
		boolean cl = (args.length > 2) ? Boolean.parseBoolean(args[2]) : true;
		Function<String, String> mapper = cl ? name -> name.replace('.', '/') + ".class"
			: name -> name.replace('.', '/');

		String result = Strings.splitAsStream(args[1])
			.map(mapper)
			.collect(Strings.joining());
		return result;
	}

	public String _dir(String[] args) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${dir;...}");
			return null;
		}
		String result = Arrays.stream(args, 1, args.length)
			.map(domain::getFile)
			.filter(File::exists)
			.map(File::getParentFile)
			.filter(File::exists)
			.map(IO::absolutePath)
			.collect(Strings.joining());
		return result;
	}

	public String _basename(String[] args) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${basename;...}");
			return null;
		}
		String result = Arrays.stream(args, 1, args.length)
			.map(domain::getFile)
			.filter(f -> f.exists() && f.getParentFile()
				.exists())
			.map(File::getName)
			.collect(Strings.joining());
		return result;
	}

	public String _isfile(String[] args) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${isfile;...}");
			return null;
		}
		boolean isfile = Arrays.stream(args, 1, args.length)
			.map(File::new)
			.map(File::getAbsoluteFile)
			.allMatch(File::isFile);
		return Boolean.toString(isfile);
	}

	public String _isdir(String[] args) {
		// if (args.length < 2) {
		// reporter.warning("Need at least one file name for ${isdir;...}");
		// return null;
		// }
		// If no dirs provided, return false
		boolean isdir = (args.length < 2) ? false
			: Arrays.stream(args, 1, args.length)
				.map(File::new)
				.map(File::getAbsoluteFile)
				.allMatch(File::isDirectory);
		return Boolean.toString(isdir);
	}

	public String _tstamp(String[] args) {
		String format;
		long now;
		TimeZone tz;

		if (args.length > 1) {
			format = args[1];
		} else {
			format = "yyyyMMddHHmm";
		}
		if (args.length > 2) {
			tz = TimeZone.getTimeZone(args[2]);
		} else {
			tz = TimeZone.getTimeZone("UTC");
		}
		if (args.length > 3) {
			now = Long.parseLong(args[3]);
		} else {
			String tstamp = domain.getProperty(Constants.TSTAMP);
			if (tstamp != null) {
				try {
					now = Long.parseLong(tstamp);
				} catch (NumberFormatException e) {
					// ignore, just use current time
					now = System.currentTimeMillis();
				}
			} else {
				now = System.currentTimeMillis();
			}
		}
		if (args.length > 4) {
			reporter.warning("Too many arguments for tstamp: %s", Arrays.toString(args));
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
		sdf.setTimeZone(tz);
		return sdf.format(new Date(now));
	}

	static final String _lsrHelp = "${lsr;<dir>;[<selector>...]}";

	public String _lsr(String[] args) {
		return ls(_lsrHelp, args, true);
	}

	static final String _lsaHelp = "${lsa;<dir>;[<selector>...]}";

	public String _lsa(String[] args) {
		return ls(_lsaHelp, args, false);
	}

	private String ls(String help, String[] args, boolean relative) {
		verifyCommand(args, help, null, 2, Integer.MAX_VALUE);

		File dir = domain.getFile(args[1]);
		if (!dir.isAbsolute())
			throw new IllegalArgumentException(
				String.format("the ${%s} macro directory parameter is not absolute: %s", args[0], dir));

		if (!dir.exists())
			throw new IllegalArgumentException(
				String.format("the ${%s} macro directory parameter does not exist: %s", args[0], dir));

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
				String.format("the ${%s} macro directory parameter points to a file instead of a directory: %s",
					args[0], dir));

		File[] array = dir.listFiles();
		if ((array == null) || (array.length == 0)) {
			return "";
		}
		Arrays.sort(array);
		Function<File, String> mapper = relative ? File::getName : IO::absolutePath;
		if (args.length < 3) {
			String result = Arrays.stream(array)
				.map(mapper)
				.collect(Strings.joining());
			return result;
		}
		List<File> files = new LinkedList<>();
		Collections.addAll(files, array);
		List<String> result = new ArrayList<>(array.length);
		Arrays.stream(args, 2, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.map(Instruction::new)
			.forEachOrdered(ins -> {
				for (Iterator<File> iter = files.iterator(); iter.hasNext();) {
					File file = iter.next();
					if (ins.matches(file.getPath())) {
						iter.remove();
						if (!ins.isNegated()) {
							result.add(mapper.apply(file));
						}
					}
				}
			});
		return Strings.join(result);
	}

	public String _currenttime(String[] args) {
		return Long.toString(System.currentTimeMillis());
	}

	/**
	 * Modify a version to set a version policy. The policy is a mask that is
	 * mapped to a version.
	 *
	 * <pre>
	 *  + increment - decrement = maintain s only
	 * pos=3 (qualifier). If qualifer == SNAPSHOT, return m.m.m-SNAPSHOT else
	 * m.m.m.q s only pos=3 (qualifier). If qualifer == SNAPSHOT, return
	 * m.m.m-SNAPSHOT else m.m.m &tilde; discard ==+ = maintain major, minor,
	 * increment micro, discard qualifier &tilde;&tilde;&tilde;= = just get the
	 * qualifier version=&quot;[${version;==;${&#x40;}},${version;=+;${&#x40;}})&quot;
	 * </pre>
	 */
	private final static String		MASK_M				= "[-+=~\\d]";
	private final static String		MASK_Q				= "[=~sS\\d]";
	private final static String		MASK_STRING			= MASK_M + "(?:" + MASK_M + "(?:" + MASK_M + "(?:" + MASK_Q
		+ ")?)?)?";
	private final static Pattern	VERSION_MASK		= Pattern.compile(MASK_STRING);
	final static String		_versionmaskHelp	= "${versionmask;<mask>;<version>}, modify a version\n"
		+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n" + "M ::= '+' | '-' | MQ\n" + "MQ ::= '~' | '='";
	final static String		_versionHelp		= _versionmaskHelp;
	final static Pattern[]			_versionPattern		= new Pattern[] {
		null, VERSION_MASK
	};

	public String _version(String[] args) {
		return _versionmask(args);
	}

	public String _versionmask(String[] args) {
		verifyCommand(args, _versionmaskHelp, _versionPattern, 2, 3);

		String mask = args[1];

		Version version = null;
		if (args.length >= 3) {
			if (isLocalTarget(args[2]))
				return LITERALVALUE;

			version = Version.parseVersion(args[2]);
		}

		return version(version, mask);
	}

	String version(Version version, String mask) {
		if (version == null) {
			String v = domain.getProperty(Constants.CURRENT_VERSION);
			if (v == null) {
				return LITERALVALUE;
			}
			version = new Version(v);
		}

		StringBuilder sb = new StringBuilder();
		String del = "";

		for (int i = 0; i < mask.length(); i++) {
			char c = mask.charAt(i);
			String result = null;
			if (c != '~') {
				if (i == 3) {
					result = version.getQualifier();
					MavenVersion mv = new MavenVersion(version);
					if (c == 'S') {
						// we have a request for a Maven snapshot
						if (mv.isSnapshot())
							return sb.append("-SNAPSHOT")
								.toString();
					} else if (c == 's') {
						// we have a request for a Maven snapshot
						if (mv.isSnapshot())
							return sb.append("-SNAPSHOT")
								.toString();
						else
							return sb.toString();
					}
				} else if (Character.isDigit(c)) {
					// Handle masks like +00, =+0
					result = String.valueOf(c);
				} else {
					int x = version.get(i);
					switch (c) {
						case '+' :
							x++;
							break;
						case '-' :
							x--;
							break;
						case '=' :
							break;
					}
					result = Integer.toString(x);
				}
				if (result != null) {
					sb.append(del);
					del = ".";
					sb.append(result);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Schortcut for version policy
	 *
	 * <pre>
	 *  -provide-policy : ${policy;[==,=+)}
	 * -consume-policy : ${policy;[==,+)}
	 * </pre>
	 */

	private static final Pattern	RANGE_MASK		= Pattern
		.compile("(\\[|\\()(" + MASK_STRING + "),(" + MASK_STRING + ")(\\]|\\))");
	static final String		_rangeHelp		= "${range;<mask>[;<version>]}, range for version, if version not specified lookup ${@}\n"
		+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n" + "M ::= '+' | '-' | MQ\n" + "MQ ::= '~' | '='";
	static final Pattern	_rangePattern[]	= new Pattern[] {
		null, RANGE_MASK
	};

	public String _range(String[] args) {
		verifyCommand(args, _rangeHelp, _rangePattern, 2, 3);
		Version version = null;
		if (args.length >= 3) {
			String string = args[2];
			if (isLocalTarget(string))
				return LITERALVALUE;

			version = new Version(string);
		} else {
			String v = domain.getProperty(Constants.CURRENT_VERSION);
			if (v == null)
				return LITERALVALUE;

			version = new Version(v);
		}
		String spec = args[1];

		Matcher m = RANGE_MASK.matcher(spec);
		m.matches();
		String floor = m.group(1);
		String floorMask = m.group(2);
		String ceilingMask = m.group(3);
		String ceiling = m.group(4);

		String left = version(version, floorMask);
		String right = version(version, ceilingMask);
		StringBuilder sb = new StringBuilder();
		sb.append(floor);
		sb.append(left);
		sb.append(",");
		sb.append(right);
		sb.append(ceiling);

		String s = sb.toString();
		VersionRange vr = new VersionRange(s);
		if (!(vr.includes(vr.getHigh()) || vr.includes(vr.getLow()))) {
			reporter.error("${range} macro created an invalid range %s from %s and mask %s", s, version, spec);
		}
		return sb.toString();
	}

	private static final String		LOCALTARGET_NAME	= "@[^${}\\[\\]()<>«»‹›]*";
	private static final Pattern	LOCALTARGET_P	= Pattern
		.compile("\\$(\\{" + LOCALTARGET_NAME + "\\}|\\[" + LOCALTARGET_NAME + "\\]|\\(" + LOCALTARGET_NAME + "\\)|<"
			+ LOCALTARGET_NAME + ">|«" + LOCALTARGET_NAME + "»|‹" + LOCALTARGET_NAME + "›)");
	boolean isLocalTarget(String string) {
		return LOCALTARGET_P.matcher(string)
			.matches();
	}

	/**
	 * System command. Execute a command and insert the result.
	 */
	public String system_internal(boolean allowFail, String[] args) throws Exception {
		if (nosystem)
			throw new RuntimeException("Macros in this mode cannot excute system commands");

		verifyCommand(args,
			"${" + (allowFail ? "system-allow-fail" : "system") + ";<command>[;<in>]}, execute a system command", null,
			2, 3);
		String command = args[1];
		String input = null;

		if (args.length > 2) {
			input = args[2];
		}

		return domain.system(allowFail, command, input);
	}

	public String _system(String[] args) throws Exception {
		return system_internal(false, args);
	}

	public String _system_allow_fail(String[] args) throws Exception {
		String result = "";
		try {
			result = system_internal(true, args);
			return result == null ? "" : result;
		} catch (Throwable t) {
			/* ignore */
			return "";
		}
	}

	static final String _envHelp = "${env;<name>[;alternative]}, get the environment variable";

	public String _env(String[] args) {
		verifyCommand(args, _envHelp, null, 2, 3);

		try {
			String ret = System.getenv(args[1]);
			if (ret != null)
				return ret;

			if (args.length > 2)
				return args[2];

		} catch (Throwable t) {
			// ignore
		}
		return "";
	}

	static final String _catHelp = "${cat;<in>}, get the content of a file";
	/**
	 * Get the contents of a file.
	 *
	 * @throws IOException
	 */

	public String _cat(String[] args) throws IOException {
		verifyCommand(args, _catHelp, null, 2, 2);
		File f = domain.getFile(args[1]);
		if (f.isFile()) {
			return IO.collect(f)
				.replaceAll("\\\\", "\\\\\\\\");
		} else if (f.isDirectory()) {
			return Arrays.toString(f.list());
		} else {
			try {
				URL url = new URL(args[1]);
				return IO.collect(url, UTF_8);
			} catch (MalformedURLException mfue) {
				// Ignore here
			}
			return null;
		}
	}

	static final String _base64Help = "${base64;<file>[;fileSizeLimit]}, get the Base64 encoding of a file";
	/**
	 * Get the Base64 encoding of a file.
	 *
	 * @throws IOException
	 */
	public String _base64(String... args) throws IOException {
		verifyCommand(args, _base64Help, null, 2, 3);

		File file = domain.getFile(args[1]);
		long maxLength = 100_000;
		if (args.length > 2)
			maxLength = Long.parseLong(args[2]);

		if (file.length() > maxLength)
			throw new IllegalArgumentException(
				"Maximum file size (" + maxLength + ") for base64 macro exceeded for file " + file);

		return Base64.encodeBase64(file);
	}

	static final String _digestHelp = "${digest;<algo>;<in>}, get a digest (e.g. MD5, SHA-256) of a file";
	/**
	 * Get a digest of a file.
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public String _digest(String... args) throws NoSuchAlgorithmException, IOException {
		verifyCommand(args, _digestHelp, null, 3, 3);

		MessageDigest digester = MessageDigest.getInstance(args[1]);
		File f = domain.getFile(args[2]);
		IO.copy(f, digester);
		byte[] digest = digester.digest();
		return Hex.toHexString(digest);
	}

	public static void verifyCommand(String[] args, String help, Pattern[] patterns, int low, int high) {
		String message = "";
		if (args.length > high) {
			message = "too many arguments";
		} else if (args.length < low) {
			message = "too few arguments";
		} else {
			for (int i = 0; patterns != null && i < patterns.length && i < args.length; i++) {
				if (patterns[i] != null) {
					Matcher m = patterns[i].matcher(args[i]);
					if (!m.matches())
						message += String.format("Argument %s (%s) does not match %s%n", i, args[i],
							patterns[i].pattern());
				}
			}
		}
		if (message.length() != 0) {
			StringBuilder sb = new StringBuilder();
			String del = "${";
			for (String arg : args) {
				sb.append(del);
				sb.append(arg);
				del = SEMICOLON;
			}
			sb.append("}, is not understood. ");
			sb.append(message);
			throw new IllegalArgumentException(sb.toString());
		}
	}

	// Helper class to track expansion of variables
	// on the stack.
	static class Link {
		final Link		previous;
		final String	key;
		final Processor	start;

		public Link(Processor start, Link previous, String key) {
			this.start = Objects.requireNonNull(start);
			this.previous = previous;
			this.key = key;
		}

		public boolean contains(String key) {
			if (this.key.equals(key))
				return true;

			if (previous == null)
				return false;

			return previous.contains(key);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			String del = "[";
			for (Link r = this; r != null; r = r.previous) {
				sb.append(del);
				sb.append(r.key);
				del = ",";
			}
			sb.append("]");
			return sb.toString();
		}
	}

	/**
	 * Take all the properties and translate them to actual values. This method
	 * takes the set properties and traverse them over all entries, including
	 * the default properties for that properties. The values no longer contain
	 * macros.
	 * <p>
	 * There are some rules
	 * <ul>
	 * <li>Property names starting with an underscore ('_') are ignored. These
	 * are reserved for properties that cause an unwanted side effect when
	 * expanded unnecessary
	 * <li>Property names starting with a minus sign ('-') are not expanded to
	 * maintain readability
	 * </ul>
	 *
	 * @return A new Properties with the flattened values
	 */
	public Properties getFlattenedProperties() {
		return getFlattenedProperties(true);
	}

	/**
	 * Take all the properties and translate them to actual values. This method
	 * takes the set properties and traverse them over all entries, including
	 * the default properties for that properties. The values no longer contain
	 * macros.
	 * <p>
	 * Property names starting with an underscore ('_') are ignored. These are
	 * reserved for properties that cause an unwanted side effect when expanded
	 * unnecessary
	 *
	 * @return A new Properties with the flattened values
	 */
	public Properties getFlattenedProperties(boolean ignoreInstructions) {
		// Some macros only work in a lower processor, so we
		// do not report unknown macros while flattening
		flattening = true;
		try {
			Properties flattened = new UTF8Properties();
			Properties source = domain.getProperties();
			for (String key : Iterables.iterable(source.propertyNames(), String.class::cast)) {
				if (!key.startsWith("_")) {
					String value = source.getProperty(key);
					if (value == null) {
						Object raw = source.get(key);
						reporter.warning("Key '%s' has a non-String value: %s:%s", key, raw == null ? ""
							: raw.getClass()
								.getName(),
							raw);
					} else {
						if (ignoreInstructions && key.startsWith("-"))
							flattened.put(key, value);
						else
							flattened.put(key, process(value));
					}
				}
			}
			return flattened;
		} finally {
			flattening = false;
		}
	}

	public final static String _fileHelp = "${file;<base>;<paths>...}, create correct OS dependent path";

	static final String			_osfileHelp	= "${osfile;<base>;<path>}, create correct OS dependent path";

	public String _osfile(String[] args) {
		verifyCommand(args, _osfileHelp, null, 3, 3);
		File base = new File(args[1]);
		File f = IO.getFile(base, args[2]);
		return IO.absolutePath(f);
	}

	public String _path(String[] args) {
		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.collect(Collectors.joining(File.pathSeparator));
		return result;
	}

	public final static String _sizeHelp = "${size;<collection>;...}, count the number of elements (of all collections combined)";

	public int _size(String[] args) {
		verifyCommand(args, _sizeHelp, null, 1, Integer.MAX_VALUE);
		long size = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.count();
		return (int) size;
	}

	public static Properties getParent(Properties p) {
		try {
			Field f = Properties.class.getDeclaredField("defaults");
			f.setAccessible(true);
			MethodHandle mh = publicLookup().unreflectGetter(f);
			return (Properties) mh.invoke(p);
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			return null;
		}
	}

	public String process(String line) {
		return process(line, domain);
	}

	public boolean isNosystem() {
		return nosystem;
	}

	public boolean setNosystem(boolean nosystem) {
		boolean tmp = this.nosystem;
		this.nosystem = nosystem;
		return tmp;
	}

	public String _unescape(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			sb.append(args[i]);
		}

		for (int j = 0; j < sb.length() - 1; j++) {
			if (sb.charAt(j) == '\\') {
				switch (sb.charAt(j + 1)) {

					case 'n' :
						sb.replace(j, j + 2, "\n");
						break;

					case 'r' :
						sb.replace(j, j + 2, "\r");
						break;

					case 'b' :
						sb.replace(j, j + 2, "\b");
						break;

					case 'f' :
						sb.replace(j, j + 2, "\f");
						break;

					case 't' :
						sb.replace(j, j + 2, "\t");
						break;

					default :
						break;
				}
			}
		}
		return sb.toString();
	}

	static final String _startswithHelp = "${startswith;<string>;<prefix>}";

	public String _startswith(String[] args) throws Exception {
		verifyCommand(args, _startswithHelp, null, 3, 3);
		if (args[1].startsWith(args[2]))
			return args[1];
		else
			return "";
	}

	static final String _endswithHelp = "${endswith;<string>;<suffix>}";

	public String _endswith(String[] args) throws Exception {
		verifyCommand(args, _endswithHelp, null, 3, 3);
		if (args[1].endsWith(args[2]))
			return args[1];
		else
			return "";
	}

	static final String _extensionHelp = "${extension;<string>}";

	public String _extension(String[] args) throws Exception {
		verifyCommand(args, _extensionHelp, null, 2, 2);
		String name = args[1];
		int n = name.indexOf('.');
		if (n < 0)
			return "";
		return name.substring(n + 1);
	}

	static final String _stemHelp = "${stem;<string>}";

	public String _stem(String[] args) throws Exception {
		verifyCommand(args, _stemHelp, null, 2, 2);
		String name = args[1];
		int n = name.indexOf('.');
		if (n < 0)
			return name;
		return name.substring(0, n);
	}

	static final String _substringHelp = "${substring;<string>;<start>[;<end>]}";

	public String _substring(String[] args) throws Exception {
		verifyCommand(args, _substringHelp, null, 3, 4);

		String string = args[1];
		int start = Integer.parseInt(args[2].equals("") ? "0" : args[2]);
		int end = string.length();

		if (args.length > 3) {
			end = Integer.parseInt(args[3]);
			if (end < 0)
				end = string.length() + end;
		}

		if (start < 0)
			start = string.length() + start;

		if (start > end) {
			int t = start;
			start = end;
			end = t;
		}

		return string.substring(start, end);
	}

	static final String	_randHelp	= "${rand;[<min>[;<end>]]}";
	static final Random	random		= new Random();

	public long _rand(String[] args) throws Exception {
		verifyCommand(args, _randHelp, null, 2, 3);

		int min = 0;
		int max = 100;
		if (args.length > 1) {
			max = Integer.parseInt(args[1]);
			if (args.length > 2) {
				min = Integer.parseInt(args[2]);
			}
		}
		int diff = max - min;

		double d = random.nextDouble() * diff + min;
		return Math.round(d);
	}

	static final String _lengthHelp = "${length;<string>}";

	public int _length(String[] args) throws Exception {
		verifyCommand(args, _lengthHelp, null, 1, 2);
		if (args.length == 1)
			return 0;

		return args[1].length();
	}

	static final String _getHelp = "${get;<index>;<list>}";

	public String _get(String[] args) throws Exception {
		verifyCommand(args, _getHelp, null, 3, 3);

		int index = Integer.parseInt(args[1]);
		List<String> list = toList(args, 2, args.length);
		if (index < 0)
			index = list.size() + index;
		return list.get(index);
	}

	static final String _sublistHelp = "${sublist;<start>;<end>[;<list>...]}";

	public String _sublist(String[] args) throws Exception {
		verifyCommand(args, _sublistHelp, null, 4, Integer.MAX_VALUE);

		int start = Integer.parseInt(args[1]);
		int end = Integer.parseInt(args[2]);
		List<String> list = toList(args, 3, args.length);

		if (start < 0)
			start = list.size() + start + 1;

		if (end < 0)
			end = list.size() + end + 1;

		if (start > end) {
			int t = start;
			start = end;
			end = t;
		}

		return Strings.join(list.subList(start, end));
	}

	private List<String> toList(String[] args, int startInclusive, int endExclusive) {
		List<String> list = Arrays.stream(args, startInclusive, endExclusive)
			.flatMap(Strings::splitQuotedAsStream)
			.collect(Collectors.toList());
		return list;
	}

	static final String _firstHelp = "${first;<list>[;<list>...]}";

	public String _first(String[] args) throws Exception {
		verifyCommand(args, _firstHelp, null, 1, Integer.MAX_VALUE);
		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.findFirst()
			.orElse("");
		return result;
	}

	static final String _lastHelp = "${last;<list>[;<list>...]}";

	public String _last(String[] args) throws Exception {
		verifyCommand(args, _lastHelp, null, 1, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.reduce((first, second) -> second)
			.orElse("");
		return result;
	}

	static final String _maxHelp = "${max;<list>[;<list>...]}";

	public String _max(String[] args) throws Exception {
		verifyCommand(args, _maxHelp, null, 2, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.max(String::compareTo)
			.orElse("");
		return result;
	}

	static final String _minHelp = "${min;<list>[;<list>...]}";

	public String _min(String[] args) throws Exception {
		verifyCommand(args, _minHelp, null, 2, Integer.MAX_VALUE);

		String result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.min(String::compareTo)
			.orElse("");
		return result;
	}

	static final String _nmaxHelp = "${nmax;<list>[;<list>...]}";

	public String _nmax(String[] args) throws Exception {
		verifyCommand(args, _nmaxHelp, null, 2, Integer.MAX_VALUE);

		double result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitAsStream)
			.mapToDouble(Double::parseDouble)
			.max()
			.orElse(Double.NaN);
		return toString(result);
	}

	static final String _nminHelp = "${nmin;<list>[;<list>...]}";

	public String _nmin(String[] args) throws Exception {
		verifyCommand(args, _nminHelp, null, 2, Integer.MAX_VALUE);

		double result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitAsStream)
			.mapToDouble(Double::parseDouble)
			.min()
			.orElse(Double.NaN);
		return toString(result);
	}

	static final String _sumHelp = "${sum;<list>[;<list>...]}";

	public String _sum(String[] args) throws Exception {
		verifyCommand(args, _sumHelp, null, 2, Integer.MAX_VALUE);

		double result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitAsStream)
			.mapToDouble(Double::parseDouble)
			.sum();
		return toString(result);
	}

	static final String _averageHelp = "${average;<list>[;<list>...]}";

	public String _average(String[] args) throws Exception {
		verifyCommand(args, _sumHelp, null, 2, Integer.MAX_VALUE);

		double result = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.mapToDouble(Double::parseDouble)
			.average()
			.orElseThrow(() -> new IllegalArgumentException("No members in list to calculate average"));
		return toString(result);
	}

	static final String _reverseHelp = "${reverse;<list>[;<list>...]}";

	public String _reverse(String[] args) throws Exception {
		verifyCommand(args, _reverseHelp, null, 2, Integer.MAX_VALUE);

		Deque<String> reversed = Arrays.stream(args, 1, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.collect(Collector.of(ArrayDeque::new, (deq, t) -> deq.addFirst(t), (d1, d2) -> {
				d2.addAll(d1);
				return d2;
			}));
		return Strings.join(reversed);
	}

	static final String _indexofHelp = "${indexof;<value>;<list>[;<list>...]}";

	public int _indexof(String[] args) throws Exception {
		verifyCommand(args, _indexofHelp, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		List<String> list = toList(args, 2, args.length);
		return list.indexOf(value);
	}

	static final String _lastindexofHelp = "${lastindexof;<value>;<list>[;<list>...]}";

	public int _lastindexof(String[] args) throws Exception {
		verifyCommand(args, _lastindexofHelp, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		List<String> list = toList(args, 2, args.length);
		return list.lastIndexOf(value);
	}

	static final String _findHelp = "${find;<target>;<searched>}";

	public int _find(String[] args) throws Exception {
		verifyCommand(args, _findHelp, null, 3, 3);

		return args[1].indexOf(args[2]);
	}

	static final String _findlastHelp = "${findlast;<find>;<target>}";

	public int _findlast(String[] args) throws Exception {
		verifyCommand(args, _findlastHelp, null, 3, 3);

		return args[2].lastIndexOf(args[1]);
	}

	static final String _splitHelp = "${split;<regex>[;<target>...]}";

	public String _split(String[] args) throws Exception {
		verifyCommand(args, _splitHelp, null, 2, Integer.MAX_VALUE);

		Pattern regex = Pattern.compile(args[1]);
		String result = Arrays.stream(args, 2, args.length)
			.flatMap(regex::splitAsStream)
			.filter(element -> !element.isEmpty())
			.collect(Strings.joining());
		return result;
	}

	static final String _jsHelp = "${js [;<js expr>...]}";

	public Object _js(String[] args) throws Exception {
		verifyCommand(args, _jsHelp, null, 2, Integer.MAX_VALUE);

		StringBuilder sb = new StringBuilder();

		for (int i = 1; i < args.length; i++)
			sb.append(args[i])
				.append(';');

		if (context == null) {
			synchronized (this) {
				if (engine == null)
					engine = new ScriptEngineManager().getEngineByName("javascript");
			}
			context = engine.getContext();
			bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("domain", domain);
			String javascript = domain.mergeProperties("javascript", ";");
			if (javascript != null && javascript.length() > 0) {
				engine.eval(javascript, context);
			}
			context.setErrorWriter(stderr);
			context.setWriter(stdout);
		}
		Object eval = engine.eval(sb.toString(), context);
		StringBuffer buffer = stdout.getBuffer();
		if (buffer.length() > 0) {
			reporter.error("Executing js: %s: %s", sb, buffer);
			buffer.setLength(0);
		}

		if (eval != null) {
			return toString(eval);
		}

		String out = stdout.toString();
		stdout.getBuffer()
			.setLength(0);
		return out;
	}

	private String toString(Object eval) {
		if (eval == null)
			return "null";

		if (eval instanceof Double || eval instanceof Float) {
			String v = eval.toString();
			return v.endsWith(".0") ? v.substring(0, v.length() - 2) : v;
		}
		return eval.toString();
	}

	private String toString(double eval) {
		String v = Double.toString(eval);
		return v.endsWith(".0") ? v.substring(0, v.length() - 2) : v;
	}

	static final String _toupperHelp = "${toupper;<target>}";

	public String _toupper(String[] args) throws Exception {
		verifyCommand(args, _tolowerHelp, null, 2, 2);

		return args[1].toUpperCase();
	}

	static final String _tolowerHelp = "${tolower;<target>}";

	public String _tolower(String[] args) throws Exception {
		verifyCommand(args, _tolowerHelp, null, 2, 2);

		return args[1].toLowerCase();
	}

	static final String _compareHelp = "${compare;<astring>;<bstring>}";

	public int _compare(String[] args) throws Exception {
		verifyCommand(args, _compareHelp, null, 3, 3);
		int n = args[1].compareTo(args[2]);
		return Integer.signum(n);
	}

	static final String _ncompareHelp = "${ncompare;<anumber>;<bnumber>}";

	public int _ncompare(String[] args) throws Exception {
		verifyCommand(args, _ncompareHelp, null, 3, 3);
		double a = Double.parseDouble(args[1]);
		double b = Double.parseDouble(args[2]);
		return Integer.signum(Double.compare(a, b));
	}

	static final String _matchesHelp = "${matches;<target>;<regex>}";

	public boolean _matches(String[] args) throws Exception {
		verifyCommand(args, _matchesHelp, null, 3, 3);

		return args[1].matches(args[2]);
	}

	static final String _substHelp = "${subst;<target>;<regex>[;<replace>[;count]]}";

	public StringBuffer _subst(String[] args) throws Exception {
		verifyCommand(args, _substHelp, null, 3, 5);

		Pattern p = Pattern.compile(args[2]);
		Matcher matcher = p.matcher(args[1]);
		String replace = (args.length > 3) ? args[3] : "";
		int count = (args.length > 4) ? Integer.parseInt(args[4]) : Integer.MAX_VALUE;

		StringBuffer sb = new StringBuffer();
		for (int i = 0; (i < count) && matcher.find(); i++) {
			matcher.appendReplacement(sb, replace);
		}
		matcher.appendTail(sb);
		return sb;
	}

	static final String _trimHelp = "${trim;<target>}";

	public String _trim(String[] args) throws Exception {
		verifyCommand(args, _trimHelp, null, 2, 2);

		return args[1].trim();
	}

	static final String _formatHelp = "${format;<format>[;args...]}";

	public String _format(String[] args) throws Exception {
		verifyCommand(args, _formatHelp, null, 2, Integer.MAX_VALUE);

		Object[] args2 = new Object[args.length + 10];

		Matcher m = PRINTF_P.matcher(args[1]);
		int n = 2;
		while (n < args.length && m.find()) {
			char conversion = m.group(5)
				.charAt(0);
			switch (conversion) {
				// d|f|c|s|h|n|x|X|u|o|z|Z|e|E|g|G|p|\n|%)");
				case 'd' :
				case 'u' :
				case 'o' :
				case 'x' :
				case 'X' :
				case 'z' :
				case 'Z' :
					args2[n - 2] = Long.parseLong(args[n]);
					n++;
					break;

				case 'f' :
				case 'e' :
				case 'E' :
				case 'g' :
				case 'G' :
				case 'a' :
				case 'A' :
					args2[n - 2] = Double.parseDouble(args[n]);
					n++;
					break;

				case 'c' :
					if (args[n].length() != 1)
						throw new IllegalArgumentException("Character expected but found '" + args[n] + "'");
					args2[n - 2] = args[n].charAt(0);
					n++;
					break;

				case 'b' :
					String v = args[n].toLowerCase();
					if (v == null || v.equals("false") || v.isEmpty() || (NUMERIC_P.matcher(v)
						.matches() && Double.parseDouble(v) == 0.0D))
						args2[n - 2] = false;
					else
						args2[n - 2] = false;
					n++;
					break;

				case 's' :
				case 'h' :
				case 'H' :
				case 'p' :
					args2[n - 2] = args[n];
					n++;
					break;

				case 't' :
				case 'T' :
					String dt = args[n];

					if (NUMERIC_P.matcher(dt)
						.matches()) {
						args2[n - 2] = Long.parseLong(dt);
					} else {
						DateFormat df;
						switch (args[n].length()) {
							case 6 :
								df = new SimpleDateFormat("yyMMdd", Locale.US);
								break;

							case 8 :
								df = new SimpleDateFormat("yyyyMMdd", Locale.US);
								break;

							case 12 :
								df = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
								break;

							case 14 :
								df = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
								break;
							case 19 :
								df = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ", Locale.US);
								break;

							default :
								throw new IllegalArgumentException("Unknown dateformat " + args[n]);
						}
						df.setTimeZone(TimeZone.getTimeZone("UTC"));
						args2[n - 2] = df.parse(args[n]);
					}
					break;

				case 'n' :
				case '%' :
					break;
			}
		}

		try (Formatter f = new Formatter()) {
			f.format(args[1], args2);
			return f.toString();
		}
	}

	static final String _isemptyHelp = "${isempty;[<target>...]}";

	public boolean _isempty(String[] args) throws Exception {
		verifyCommand(args, _isemptyHelp, null, 1, Integer.MAX_VALUE);

		boolean result = Arrays.stream(args, 1, args.length)
			.noneMatch(s -> !s.trim()
				.isEmpty());
		return result;
	}

	static final String _isnumberHelp = "${isnumber;<target>[;<target>...]}";

	public boolean _isnumber(String[] args) throws Exception {
		verifyCommand(args, _isnumberHelp, null, 2, Integer.MAX_VALUE);

		boolean result = Arrays.stream(args, 1, args.length)
			.allMatch(s -> NUMERIC_P.matcher(s)
				.matches());
		return result;
	}

	static final String _isHelp = "${is;<a>;<b>}";

	public boolean _is(String[] args) throws Exception {
		verifyCommand(args, _isHelp, null, 3, Integer.MAX_VALUE);
		String a = args[1];

		boolean result = Arrays.stream(args, 2, args.length)
			.allMatch(a::equals);
		return result;
	}

	/**
	 * Map a value from a list to a new value
	 */

	static final String _mapHelp = "${map;<macro>[;<list>...]}";

	public String _map(String[] args) throws Exception {
		verifyCommand(args, _mapHelp, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		String result = Arrays.stream(args, 2, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.map(s -> process("${" + macro + SEMICOLON + s + "}"))
			.collect(Strings.joining());
		return result;
	}

	/**
	 * Map a value from a list to a new value, providing the value and the index
	 */

	static final String _foreachHelp = "${foreach;<macro>[;<list>...]}";

	public String _foreach(String[] args) throws Exception {
		verifyCommand(args, _foreachHelp, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args, 2, args.length);
		String result = IntStream.range(0, list.size())
			.mapToObj(n -> process("${" + macro + SEMICOLON + list.get(n) + SEMICOLON + n + "}"))
			.collect(Strings.joining());
		return result;
	}

	/**
	 * Take a list and convert this to the argumets
	 */

	static final String _applyHelp = "${apply;<macro>[;<list>...]}";

	public String _apply(String[] args) throws Exception {
		verifyCommand(args, _applyHelp, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		String result = Arrays.stream(args, 2, args.length)
			.flatMap(Strings::splitQuotedAsStream)
			.collect(Collectors.joining(SEMICOLON, "${" + macro + SEMICOLON, "}"));
		return process(result);
	}

	/**
	 * Format bytes
	 */
	public String _bytes(String[] args) {
		try (Formatter sb = new Formatter()) {
			for (int i = 0; i < args.length; i++) {
				long l = Long.parseLong(args[1]);
				bytes(sb, l, 0, new String[] {
					"b", "Kb", "Mb", "Gb", "Tb", "Pb", "Eb", "Zb", "Yb", "Bb", "Geopbyte"
				});
			}
			return sb.toString();
		}
	}

	private void bytes(Formatter sb, double l, int i, String[] strings) {
		if (l > 1024 && i < strings.length - 1) {
			bytes(sb, l / 1024, i + 1, strings);
			return;
		}
		l = Math.round(l * 10) / 10;
		sb.format("%s %s", l, strings[i]);
	}

	static final String _globHelp = "${glob;<globexp>} (turn it into a regular expression)";

	public String _glob(String[] args) {
		verifyCommand(args, _globHelp, null, 2, 2);
		String glob = args[1];
		boolean negate = false;
		if (glob.startsWith("!")) {
			glob = glob.substring(1);
			negate = true;
		}

		Pattern pattern = Glob.toPattern(glob);
		if (negate)
			return "(?!" + pattern.pattern() + ")";
		else
			return pattern.pattern();
	}

	public boolean doCondition(String arg) throws Exception {
		ExtendedFilter f = new ExtendedFilter(arg);
		return f.match(new Get() {

			@Override
			public Object get(String key) throws Exception {
				if (key.endsWith("[]")) {
					key = key.substring(0, key.length() - 2);
					return Strings.split(domain.getProperty(key));
				} else
					return domain.getProperty(key);
			}
		});
	}

	public Map<String, String> getCommands() {
		Set<Object> targets = new LinkedHashSet<>();
		targets.addAll(Arrays.asList(targets));
		Processor rover = domain;
		while (rover != null) {
			targets.add(rover);
			rover = rover.getParent();
		}
		targets.add(this);

		return targets.stream()
			.map(Object::getClass)
			.map(Class::getMethods)
			.flatMap(Arrays::stream)
			.filter(m -> !Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) && m.getName()
				.startsWith("_"))
			.collect(toMap(m -> m.getName()
				.substring(1), m -> {
					try {
						Field f = m.getDeclaringClass()
							.getDeclaredField(m.getName() + "Help");
						f.setAccessible(true);
						MethodHandle mh = publicLookup().unreflectGetter(f);
						return (String) mh.invoke();
					} catch (Error e) {
						throw e;
					} catch (Throwable e) {
						return "";
					}
				}, (u, v) -> u, TreeMap::new));
	}

}
