package aQute.lib.getopt;

import static java.lang.invoke.MethodHandles.publicLookup;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.configurable.Config;
import aQute.configurable.Configurable;
import aQute.lib.justif.Justif;
import aQute.lib.markdown.MarkdownFormatter;
import aQute.libg.generics.Create;
import aQute.libg.reporter.ReporterMessages;
import aQute.service.reporter.Reporter;

/**
 * Helps parsing command lines. This class takes target object, a primary
 * command, and a list of arguments. It will then find the command in the target
 * object. The method of this command must start with a "_" and take an
 * parameter of Options type. Usually this is an interface that extends Options.
 * The methods on this interface are options or flags (when they return
 * boolean).
 */
@SuppressWarnings("unchecked")
public class CommandLine {
	final static int				LINELENGTH	= 60;
	private final static Pattern	ASSIGNMENT	= Pattern.compile("(\\w++)\\s*=\\s*(\\S+)\\s*");
	Reporter						reporter;
	Justif							justif		= new Justif(80, 30, 32, 70);
	CommandLineMessages				msg;
	private Object					result;

	class Option {
		public char		shortcut;
		public String	name;
		public String	paramType;
		public String	description;
		public boolean	required;
	}

	public CommandLine(Reporter reporter) {
		this.reporter = reporter;
		msg = ReporterMessages.base(reporter, CommandLineMessages.class);
	}

	/**
	 * Execute a command in a target object with a set of options and arguments
	 * and returns help text if something fails. Errors are reported.
	 */

	public String execute(Object target, String cmd, List<String> input) throws Exception {

		if (cmd.equals("help")) {
			StringBuilder sb = new StringBuilder();
			Formatter f = new Formatter(sb);
			if (input.isEmpty())
				help(f, target);
			else {
				for (String s : input) {
					help(f, target, s);
				}
			}
			f.flush();
			justif.wrap(sb);
			return sb.toString();
		}

		//
		// Find the appropriate method
		//

		List<String> arguments = new ArrayList<>(input);
		Map<String, Method> commands = getCommands(target);

		Method m = commands.get(cmd);
		if (m == null) {
			msg.NoSuchCommand_(cmd);
			return help(target, null, null);
		}

		//
		// Parse the options
		//

		Class<? extends Options> optionClass = (Class<? extends Options>) m.getParameterTypes()[0];
		Options options = getOptions(optionClass, arguments);
		if (options == null) {
			// had some error, already reported
			return help(target, cmd, null);
		}

		// Check if we have an @Arguments annotation that
		// provides patterns for the remainder arguments

		Arguments argumentsAnnotation = optionClass.getAnnotation(Arguments.class);
		if (argumentsAnnotation != null) {
			String[] patterns = argumentsAnnotation.arg();

			// Check for commands without any arguments

			if (patterns.length == 0 && arguments.size() > 0) {
				msg.TooManyArguments_(arguments);
				return help(target, cmd, null);
			}

			// Match the patterns to the given command line

			int i = 0;
			for (; i < patterns.length; i++) {
				String pattern = patterns[i];

				boolean optional = pattern.matches("\\[.*\\]");

				// Handle vararg

				if (pattern.contains("...")) {
					i = Integer.MAX_VALUE;
					break;
				}

				// Check if we're running out of args

				if (i >= arguments.size()) {
					if (!optional) {
						msg.MissingArgument_(patterns[i]);
						return help(target, cmd, optionClass);
					}
				}
			}

			// Check if we have unconsumed arguments left

			if (i < arguments.size()) {
				msg.TooManyArguments_(arguments);
				return help(target, cmd, optionClass);
			}
		}
		if (reporter.getErrors()
			.isEmpty()) {
			m.setAccessible(true);
			try {
				MethodHandle mh = publicLookup().unreflect(m);
				result = Modifier.isStatic(m.getModifiers()) ? mh.invoke(options) : mh.invoke(target, options);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new InvocationTargetException(e);
			}
			return null;
		}
		return help(target, cmd, optionClass);
	}

