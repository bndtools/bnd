package aQute.libg.sed;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.collections.ExtList;
import aQute.lib.collections.SortedList;
import aQute.lib.date.Dates;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

/**
 * Provide a macro Domain. This Domain can replace variables in strings based on
 * a properties and a domain. The domain can implement functions that start with
 * a "_" and take args[], the names of these functions are available as
 * functions in the macro Domain (without the _). Macros can nest to any depth
 * but may not contain loops. Add POSIX macros: ${#parameter} String length.
 * ${parameter%word} Remove smallest suffix pattern. ${parameter%%word} Remove
 * largest suffix pattern. ${parameter#word} Remove smallest prefix pattern.
 * ${parameter##word} Remove largest prefix pattern.
 */
public class ReplacerAdapter extends ReporterAdapter implements Replacer {
	static final Random				random		= new Random();
	private final static Pattern	WILDCARD	= Pattern.compile("[*?|({\\[]");
	Domain							domain;
	List<Object>					targets		= new ArrayList<>();
	boolean							flattening;
	File							base		= new File(System.getProperty("user.dir"));
	Reporter						reporter	= this;

	public ReplacerAdapter(Domain domain) {
		this.domain = domain;
	}

	public ReplacerAdapter(final Map<String, String> domain) {
		this(new Domain() {

			@Override
			public Map<String, String> getMap() {
				return domain;
			}

			@Override
			public Domain getParent() {
				return null;
			}

		});
	}

	public ReplacerAdapter target(Object target) {
		assert target != null;
		targets.add(target);
		return this;
	}

	public ReplacerAdapter target(File base) {
		this.base = base;
		return this;
	}

	public String process(String line, Domain source) {
		return process(line, new Link(source, null, line));
	}

	String process(String line, Link link) {
		StringBuilder sb = new StringBuilder();
		process(line, 0, '\u0000', '\u0000', sb, link);
		return sb.toString();
	}

	int process(CharSequence org, int index, char begin, char end, StringBuilder result, Link link) {
		StringBuilder line = new StringBuilder(org);
		int nesting = 1;

		StringBuilder variable = new StringBuilder();
		outer: while (index < line.length()) {
			char c1 = line.charAt(index++);
			if (c1 == end) {
				if (--nesting == 0) {
					result.append(replace(variable.toString(), link));
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
					variable.append(base.getAbsolutePath());
					variable.append('/');
					continue outer;
				}
			}
			variable.append(c1);
		}
		result.append(variable);
		return index;
	}

