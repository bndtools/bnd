package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.base64.Base64;
import aQute.lib.collections.ExtList;
import aQute.lib.collections.Iterables;
import aQute.lib.collections.SortedList;
import aQute.lib.filter.ExtendedFilter;
import aQute.lib.filter.Get;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;
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
	final static String		NULLVALUE		= "c29e43048791e250dfd5723e7b8aa048df802c9262cfa8fbc4475b2e392a8ad2";
	final static String		LITERALVALUE	= "017a3ddbfc0fcd27bcdb2590cdb713a379ae59ef";
	final static Pattern	NUMERIC_P		= Pattern.compile("[-+]?(\\d*\\.?\\d+|\\d+\\.)(e[-+]?[0-9]+)?");
	final static Pattern	PRINTF_P		= Pattern.compile(
		"%(?:(\\d+)\\$)?(-|\\+|0|\\(|,|\\^|#| )*(\\d*)?(?:\\.(\\d+))?(a|A|b|B|h|H|d|f|c|s|x|X|u|o|z|Z|e|E|g|G|p|n|b|B|%)");
	Processor				domain;
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
							.collect(joining(","));
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
					if (value != null)
						return value;
				}

				if (key != null && key.indexOf(';') >= 0) {
					String parts[] = key.split(";");
					if (parts.length > 1) {
						if (parts.length >= 16) {
							domain.error("too many arguments for template: %s, max is 16", key);
						}

						String template = domain.getProperties()
							.getProperty(parts[0]);
						if (template != null) {
							domain = new Processor(domain);
							for (int i = 0; i < 16; i++) {
								domain.setProperty("" + i, i < parts.length ? parts[i] : "null");
							}
							ExtList<String> args = new ExtList<>(parts);
							args.remove(0);
							domain.setProperty("#", args.join());
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
				domain.warning("Found empty macro key");
			}
		} else {
			domain.warning("Found null macro key");
		}

		// Prevent recursion, but try to get a profiled variable
		if (key != null && !key.startsWith("[") && !key.equals(Constants.PROFILE)) {
			if (profile == null)
				profile = domain.get(Constants.PROFILE);
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
				domain.warning("No translation found for macro: %s", key);
		}
		return "$" + begin + key + end;
	}

	/**
	 * Parse the key as a command. A command consist of parameters separated by
	 * ':'.
	 */
	static Pattern commands = Pattern.compile("(?<!\\\\);");

	@SuppressWarnings("resource")
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
			try {
				Method m = target.getClass()
					.getMethod(cname, String[].class);
				Object result = m.invoke(target, new Object[] {
					args
				});
				return result == null ? NULLVALUE : result.toString();
			} catch (NoSuchMethodException e) {
				return null;
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof IllegalArgumentException) {
					domain.error("%s, for cmd: %s, arguments; %s", e.getCause()
						.getMessage(), method, Arrays.toString(args));
				} else {
					domain.warning("Exception in replace: %s", e.getCause());
				}
				return NULLVALUE;
			} catch (Exception e) {
				domain.warning("Exception in replace: %s method=%s", e, method);
				return NULLVALUE;
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
			Processor.split(args[i], set);
		}
		return Processor.join(set, ",");
	}

	/**
	 * Return the first list where items from the second list are removed.
	 */
	static String _removeall = "${removeall;<list>;<list>}";

	public String _removeall(String args[]) {
		verifyCommand(args, _removeall, null, 1, 3);
		List<String> result = new ArrayList<>();
		if (args.length > 1) {
			Processor.split(args[1], result);
		}
		List<String> remove = new ArrayList<>();
		if (args.length > 2) {
			Processor.split(args[2], remove);
		}
		result.removeAll(remove);
		return Processor.join(result, ",");
	}

	/**
	 * Return the first list where items not in the second list are removed.
	 */
	static String _retainall = "${retainall;<list>;<list>}";

	public String _retainall(String args[]) {
		verifyCommand(args, _retainall, null, 1, 3);
		List<String> result = new ArrayList<>();
		if (args.length > 1) {
			Processor.split(args[1], result);
		}
		List<String> retain = new ArrayList<>();
		if (args.length > 2) {
			Processor.split(args[2], retain);
		}
		result.retainAll(retain);
		return Processor.join(result, ",");
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

	public String _select(String args[]) {
		return filter(args, false);
	}

	public String _filterout(String args[]) {
		return filter(args, true);

	}

	public String _reject(String args[]) {
		return filter(args, true);

	}

	static String _filterHelp = "${%s;<list>;<regex>}";

	String filter(String[] args, boolean include) {
		verifyCommand(args, String.format(_filterHelp, args[0]), null, 3, 3);

		Collection<String> list = toCollection(args[1]);
		Pattern pattern = Pattern.compile(args[2]);

		list.removeIf(s -> pattern.matcher(s)
			.matches() == include);
		return Processor.join(list);
	}

	ArrayList<String> toCollection(String arg) {
		return new ArrayList<>(Processor.split(arg));
	}

	static String _sortHelp = "${sort;<list>...}";

	public String _sort(String args[]) {
		verifyCommand(args, _sortHelp, null, 2, Integer.MAX_VALUE);

		List<String> result = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		Collections.sort(result);
		return Processor.join(result);
	}

	static String _nsortHelp = "${nsort;<list>...}";

	public String _nsort(String args[]) {
		verifyCommand(args, _nsortHelp, null, 2, Integer.MAX_VALUE);

		ExtList<String> result = new ExtList<>();
		for (int i = 1; i < args.length; i++) {
			result.addAll(ExtList.from(args[i]));
		}
		Collections.sort(result, new Comparator<String>() {

			@Override
			public int compare(String a, String b) {
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

			}
		});
		return result.join();
	}

	static String _joinHelp = "${join;<list>...}";

	public String _join(String args[]) {

		verifyCommand(args, _joinHelp, null, 1, Integer.MAX_VALUE);

		List<String> result = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		return Processor.join(result);
	}

	static String _sjoinHelp = "${sjoin;<separator>;<list>...}";

	public String _sjoin(String args[]) throws Exception {
		verifyCommand(args, _sjoinHelp, null, 2, Integer.MAX_VALUE);

		List<String> result = new ArrayList<>();
		for (int i = 2; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		return Processor.join(result, args[1]);
	}

	static String _ifHelp = "${if;<condition>;<iftrue> [;<iffalse>] } condition is either a filter expression or truthy";

	public String _if(String args[]) throws Exception {
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

	public Object _now(String args[]) {
		verifyCommand(args, _nowHelp, null, 1, 2);
		Date now = new Date();

		if (args.length == 2) {
			if ("long".equals(args[1]))
				return now.getTime();

			DateFormat df = new SimpleDateFormat(args[1], Locale.US);
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			return df.format(now);
		}
		return new Date();
	}

	public final static String _fmodifiedHelp = "${fmodified;<list of filenames>...}, return latest modification date";

	public String _fmodified(String args[]) throws Exception {
		verifyCommand(args, _fmodifiedHelp, null, 2, Integer.MAX_VALUE);

		long time = 0;
		Collection<String> names = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], names);
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
			return new Date(Long.parseLong(args[1])).toString();
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
		verifyCommand(args, "${def;<name>[;<value>]}, get the property or a default value if unset", null, 2, 3);

		return domain.getProperty(args[1], args.length == 3 ? args[2] : "");
	}

	static String _replace = "${replace;<list>;<regex>;[<replace>[;delimiter]]}";

	/**
	 * replace ; <list> ; regex ; replace
	 *
	 * @param args
	 */
	public String _replace(String args[]) {
		verifyCommand(args, _replace, null, 3, 5);

		String replace = "";
		if (args.length > 3) {
			replace = args[3];
		}

		String middle = ", ";
		if (args.length > 4)
			middle = args[4];

		String list[] = args[1].split("\\s*,\\s*");
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < list.length; i++) {
			String element = list[i].trim();
			if (!element.equals("")) {
				sb.append(del);
				sb.append(element.replaceAll(args[2], replace));
				del = middle;
			}
		}

		return sb.toString();
	}

	public String _warning(String args[]) throws Exception {
		for (int i = 1; i < args.length; i++) {
			SetLocation warning = domain.warning("%s", process(args[i]));
			FileLine header = domain.getHeader(Pattern.compile(".*"), Pattern.compile("\\$\\{warning;"));
			if (header != null)
				header.set(warning);
		}
		return "";
	}

	public String _error(String args[]) throws Exception {
		for (int i = 1; i < args.length; i++) {
			SetLocation error = domain.error("%s", process(args[i]));
			FileLine header = domain.getHeader(Pattern.compile(".*"), Pattern.compile("\\$\\{error;"));
			if (header != null)
				header.set(error);
		}
		return "";
	}

	/**
	 * toclassname ; <path>.class ( , <path>.class ) *
	 */
	static String _toclassnameHelp = "${classname;<list of class names>}, convert class paths to FQN class names ";

	public String _toclassname(String args[]) {
		verifyCommand(args, _toclassnameHelp, null, 2, 2);
		Collection<String> paths = Processor.split(args[1]);

		List<String> names = new ArrayList<>(paths.size());
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
				domain.warning("in toclassname, %s is not a class path because it does not end in .class", args[1]);
			}
		}
		return Processor.join(names, ",");
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

		Collection<String> names = Processor.split(args[1]);
		Collection<String> paths = new ArrayList<>(names.size());
		for (String name : names) {
			String path = name.replace('.', '/') + (cl ? ".class" : "");
			paths.add(path);
		}
		return Processor.join(paths, ",");
	}

	public String _dir(String args[]) {
		if (args.length < 2) {
			domain.warning("Need at least one file name for ${dir;...}");
			return null;
		}
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			File f = domain.getFile(args[i]);
			if (f.exists() && f.getParentFile()
				.exists()) {
				sb.append(del);
				sb.append(IO.absolutePath(f.getParentFile()));
				del = ",";
			}
		}
		return sb.toString();

	}

	public String _basename(String args[]) {
		if (args.length < 2) {
			domain.warning("Need at least one file name for ${basename;...}");
			return null;
		}
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			File f = domain.getFile(args[i]);
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
			domain.warning("Need at least one file name for ${isfile;...}");
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
		// if (args.length < 2) {
		// domain.warning("Need at least one file name for ${isdir;...}");
		// return null;
		// }
		boolean isdir = true;
		// If no dirs provided, return false
		if (args.length < 2) {
			isdir = false;
		}
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
			domain.warning("Too many arguments for tstamp: %s", Arrays.toString(args));
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

		File dir = domain.getFile(args[1]);
		if (!dir.isAbsolute())
			throw new IllegalArgumentException("the ${ls} macro directory parameter is not absolute: " + dir);

		if (!dir.exists())
			throw new IllegalArgumentException("the ${ls} macro directory parameter does not exist: " + dir);

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
				"the ${ls} macro directory parameter points to a file instead of a directory: " + dir);

		Collection<File> files = new ArrayList<>(new SortedList<>(dir.listFiles()));

		for (int i = 2; i < args.length; i++) {
			Instructions filters = new Instructions(args[i]);
			files = filters.select(files, true);
		}

		List<String> result = new ArrayList<>();
		for (File file : files)
			result.add(relative ? file.getName()
				: IO.absolutePath(file));

		return Processor.join(result, ",");
	}

	public String _currenttime(String args[]) {
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
	final static String		MASK_STRING			= "[\\-+=~0123456789]{0,3}[=~]?";
	final static Pattern	MASK				= Pattern.compile(MASK_STRING);
	final static String		_versionHelp		= "${version;<mask>;<version>}, modify a version\n"
		+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n" + "M ::= '+' | '-' | MQ\n" + "MQ ::= '~' | '='";
	final static Pattern	_versionPattern[]	= new Pattern[] {
		null, null, MASK, Verifier.VERSION
	};

	public String _versionmask(String args[]) {
		return _version(args);
	}

	public String _version(String args[]) {
		verifyCommand(args, _versionHelp, null, 2, 3);

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
			String v = domain.getProperty("@");
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

	static Pattern	RANGE_MASK		= Pattern.compile("(\\[|\\()(" + MASK_STRING + "),(" + MASK_STRING + ")(\\]|\\))");
	static String	_rangeHelp		= "${range;<mask>[;<version>]}, range for version, if version not specified lookup ${@}\n"
		+ "<mask> ::= [ M [ M [ M [ MQ ]]]\n" + "M ::= '+' | '-' | MQ\n" + "MQ ::= '~' | '='";
	static Pattern	_rangePattern[]	= new Pattern[] {
		null, RANGE_MASK
	};

	public String _range(String args[]) {
		verifyCommand(args, _rangeHelp, _rangePattern, 2, 3);
		Version version = null;
		if (args.length >= 3) {
			String string = args[2];
			if (isLocalTarget(string))
				return LITERALVALUE;

			version = new Version(string);
		} else {
			String v = domain.getProperty("@");
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
			domain.error("${range} macro created an invalid range %s from %s and mask %s", s, version, spec);
		}
		return sb.toString();
	}

	boolean isLocalTarget(String string) {
		return string.matches("\\$(\\{@\\}|\\[@\\]|\\(@\\)|<@>|«@»|‹@›)");
	}

	/**
	 * System command. Execute a command and insert the result.
	 */
	public String system_internal(boolean allowFail, String args[]) throws Exception {
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

		if (File.separatorChar == '\\')
			command = "cmd /c \"" + command + "\"";

		Process process = Runtime.getRuntime()
			.exec(command, null, domain.getBase());
		if (input != null) {
			process.getOutputStream()
				.write(input.getBytes(UTF_8));
		}
		process.getOutputStream()
			.close();

		String s = IO.collect(process.getInputStream(), UTF_8);
		int exitValue = process.waitFor();

		if (exitValue != 0) {
			if (!allowFail) {
				domain.error("System command %s failed with exit code %d", command, exitValue);
			} else {
				domain.warning("System command %s failed with exit code %d (allowed)", command, exitValue);

			}
			return null;
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
			return result == null ? "" : result;
		} catch (Throwable t) {
			/* ignore */
			return "";
		}
	}

	public String _env(String args[]) {
		verifyCommand(args, "${env;<name>[;alternative]}, get the environment variable", null, 2, 3);

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

	/**
	 * Get the contents of a file.
	 *
	 * @throws IOException
	 */

	public String _cat(String args[]) throws IOException {
		verifyCommand(args, "${cat;<in>}, get the content of a file", null, 2, 2);
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

	/**
	 * Get the Base64 encoding of a file.
	 *
	 * @throws IOException
	 */
	public String _base64(String... args) throws IOException {
		verifyCommand(args, "${base64;<file>[;fileSizeLimit]}, get the Base64 encoding of a file", null, 2, 3);

		File file = domain.getFile(args[1]);
		long maxLength = 100_000;
		if (args.length > 2)
			maxLength = Long.parseLong(args[2]);

		if (file.length() > maxLength)
			throw new IllegalArgumentException(
				"Maximum file size (" + maxLength + ") for base64 macro exceeded for file " + file);

		return Base64.encodeBase64(file);
	}

	/**
	 * Get a digest of a file.
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public String _digest(String... args) throws NoSuchAlgorithmException, IOException {
		verifyCommand(args, "${digest;<algo>;<in>}, get a digest (e.g. MD5, SHA-256) of a file", null, 3, 3);

		MessageDigest digester = MessageDigest.getInstance(args[1]);
		File f = domain.getFile(args[2]);
		IO.copy(f, digester);
		byte[] digest = digester.digest();
		return Hex.toHexString(digest);
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
						domain.warning("Key '%s' has a non-String value: %s:%s", key, raw == null ? ""
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

	public String _osfile(String args[]) {
		verifyCommand(args, _fileHelp, null, 3, 3);
		File base = new File(args[1]);
		File f = Processor.getFile(base, args[2]);
		return IO.absolutePath(f);
	}

	public String _path(String args[]) {
		List<String> list = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			list.addAll(Processor.split(args[i]));
		}
		return Processor.join(list, File.pathSeparator);
	}

	public final static String _sizeHelp = "${size;<collection>;...}, count the number of elements (of all collections combined)";

	public int _size(String args[]) {
		verifyCommand(args, _sizeHelp, null, 1, 16);
		int size = 0;
		for (int i = 1; i < args.length; i++) {
			ExtList<String> l = ExtList.from(args[i]);
			size += l.size();
		}
		return size;
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

	static String _startswith = "${startswith;<string>;<prefix>}";

	public String _startswith(String args[]) throws Exception {
		verifyCommand(args, _startswith, null, 3, 3);
		if (args[1].startsWith(args[2]))
			return args[1];
		else
			return "";
	}

	static String _endswith = "${endswith;<string>;<suffix>}";

	public String _endswith(String args[]) throws Exception {
		verifyCommand(args, _endswith, null, 3, 3);
		if (args[1].endsWith(args[2]))
			return args[1];
		else
			return "";
	}

	static String _extension = "${extension;<string>}";

	public String _extension(String args[]) throws Exception {
		verifyCommand(args, _extension, null, 2, 2);
		String name = args[1];
		int n = name.indexOf('.');
		if (n < 0)
			return "";
		return name.substring(n + 1);
	}

	static String _stem = "${stem;<string>}";

	public String _stem(String args[]) throws Exception {
		verifyCommand(args, _stem, null, 2, 2);
		String name = args[1];
		int n = name.indexOf('.');
		if (n < 0)
			return name;
		return name.substring(0, n);
	}

	static String _substring = "${substring;<string>;<start>[;<end>]}";

	public String _substring(String args[]) throws Exception {
		verifyCommand(args, _substring, null, 3, 4);

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

	static String	_rand	= "${rand;[<min>[;<end>]]}";
	static Random	random	= new Random();

	public long _rand(String args[]) throws Exception {
		verifyCommand(args, _rand, null, 2, 3);

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

	static String _length = "${length;<string>}";

	public int _length(String args[]) throws Exception {
		verifyCommand(args, _length, null, 1, 2);
		if (args.length == 1)
			return 0;

		return args[1].length();
	}

	static String _get = "${get;<index>;<list>}";

	public String _get(String args[]) throws Exception {
		verifyCommand(args, _get, null, 3, 3);

		int index = Integer.parseInt(args[1]);
		List<String> list = toList(args, 2, 3);
		if (index < 0)
			index = list.size() + index;
		return list.get(index);
	}

	static String _sublist = "${sublist;<start>;<end>[;<list>...]}";

	public String _sublist(String args[]) throws Exception {
		verifyCommand(args, _sublist, null, 4, Integer.MAX_VALUE);

		int start = Integer.parseInt(args[1]);
		int end = Integer.parseInt(args[2]);
		ExtList<String> list = toList(args, 3, args.length);

		if (start < 0)
			start = list.size() + start + 1;

		if (end < 0)
			end = list.size() + end + 1;

		if (start > end) {
			int t = start;
			start = end;
			end = t;
		}

		return Processor.join(list.subList(start, end));
	}

	private ExtList<String> toList(String[] args, int i, int j) {
		ExtList<String> list = new ExtList<>();
		for (; i < j; i++) {
			Processor.split(args[i], list);
		}
		return list;
	}

	static String _first = "${first;<list>[;<list>...]}";

	public String _first(String args[]) throws Exception {
		verifyCommand(args, _first, null, 1, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			return "";

		return list.get(0);
	}

	static String _last = "${last;<list>[;<list>...]}";

	public String _last(String args[]) throws Exception {
		verifyCommand(args, _last, null, 1, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			return "";

		return list.get(list.size() - 1);
	}

	static String _max = "${max;<list>[;<list>...]}";

	public String _max(String args[]) throws Exception {
		verifyCommand(args, _max, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		String a = null;

		for (String s : list) {
			if (a == null || a.compareTo(s) < 0)
				a = s;
		}
		if (a == null)
			return "";

		return a;
	}

	static String _min = "${min;<list>[;<list>...]}";

	public String _min(String args[]) throws Exception {
		verifyCommand(args, _min, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		String a = null;

		for (String s : list) {
			if (a == null || a.compareTo(s) > 0)
				a = s;
		}
		if (a == null)
			return "";

		return a;
	}

	static String _nmax = "${nmax;<list>[;<list>...]}";

	public String _nmax(String args[]) throws Exception {
		verifyCommand(args, _nmax, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = Double.NaN;

		for (String s : list) {
			double v = Double.parseDouble(s);
			if (Double.isNaN(d) || v > d)
				d = v;
		}
		return toString(d);
	}

	static String _nmin = "${nmin;<list>[;<list>...]}";

	public String _nmin(String args[]) throws Exception {
		verifyCommand(args, _nmin, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = Double.NaN;

		for (String s : list) {
			double v = Double.parseDouble(s);
			if (Double.isNaN(d) || v < d)
				d = v;
		}
		return toString(d);
	}

	static String _sum = "${sum;<list>[;<list>...]}";

	public String _sum(String args[]) throws Exception {
		verifyCommand(args, _sum, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = 0;

		for (String s : list) {
			double v = Double.parseDouble(s);
			d += v;
		}
		return toString(d);
	}

	static String _average = "${average;<list>[;<list>...]}";

	public String _average(String args[]) throws Exception {
		verifyCommand(args, _sum, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			throw new IllegalArgumentException("No members in list to calculate average");

		double d = 0;

		for (String s : list) {
			double v = Double.parseDouble(s);
			d += v;
		}
		return toString(d / list.size());
	}

	static String _reverse = "${reverse;<list>[;<list>...]}";

	public String _reverse(String args[]) throws Exception {
		verifyCommand(args, _reverse, null, 2, Integer.MAX_VALUE);

		ExtList<String> list = toList(args, 1, args.length);
		Collections.reverse(list);
		return Processor.join(list);
	}

	static String _indexof = "${indexof;<value>;<list>[;<list>...]}";

	public int _indexof(String args[]) throws Exception {
		verifyCommand(args, _indexof, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		ExtList<String> list = toList(args, 2, args.length);
		return list.indexOf(value);
	}

	static String _lastindexof = "${lastindexof;<value>;<list>[;<list>...]}";

	public int _lastindexof(String args[]) throws Exception {
		verifyCommand(args, _indexof, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		ExtList<String> list = toList(args, 1, args.length);
		return list.lastIndexOf(value);
	}

	static String _find = "${find;<target>;<searched>}";

	public int _find(String args[]) throws Exception {
		verifyCommand(args, _find, null, 3, 3);

		return args[1].indexOf(args[2]);
	}

	static String _findlast = "${findlast;<find>;<target>}";

	public int _findlast(String args[]) throws Exception {
		verifyCommand(args, _findlast, null, 3, 3);

		return args[2].lastIndexOf(args[1]);
	}

	static String _split = "${split;<regex>[;<target>...]}";

	public String _split(String args[]) throws Exception {
		verifyCommand(args, _split, null, 2, Integer.MAX_VALUE);

		List<String> collected = new ArrayList<>();
		for (int n = 2; n < args.length; n++) {
			String value = args[n];
			String[] split = value.split(args[1]);
			for (String s : split)
				if (!s.isEmpty())
					collected.add(s);
		}
		return Processor.join(collected);
	}

	static String _js = "${js [;<js expr>...]}";

	public Object _js(String args[]) throws Exception {
		verifyCommand(args, _js, null, 2, Integer.MAX_VALUE);

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
			domain.error("Executing js: %s: %s", sb, buffer);
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
			if (v.endsWith(".0"))
				return v.substring(0, v.length() - 2);
		}
		return eval.toString();

	}

	static String _toupper = "${toupper;<target>}";

	public String _toupper(String args[]) throws Exception {
		verifyCommand(args, _tolower, null, 2, 2);

		return args[1].toUpperCase();
	}

	static String _tolower = "${tolower;<target>}";

	public String _tolower(String args[]) throws Exception {
		verifyCommand(args, _tolower, null, 2, 2);

		return args[1].toLowerCase();
	}

	static String _compare = "${compare;<astring>;<bstring>}";

	public int _compare(String args[]) throws Exception {
		verifyCommand(args, _compare, null, 3, 3);
		int n = args[1].compareTo(args[2]);
		if (n == 0)
			return 0;

		return n > 0 ? 1 : -1;
	}

	static String _ncompare = "${ncompare;<anumber>;<bnumber>}";

	public int _ncompare(String args[]) throws Exception {
		verifyCommand(args, _ncompare, null, 3, 3);
		double a = Double.parseDouble(args[1]);
		double b = Double.parseDouble(args[2]);
		if (a > b)
			return 1;
		if (a < b)
			return -1;
		return 0;
	}

	static String _matches = "${matches;<target>;<regex>}";

	public boolean _matches(String args[]) throws Exception {
		verifyCommand(args, _matches, null, 3, 3);

		return args[1].matches(args[2]);
	}

	static String _subst = "${subst;<target>;<regex>[;<replace>[;count]]}";

	public StringBuffer _subst(String args[]) throws Exception {
		verifyCommand(args, _subst, null, 3, 5);

		Pattern p = Pattern.compile(args[2]);
		Matcher matcher = p.matcher(args[1]);
		String replace = "";
		int count = Integer.MAX_VALUE;

		if (args.length > 3) {
			replace = args[3];
		}

		if (args.length > 4) {
			count = Integer.parseInt(args[4]);
		}

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < count; i++) {
			if (matcher.find()) {
				matcher.appendReplacement(sb, replace);
			} else
				break;
		}
		matcher.appendTail(sb);
		return sb;
	}

	static String _trim = "${trim;<target>}";

	public String _trim(String args[]) throws Exception {
		verifyCommand(args, _trim, null, 2, 2);

		return args[1].trim();
	}

	static String _format = "${format;<format>[;args...]}";

	public String _format(String args[]) throws Exception {
		verifyCommand(args, _format, null, 2, Integer.MAX_VALUE);

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

	static String _isempty = "${isempty;[<target>...]}";

	public boolean _isempty(String args[]) throws Exception {
		verifyCommand(args, _isempty, null, 1, Integer.MAX_VALUE);

		for (int i = 1; i < args.length; i++)
			if (!args[i].trim()
				.isEmpty())
				return false;

		return true;
	}

	static String _isnumber = "${isnumber[;<target>...]}";

	public boolean _isnumber(String args[]) throws Exception {
		verifyCommand(args, _isnumber, null, 2, Integer.MAX_VALUE);

		for (int i = 1; i < args.length; i++)
			if (!NUMERIC_P.matcher(args[i])
				.matches())
				return false;

		return true;
	}

	static String _is = "${is;<a>;<b>}";

	public boolean _is(String args[]) throws Exception {
		verifyCommand(args, _is, null, 3, Integer.MAX_VALUE);
		String a = args[1];

		for (int i = 2; i < args.length; i++)
			if (!a.equals(args[i]))
				return false;

		return true;
	}

	/**
	 * Map a value from a list to a new value
	 */

	static String _map = "${map;<macro>[;<list>...]}";

	public String _map(String args[]) throws Exception {
		verifyCommand(args, _map, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args, 2, args.length);
		List<String> result = new ArrayList<>();

		for (String s : list) {
			String invoc = process("${" + macro + ";" + s + "}");
			result.add(invoc);
		}

		return Processor.join(result);
	}

	/**
	 * Map a value from a list to a new value, providing the value and the index
	 */

	static String _foreach = "${foreach;<macro>[;<list>...]}";

	public String _foreach(String args[]) throws Exception {
		verifyCommand(args, _foreach, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args, 2, args.length);
		List<String> result = new ArrayList<>();

		int n = 0;
		for (String s : list) {
			String invoc = process("${" + macro + ";" + s + ";" + n++ + "}");
			result.add(invoc);
		}

		return Processor.join(result);
	}

	/**
	 * Take a list and convert this to the argumets
	 */

	static String _apply = "${apply;<macro>[;<list>...]}";

	public String _apply(String args[]) throws Exception {
		verifyCommand(args, _apply, null, 2, Integer.MAX_VALUE);
		String macro = args[1];
		List<String> list = toList(args, 2, args.length);
		List<String> result = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		sb.append("${")
			.append(macro);
		for (String s : list) {
			sb.append(";")
				.append(s);
		}
		sb.append("}");

		return process(sb.toString());
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

	static String _globHelp = "${glob;<globexp>} (turn it into a regular expression)";

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
					return toCollection(domain.getProperty(key));
				} else

					return domain.getProperty(key);
			}

		});
	}

}