	public void generateDocumentation(Object target, Appendable out) {
		MarkdownFormatter f = new MarkdownFormatter(out);

		f.h1("Available Commands:");

		Map<String, Method> commands = getCommands(target);
		for (String command : commands.keySet()) {
			Class<? extends Options> specification = (Class<? extends Options>) commands.get(command)
				.getParameterTypes()[0];
			Map<String, Method> options = getOptions(specification);
			Arguments patterns = specification.getAnnotation(Arguments.class);

			f.h2(command);

			Description descr = specification.getAnnotation(Description.class);
			if (descr != null) {
				f.format("%s%n%n", descr.value());
			}

			f.h3("Synopsis:");
			f.code(getSynopsis(command, options, patterns));

			if (!options.isEmpty()) {
				f.h3("Options:");
				for (Entry<String, Method> entry : options.entrySet()) {
					Option option = getOption(entry.getKey(), entry.getValue());

					f.inlineCode("%s -%s --%s %s%s", option.required ? " " : "[", //
						option.shortcut, //
						option.name, option.paramType, //
						option.required ? " " : "]");

					if (option.description != null) {
						f.format("%s", option.description);
						f.endP();
					}

				}
				f.format("%n");
			}
		}
		f.flush();
	}

	private String help(Object target, String cmd, Class<? extends Options> type) throws Exception {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		if (cmd == null)
			help(f, target);
		else if (type == null)
			help(f, target, cmd);
		else
			help(f, target, cmd, type);

		f.flush();
		justif.wrap(sb);
		return sb.toString();
	}

	/**
	 * Parse the options in a command line and return an interface that provides
	 * the options from this command line. This will parse up to (and including)
	 * -- or an argument that does not start with -
	 */
	public <T extends Options> T getOptions(Class<T> specification, List<String> arguments) throws Exception {
		Map<String, String> properties = Create.map();
		Map<String, Object> values = new HashMap<>();
		Map<String, Method> options = getOptions(specification);

		argloop: while (arguments.size() > 0) {

			String option = arguments.get(0);

			if (option.startsWith("-")) {

				arguments.remove(0);

				if (option.startsWith("--")) {

					if ("--".equals(option))
						break argloop;

					// Full named option, e.g. --output
					String name = option.substring(2);
					Method m = options.get(name);
					if (m == null) { // Maybe due to capitalization modif
						m = options.get(Character.toLowerCase(name.charAt(0)) + name.substring(1));
					}
					if (m == null)
						msg.UnrecognizedOption_(name);
					else
						assignOptionValue(values, m, arguments, true);

				} else {

					// Set of single character named options like -a

					charloop: for (int j = 1; j < option.length(); j++) {

						char optionChar = option.charAt(j);

						for (Entry<String, Method> entry : options.entrySet()) {
							if (entry.getKey()
								.charAt(0) == optionChar) {
								boolean last = (j + 1) >= option.length();
								assignOptionValue(values, entry.getValue(), arguments, last);
								continue charloop;
							}
						}
						msg.UnrecognizedOption_(optionChar + "");
					}
				}
			} else {
				Matcher m = ASSIGNMENT.matcher(option);
				if (m.matches()) {
					properties.put(m.group(1), m.group(2));
				}
				break;
			}
		}

		// check if all required elements are set

		for (Entry<String, Method> entry : options.entrySet()) {
			Method m = entry.getValue();
			String name = entry.getKey();
			if (!values.containsKey(name) && isMandatory(m))
				msg.OptionNotSet_(name);
		}

		values.put(".", arguments);
		values.put(".arguments", arguments);
		values.put(".command", this);
		values.put(".properties", properties);
		return Configurable.createConfigurable(specification, values);
	}