	/**
	 * Traverses a string to find a macro. It can handle nested brackets.
	 *
	 * @param line The line with the macro
	 * @param index Points to the character after the '$'
	 * @return the end position
	 */
	public int findMacro(CharSequence line, int index) {
		if (index >= line.length() || line.charAt(index) != '$')
			return -1;

		index++;

		int nesting = 1;
		char begin = line.charAt(index++);
		char end = getTerminator(begin);
		if (end == 0)
			return -1;

		while (index < line.length()) {
			char c1 = line.charAt(index++);
			if (c1 == end) {
				if (--nesting == 0) {
					return index;
				}
			} else if (c1 == begin)
				nesting++;
			else if (c1 == '\\' && index < line.length() - 1) {
				index++;
			} else if (c1 == '\'' || c1 == '"') {
				string: while (index < line.length()) {
					char c2 = line.charAt(index++);
					switch (c2) {
						case '"' :
						case '\'' :
							if (c2 == c1)
								break string;
							break;

						case '\\' :
							index++;
							break;
					}
				}
			}
		}
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

	public String getProcessed(String key) {
		return replace(key, null);
	}

	protected String replace(String key, Link link) {
		if (link != null && link.contains(key))
			return "${infinite:" + link + "}";

		if (key != null) {
			key = key.trim();
			if (key.length() > 0) {
				Domain source = domain;
				String value = null;
				if (key.indexOf(';') < 0) {
					if (WILDCARD.matcher(key)
						.find()) {
						Glob ins = new Glob(key);
						StringBuilder sb = new StringBuilder();
						String del = "";
						for (String k : getAllKeys()) {
							if (ins.matcher(k)
								.find()) {
								String v = replace(k, new Link(source, link, key));
								if (v != null) {
									sb.append(del);
									del = ",";
									sb.append(v);
								}
							}
						}
						return sb.toString();
					}

					while (value == null && source != null) {
						value = source.getMap()
							.get(key);
						if (value != null)
							return process(value, new Link(source, link, key));

						source = source.getParent();
					}
				}
				value = doCommands(key, link);
				if (value != null)
					return process(value, new Link(source, link, key));

				if (key != null && key.trim()
					.length() > 0) {
					value = System.getProperty(key);
					if (value != null)
						return value;
				}
				if (key.indexOf(';') >= 0) {
					String parts[] = key.split(";");
					if (parts.length > 1) {
						if (parts.length >= 16) {
							error("too many arguments for template: %s, max is 16", key);
						}

						String template = domain.getMap()
							.get(parts[0]);
						if (template != null) {
							final Domain old = domain;
							try {
								final Map<String, String> args = new HashMap<>();
								for (int i = 0; i < 16; i++) {
									args.put("" + i, i < parts.length ? parts[i] : "null");
								}
								domain = new Domain() {

									@Override
									public Map<String, String> getMap() {
										return args;
									}

									@Override
									public Domain getParent() {
										return old;
									}

								};
								ExtList<String> args0 = new ExtList<>(parts);
								args0.remove(0);
								args.put("#", args0.join());

								value = process(template, new Link(domain, link, key));
								if (value != null)
									return value;
							} finally {
								domain = old;
							}
						}
					}
				}
				if (!flattening && !key.equals("@"))
					reporter.warning("No translation found for macro: %s", key);
			} else {
				reporter.warning("Found empty macro key");
			}
		} else {
			reporter.warning("Found null macro key");
		}
		return "${" + key + "}";
	}

	private List<String> getAllKeys() {
		List<String> l = new ArrayList<>();
		Domain source = domain;
		do {
			l.addAll(source.getMap()
				.keySet());
			source = source.getParent();
		} while (source != null);

		Collections.sort(l);
		return l;
	}

	/**
	 * Parse the key as a command. A command consist of parameters separated by
	 * ':'.
	 */
	private final static Pattern commands = Pattern.compile("(?<!\\\\);");

	private String doCommands(String key, Link source) {
		String[] args = commands.split(key);
		if (args == null || args.length == 0)
			return null;

		for (int i = 0; i < args.length; i++)
			if (args[i].indexOf('\\') >= 0)
				args[i] = args[i].replaceAll("\\\\;", ";");

		if (args[0].startsWith("^")) {
			String varname = args[0].substring(1)
				.trim();

			Domain parent = source.start.getParent();
			if (parent != null)
				return parent.getMap()
					.get(varname);
			return null;
		}

		Domain rover = domain;
		while (rover != null) {
			String result = doCommand(rover, args[0], args);
			if (result != null)
				return result;

			rover = rover.getParent();
		}

		for (Object target : targets) {
			String result = doCommand(target, args[0], args);
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
			String cname = "_" + method.replaceAll("-", "_");
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
				reporter.warning("Exception in replace: %s method=%s", e, method);
				e.printStackTrace();
				return null;
			}
			try {
				Object result = Modifier.isStatic(m.getModifiers()) ? mh.invoke(args) : mh.invoke(target, args);
				return "" + result;
			} catch (Error e) {
				throw e;
			} catch (WrongMethodTypeException e) {
				reporter.warning("Exception in replace: %s method=%s", e, method);
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				reporter.error("%s, for cmd: %s, arguments; %s", e, method, Arrays.toString(args));
			} catch (Throwable e) {
				reporter.warning("Exception in replace: %s method=%s", e, method);
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Return a unique list where the duplicates are removed.
	 */
	static String _uniqHelp = "${uniq;<list> ...}";

	public String _uniq(String args[]) {
		verifyCommand(args, _uniqHelp, null, 1, Integer.MAX_VALUE);
		Set<String> set = new LinkedHashSet<>();
		for (int i = 1; i < args.length; i++) {
			set.addAll(ExtList.from(args[i].trim()));
		}
		ExtList<String> rsult = new ExtList<>();
		rsult.addAll(set);
		return rsult.join(",");
	}

	public String _pathseparator(String args[]) {
		return File.pathSeparator;
	}

	public String _separator(String args[]) {
		return File.separator;
	}

	public String _filter(String args[]) {
		return filter(args, false);
	}

	public String _filterout(String args[]) {
		return filter(args, true);

	}

	static String _filterHelp = "${%s;<list>;<regex>}";

	String filter(String[] args, boolean include) {
		verifyCommand(args, String.format(_filterHelp, args[0]), null, 3, 3);

		ExtList<String> list = ExtList.from(args[1]);
		Pattern pattern = Pattern.compile(args[2]);

		list.removeIf(s -> pattern.matcher(s)
			.matches() == include);
		return list.join();
	}

	static String _sortHelp = "${sort;<list>...}";

	public String _sort(String args[]) {
		verifyCommand(args, _sortHelp, null, 2, Integer.MAX_VALUE);

		ExtList<String> result = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			result.addAll(ExtList.from(args[i]));
		}
		Collections.sort(result);
		return result.join();
	}

	static String _nsortHelp = "${nsort;<list>...}";

	public String _nsort(String args[]) {
		verifyCommand(args, _nsortHelp, null, 2, Integer.MAX_VALUE);

		ExtList<String> result = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			result.addAll(ExtList.from(args[i]));
		}
		Collections.sort(result, (a, b) -> {
			while (a.startsWith("0"))
				a = a.substring(1);

			while (b.startsWith("0"))
				b = b.substring(1);

			if (a.length() == b.length())
				return a.compareTo(b);
			else if (a.length() > b.length())
				return 1;
			else
				return -1;

		});
		return result.join();
	}

	static String _joinHelp = "${join;<list>...}";

	public String _join(String args[]) {

		verifyCommand(args, _joinHelp, null, 1, Integer.MAX_VALUE);

		ExtList<String> result = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			result.addAll(ExtList.from(args[i]));
		}
		return result.join();
	}