	/**
	 * Answer a list of the options specified in an options interface
	 */
	private Map<String, Method> getOptions(Class<? extends Options> interf) {
		Map<String, Method> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		for (Method m : interf.getMethods()) {
			if (m.getName()
				.startsWith("_"))
				continue;

			String name;

			Config cfg = m.getAnnotation(Config.class);
			if (cfg == null || cfg.id() == null || cfg.id()
				.equals(Config.NULL))
				name = m.getName();
			else
				name = cfg.id();

			map.put(name, m);
		}

		// In case two options have the same first char, uppercase one of them
		// In case 3+ --------------------------------, throw an error
		char prevChar = '\0';
		boolean throwOnNextMatch = false;
		Map<String, Method> toModify = new HashMap<>();
		for (String name : map.keySet()) {
			if (Character.toLowerCase(name.charAt(0)) != name.charAt(0)) { //
				throw new Error("Only commands with lower case first char are acceptable (" + name + ")");
			}

			if (Character.toLowerCase(name.charAt(0)) == prevChar) {
				if (throwOnNextMatch) {
					throw new Error("3 options with same first letter (one is: " + name + ")");
				} else {
					toModify.put(name, map.get(name));
					throwOnNextMatch = true;
				}
			} else {
				throwOnNextMatch = false;
				prevChar = name.charAt(0);
			}
		}

		for (String name : toModify.keySet()) {
			map.remove(name);
			String newName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			map.put(newName, toModify.get(name));
		}

		return map;
	}

	/**
	 * Assign an option, must handle flags, parameters, and parameters that can
	 * happen multiple times.
	 *
	 * @param options The command line map
	 * @param args the args input
	 * @param m the selected method for this option
	 * @param last if this is the last in a multi single character option
	 */
	public void assignOptionValue(Map<String, Object> options, Method m, List<String> args, boolean last) {
		String name = m.getName();
		Type type = m.getGenericReturnType();

		if (isOption(m)) {

			// The option is a simple flag

			options.put(name, true);
		} else {

			// The option is followed by an argument

			if (!last) {
				msg.Option__WithArgumentNotLastInAbbreviation_(name, name.charAt(0), getTypeDescriptor(type));
				return;
			}

			if (args.isEmpty()) {
				msg.MissingArgument__(name, name.charAt(0));
				return;
			}

			String parameter = args.remove(0);

			if (Collection.class.isAssignableFrom(m.getReturnType())) {

				Collection<Object> optionValues = (Collection<Object>) options.get(m.getName());

				if (optionValues == null) {
					optionValues = new ArrayList<>();
					options.put(name, optionValues);
				}

				optionValues.add(parameter);
			} else {

				if (options.containsKey(name)) {
					msg.OptionCanOnlyOccurOnce_(name);
					return;
				}

				options.put(name, parameter);
			}
		}
	}

	/**
	 * Provide a help text.
	 */

	public void help(Formatter f, Object target, String cmd, Class<? extends Options> specification) {
		Description descr = specification.getAnnotation(Description.class);
		Arguments patterns = specification.getAnnotation(Arguments.class);

		String description = descr == null ? "" : descr.value();

		f.format("%nNAME%n  %s \t0- \t1%s%n%n", cmd, description);

		Map<String, Method> options = getOptions(specification);
		f.format("SYNOPSIS%n");
		f.format(getSynopsis(cmd, options, patterns));

		help(f, specification, "OPTIONS");
	}

	private void help(Formatter f, Class<? extends Options> specification, String title) {
		Map<String, Method> options = getOptions(specification);
		if (!options.isEmpty()) {
			f.format("%n%s%n%n", title);
			for (Entry<String, Method> entry : options.entrySet()) {
				Option option = getOption(entry.getKey(), entry.getValue());

				f.format("   %s -%s, --%s %s%s \t0- \t1%s%n", option.required ? " " : "[", //
					option.shortcut, //
					option.name, option.paramType, //
					option.required ? " " : "]", //
					option.description);
			}
			f.format("%n");
		}
	}

	private Option getOption(String optionName, Method m) {
		Option option = new Option();
		Config cfg = m.getAnnotation(Config.class);
		Description d = m.getAnnotation(Description.class);

		option.shortcut = optionName.charAt(0);
		option.name = Character.toLowerCase(optionName.charAt(0)) + optionName.substring(1);
		option.description = cfg != null ? cfg.description() : (d == null ? "" : d.value());
		option.required = isMandatory(m);
		String pt = getTypeDescriptor(m.getGenericReturnType());
		if (pt.length() != 0)
			pt += " ";
		option.paramType = pt;

		return option;
	}

	private String getSynopsis(String cmd, Map<String, Method> options, Arguments patterns) {
		StringBuilder sb = new StringBuilder();
		if (options.isEmpty())
			sb.append(String.format("   %s ", cmd));
		else
			sb.append(String.format("   %s [options] ", cmd));

		if (patterns == null)
			sb.append(String.format(" ...%n%n"));
		else {
			String del = " ";
			for (String pattern : patterns.arg()) {
				if (pattern.equals("..."))
					sb.append(String.format("%s...", del));
				else
					sb.append(String.format("%s<%s>", del, pattern));
				del = " ";
			}
			sb.append(String.format("%n"));
		}
		return sb.toString();
	}

	private final static Pattern LAST_PART = Pattern.compile(".*[.$]([^.$]+)");

	private static String lastPart(String name) {
		Matcher m = LAST_PART.matcher(name);
		if (m.matches())
			return m.group(1);
		return name;
	}

	/**
	 * Show all commands in a target
	 */
	public void help(Formatter f, Object target) throws Exception {
		f.format("%n");
		Description descr = target.getClass()
			.getAnnotation(Description.class);
		if (descr != null) {
			f.format("%s%n%n", descr.value());
		}
		for (Entry<String, Method> e : getCommands(target).entrySet()) {
			Method m = e.getValue();
			if (m.getName()
				.startsWith("__")) {
				Class<? extends Options> options = (Class<? extends Options>) m.getParameterTypes()[0];
				help(f, options, "MAIN OPTIONS");
			}
		}
		f.format("Available sub-commands: %n%n");

		for (Entry<String, Method> e : getCommands(target).entrySet()) {
			if (e.getValue()
				.getName()
				.startsWith("__"))
				continue;

			Description d = e.getValue()
				.getAnnotation(Description.class);
			String desc = " ";
			if (d != null)
				desc = d.value();

			f.format("  %s\t0-\t1%s %n", e.getKey(), desc);
		}
		f.format("%n");

	}

	/**
	 * Show the full help for a given command
	 */
	public void help(Formatter f, Object target, String cmd) {

		Method m = getCommands(target).get(cmd);
		if (m == null)
			f.format("No such command: %s%n", cmd);
		else {
			Class<? extends Options> options = (Class<? extends Options>) m.getParameterTypes()[0];
			help(f, target, cmd, options);
		}
	}

	/**
	 * Parse a class and return a list of command names
	 *
	 * @param target
	 * @return command names
	 */
	public Map<String, Method> getCommands(Object target) {
		Map<String, Method> map = new TreeMap<>();

		for (Method m : target.getClass()
			.getMethods()) {

			if (m.getParameterTypes().length == 1 && m.getName()
				.startsWith("_")) {
				Class<?> clazz = m.getParameterTypes()[0];
				if (Options.class.isAssignableFrom(clazz)) {
					String name = m.getName()
						.substring(1);
					map.put(name, m);
				}
			}
		}
		return map;
	}

	/**
	 * Answer if the method is marked mandatory
	 */
	private boolean isMandatory(Method m) {
		Config cfg = m.getAnnotation(Config.class);
		if (cfg == null)
			return false;

		return cfg.required();
	}

	/**
	 * @param m
	 */
	private boolean isOption(Method m) {
		return m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class;
	}

	/**
	 * Show a type in a nice way
	 */

	private String getTypeDescriptor(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type c = pt.getRawType();
			if (c instanceof Class) {
				if (Collection.class.isAssignableFrom((Class<?>) c)) {
					return getTypeDescriptor(pt.getActualTypeArguments()[0]) + "*";
				}
			}
		}
		if (!(type instanceof Class))
			return "<>";

		Class<?> clazz = (Class<?>) type;

		if (clazz == Boolean.class || clazz == boolean.class)
			return ""; // Is a flag

		return "<" + lastPart(clazz.getName()
			.toLowerCase()) + ">";
	}

	public Object getResult() {
		return result;
	}

	public String subCmd(Options opts, Object target) throws Exception {
		List<String> arguments = opts._arguments();

		if (arguments.isEmpty()) {
			Justif j = new Justif();
			Formatter f = j.formatter();
			help(f, target);
			return j.wrap();
		} else {
			String cmd = arguments.remove(0);
			return execute(target, cmd, arguments);
		}
	}
}