	static String _ifHelp = "${if;<condition>;<iftrue> [;<iffalse>] }";

	public String _if(String args[]) {
		verifyCommand(args, _ifHelp, null, 3, 4);
		String condition = args[1].trim();
		if (!condition.equalsIgnoreCase("false"))
			if (condition.length() != 0)
				return args[2];

		if (args.length > 3)
			return args[3];
		return "";
	}

	private static final DateTimeFormatter DATE_TOSTRING = Dates.DATE_TOSTRING.withZone(Dates.UTC_ZONE_ID);

	public String _now(String args[]) {
		long now = System.currentTimeMillis();
		if (args.length == 2) {
			if ("long".equals(args[1])) {
				return Long.toString(now);
			}
			DateFormat df = new SimpleDateFormat(args[1], Locale.ROOT);
			df.setTimeZone(Dates.UTC_TIME_ZONE);
			return df.format(new Date(now));
		}
		return Dates.formatMillis(DATE_TOSTRING, now);
	}

	public final static String _fmodifiedHelp = "${fmodified;<list of filenames>...}, return latest modification date";

	public String _fmodified(String args[]) throws Exception {
		verifyCommand(args, _fmodifiedHelp, null, 2, Integer.MAX_VALUE);

		long time = 0;
		Collection<String> names = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			names.addAll(ExtList.from(args[i]));
		}
		for (String name : names) {
			File f = new File(name);
			if (f.exists() && f.lastModified() > time)
				time = f.lastModified();
		}
		return "" + time;
	}

	public String _long2date(String args[]) {
		try {
			return Dates.formatMillis(DATE_TOSTRING, Long.parseLong(args[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "not a valid long";
	}

	public String _literal(String args[]) {
		if (args.length != 2)
			throw new RuntimeException("Need a value for the ${literal;<value>} macro");
		return "${" + args[1] + "}";
	}

	public String _def(String args[]) {
		if (args.length != 2)
			throw new RuntimeException("Need a value for the ${def;<value>} macro");

		String value = domain.getMap()
			.get(args[1]);
		if (value == null)
			return "";

		return value;
	}

	/**
	 * replace ; <list> ; regex ; replace
	 *
	 * @param args
	 * @return result
	 */
	public String _replace(String args[]) {
		if (args.length != 4) {
			reporter.warning("Invalid nr of arguments to replace %s", Arrays.asList(args));
			return null;
		}
		Pattern regex = Pattern.compile(args[2]);
		String replace = (args.length > 3) ? args[3] : "";
		String middle = (args.length > 4) ? args[4] : ",";

		String result = Strings.splitAsStream(args[1])
			.map(element -> regex.matcher(element)
				.replaceAll(replace))
			.collect(joining(middle));
		return result;
	}

	public String _warning(String args[]) {
		for (int i = 1; i < args.length; i++) {
			reporter.warning("%s", process(args[i]));
		}
		return "";
	}

	public String _error(String args[]) {
		for (int i = 1; i < args.length; i++) {
			reporter.error("%s", process(args[i]));
		}
		return "";
	}

	/**
	 * toclassname ; <path>.class ( , <path>.class ) *
	 */
	static String _toclassnameHelp = "${classname;<list of class names>}, convert class paths to FQN class names ";

	public String _toclassname(String args[]) {
		verifyCommand(args, _toclassnameHelp, null, 2, 2);
		Collection<String> paths = ExtList.from(args[1]);

		ExtList<String> names = new ExtList<>(paths.size());
		for (String path : paths) {
			if (path.endsWith(".class")) {
				String name = path.substring(0, path.length() - 6)
					.replace('/', '.');
				names.add(name);
			} else if (path.endsWith(".java")) {
				String name = path.substring(0, path.length() - 5)
					.replace('/', '.');
				names.add(name);
			} else {
				reporter.warning("in toclassname, %s, is not a class path because it does not end in .class", args[1]);
			}
		}
		return names.join(",");
	}

	/**
	 * toclassname ; <path>.class ( , <path>.class ) *
	 */

	static String _toclasspathHelp = "${toclasspath;<list>[;boolean]}, convert a list of class names to paths";

	public String _toclasspath(String args[]) {
		verifyCommand(args, _toclasspathHelp, null, 2, 3);
		boolean cl = true;
		if (args.length > 2)
			cl = Boolean.valueOf(args[2]);

		ExtList<String> names = ExtList.from(args[1]);
		ExtList<String> paths = new ExtList<>(names.size());
		for (String name : names) {
			String path = name.replace('.', '/') + (cl ? ".class" : "");
			paths.add(path);
		}
		return paths.join(",");
	}

	public String _dir(String args[]) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${dir;...}");
			return null;
		}
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			File f = IO.getFile(base, args[i]);
			if (f.exists() && f.getParentFile()
				.exists()) {
				sb.append(del);
				sb.append(f.getParentFile()
					.getAbsolutePath());
				del = ",";
			}
		}
		return sb.toString();

	}

	public String _basename(String args[]) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${basename;...}");
			return null;
		}
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			File f = IO.getFile(base, args[i]);
			if (f.exists() && f.getParentFile()
				.exists()) {
				sb.append(del);
				sb.append(f.getName());
				del = ",";
			}
		}
		return sb.toString();

	}

	public String _isfile(String args[]) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${isfile;...}");
			return null;
		}
		boolean isfile = true;
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]).getAbsoluteFile();
			isfile &= f.isFile();
		}
		return isfile ? "true" : "false";

	}

	public String _isdir(String args[]) {
		if (args.length < 2) {
			reporter.warning("Need at least one file name for ${isdir;...}");
			return null;
		}
		boolean isdir = true;
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]).getAbsoluteFile();
			isdir &= f.isDirectory();
		}
		return isdir ? "true" : "false";

	}

	public String _tstamp(String args[]) {
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
			tz = Dates.UTC_TIME_ZONE;
		}
		if (args.length > 3) {
			now = Long.parseLong(args[3]);
		} else {
			now = System.currentTimeMillis();
		}
		if (args.length > 4) {
			reporter.warning("Too many arguments for tstamp: %s", Arrays.toString(args));
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
		sdf.setTimeZone(tz);
		return sdf.format(new Date(now));
	}

	/**
	 * Wildcard a directory. The lists can contain Instruction that are matched
	 * against the given directory ${lsr;<dir>;<list>(;<list>)*} ${lsa;<dir>;
	 * <list>(;<list>)*}
	 *
	 * @author aqute
	 */

	public String _lsr(String args[]) {
		return ls(args, true);
	}

	public String _lsa(String args[]) {
		return ls(args, false);
	}

	String ls(String args[], boolean relative) {
		if (args.length < 2)
			throw new IllegalArgumentException("the ${ls} macro must at least have a directory as parameter");

		File dir = IO.getFile(base, args[1]);
		if (!dir.isAbsolute())
			throw new IllegalArgumentException("the ${ls} macro directory parameter is not absolute: " + dir);

		if (!dir.exists())
			throw new IllegalArgumentException("the ${ls} macro directory parameter does not exist: " + dir);

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
				"the ${ls} macro directory parameter points to a file instead of a directory: " + dir);

		List<File> files = new ArrayList<>(new SortedList<>(dir.listFiles()));

		for (int i = 2; i < args.length; i++) {
			Glob filters = new Glob(args[i]);
			filters.select(files);
		}

		ExtList<String> result = new ExtList<>();
		for (File file : files)
			result.add(relative ? file.getName() : IO.absolutePath(file));

		return result.join(",");
	}

	public String _currenttime(String args[]) {
		return Long.toString(System.currentTimeMillis());
	}

	/**
	 * System command. Execute a command and insert the result.
	 */
	public String system_internal(boolean allowFail, String args[]) throws Exception {
		verifyCommand(args,
			"${" + (allowFail ? "system-allow-fail" : "system") + ";<command>[;<in>]}, execute a system command", null,
			2, 3);
		String command = args[1];
		String input = null;

		if (args.length > 2) {
			input = args[2];
		}

		Process process = Runtime.getRuntime()
			.exec(command, null, base);
		if (input != null) {
			process.getOutputStream()
				.write(input.getBytes(UTF_8));
		}
		process.getOutputStream()
			.close();

		String s = IO.collect(process.getInputStream(), UTF_8);
		s += IO.collect(process.getErrorStream(), UTF_8);
		int exitValue = process.waitFor();
		if (exitValue != 0)
			return exitValue + "";

		if (exitValue != 0) {
			if (!allowFail) {
				reporter.error("System command %s failed with exit code %d", command, exitValue);
			} else {
				reporter.warning("System command %s failed with exit code %d (allowed)", command, exitValue);

			}
		}

		return s.trim();
	}

	public String _system(String args[]) throws Exception {
		return system_internal(false, args);
	}

	public String _system_allow_fail(String args[]) throws Exception {
		String result = "";
		try {
			result = system_internal(true, args);
		} catch (Throwable t) {
			/* ignore */
		}
		return result;
	}

	public String _env(String args[]) {
		verifyCommand(args, "${env;<name>}, get the environmet variable", null, 2, 2);

		try {
			return System.getenv(args[1]);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Get the contents of a file.
	 *
	 * @return contents of file
	 * @throws IOException
	 */

	public String _cat(String args[]) throws IOException {
		verifyCommand(args, "${cat;<in>}, get the content of a file", null, 2, 2);
		File f = IO.getFile(base, args[1]);
		if (f.isFile()) {
			return IO.collect(f);
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

	public static void verifyCommand(String args[], String help, Pattern[] patterns, int low, int high) {
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
				del = ";";
			}
			sb.append("}, is not understood. ");
			sb.append(message);
			throw new IllegalArgumentException(sb.toString());
		}
	}

	// Helper class to track expansion of variables
	// on the stack.
	static class Link {
		Link	previous;
		String	key;
		Domain	start;

		public Link(Domain start, Link previous, String key) {
			this.previous = previous;
			this.key = key;
			this.start = start;
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
			StringBuilder sb = new StringBuilder("[");
			append(sb);
			sb.append("]");
			return sb.toString();
		}

		private void append(StringBuilder sb) {
			if (previous != null) {
				previous.append(sb);
				sb.append(",");
			}
			sb.append(key);
		}
	}

	/**
	 * Take all the properties and translate them to actual values. This method
	 * takes the set properties and traverse them over all entries, including
	 * the default properties for that properties. The values no longer contain
	 * macros.
	 *
	 * @return A new Properties with the flattened values
	 */
	public Map<String, String> getFlattenedProperties() {
		// Some macros only work in a lower Domain, so we
		// do not report unknown macros while flattening
		flattening = true;
		try {
			Map<String, String> flattened = new HashMap<>();
			Map<String, String> source = domain.getMap();
			for (String key : source.keySet()) {
				if (!key.startsWith("_"))
					if (key.startsWith("-"))
						flattened.put(key, source.get(key));
					else
						flattened.put(key, process(source.get(key)));
			}
			return flattened;
		} finally {
			flattening = false;
		}
	}

	public final static String _fileHelp = "${file;<base>;<paths>...}, create correct OS dependent path";

	public String _osfile(String args[]) {
		verifyCommand(args, _fileHelp, null, 3, 3);
		File base = new File(args[1]);
		File f = IO.getFile(base, args[2]);
		return f.getAbsolutePath();
	}

	public String _path(String args[]) {
		ExtList<String> list = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			list.addAll(ExtList.from(args[i]));
		}
		return list.join(File.pathSeparator);
	}

	public static Properties getParent(Properties p) {
		try {
			Field f = Properties.class.getDeclaredField("defaults");
			f.setAccessible(true);
			return (Properties) f.get(p);
		} catch (Exception e) {
			Field[] fields = Properties.class.getFields();
			System.err.println(Arrays.toString(fields));
			return null;
		}
	}

	@Override
	public String process(String line) {
		return process(line, domain);
	}

	/**
	 * Generate a random string, which is guaranteed to be a valid Java
	 * identifier (first character is an ASCII letter, subsequent characters are
	 * ASCII letters or numbers). Takes an optional parameter for the length of
	 * string to generate; default is 8 characters.
	 */
	public String _random(String[] args) {
		int numchars = 8;
		if (args.length > 1) {
			try {
				numchars = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character count parameter in ${random} macro.");
			}
		}

		char[] letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		char[] alphanums = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

		char[] array = new char[numchars];
		for (int i = 0; i < numchars; i++) {
			char c;
			if (i == 0)
				c = letters[random.nextInt(letters.length)];
			else
				c = alphanums[random.nextInt(alphanums.length)];
			array[i] = c;
		}

		return new String(array);
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public int _processors(String args[]) {
		float multiplier = 1F;
		if (args.length > 1)
			multiplier = Float.parseFloat(args[1]);

		return (int) (Runtime.getRuntime()
			.availableProcessors() * multiplier);
	}

	public long _maxMemory(String args[]) {
		return Runtime.getRuntime()
			.maxMemory();
	}

	public long _freeMemory(String args[]) {
		return Runtime.getRuntime()
			.freeMemory();
	}

	public long _nanoTime(String args[]) {
		return System.nanoTime();
	}

	public void addTarget(Object target) {
		targets.remove(target);
		targets.add(target);
	}

	public void removeTarget(Object target) {
		targets.remove(target);
	}

	public String _unescape(String args[]) {
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

	/**
	 * Format bytes
	 */

	public String _bytes(String[] args) {
		Formatter sb = new Formatter();
		for (int i = 0; i < args.length; i++) {
			long l = Long.parseLong(args[1]);
			bytes(sb, l, 0, new String[] {
				"b", "Kb", "Mb", "Gb", "Tb", "Pb", "Eb", "Zb", "Yb", "Bb", "Geopbyte"
			});
		}
		return sb.toString();
	}

	private void bytes(Formatter sb, double l, int i, String[] strings) {
		if (l > 1024 && i < strings.length - 1) {
			bytes(sb, l / 1024, i + 1, strings);
			return;
		}
		l = Math.round(l * 10) / 10;
		sb.format("%s %s", l, strings[i]);
	}
}
