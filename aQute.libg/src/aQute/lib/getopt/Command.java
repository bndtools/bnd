package aQute.lib.getopt;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.configurable.*;
import aQute.lib.collections.*;
import aQute.libg.generics.*;

/**
 * Helps parsing command lines.
 * 
 */
public class Command {
	static Pattern	ASSIGNMENT	= Pattern.compile("(\\w[\\w\\d]*+)\\s*=\\s*([^\\s]+)\\s*");
	Appendable		app;
	Formatter		out;

	public Command(Appendable out) {
		this.app = out;
		this.out = new Formatter(out);
	}

	/**
	 * Parse a command line and return an interface that provides access to the
	 * flags and parameters.
	 * 
	 * @param args
	 * @param first
	 * @param specification
	 * @return
	 * @throws SecurityException
	 */

	public <T extends Options> T getopt(String args[], Class<T> specification,
			boolean stopAtFirstNonParm) {
		return getopt(new ExtList<String>(args), specification);
	}

	public <T extends Options> T getopt(List<String> args, Class<T> specification)
			throws SecurityException {
		Map<String, String> properties = Create.map();
		Collection<String> value = new ArrayList<String>();
		Map<String, Object> line = new HashMap<String, Object>();
		argloop: while (args.size() > 0) {
			String arg = args.get(0);

			if (arg.startsWith("-")) {
				args.remove(0);
				//
				// Check for full named options
				//
				if (arg.startsWith("--")) {

					//
					// A -- will break parsing and will treat the remainder
					// as
					// values.
					//
					if ("--".equals(arg)) {
						value.addAll(args);
						break argloop;
					}

					if ("--help".equals(arg) || "-h".equals(arg) || "-?".equals(arg)) {
						line.put(".help", "true");
						break argloop;
					}

					// Full named option, e.g. --output
					String name = arg.substring(2);
					for (Method m : specification.getMethods()) {
						if (getName(m).equals(name)) {
							String error = assign(line, m, args, true);
							if (error != null) {
								out.format("for %s, %s", arg, error);
								return null;
							}
							break;
						}
					}

				} else // Set of single character named options
				{
					charloop: for (int j = 1; j < arg.length(); j++) {

						char c = arg.charAt(j); // get option

						//
						// Find the method that starts with that character
						//
						for (Method m : specification.getMethods()) {
							if (getName(m).charAt(0) == c) {
								String error = assign(line, m, args, (j + 1) >= arg.length());
								if (error != null) {
									out.format("for %s, %s", arg, error);
								}
								continue charloop;
							}
						}
						out.format("No such option -%s", c);
						return null;
					}
				}
			} else {
				Matcher m = ASSIGNMENT.matcher(arg);
				if (m.matches()) {
					properties.put(m.group(1), m.group(2));
				} else {
					value.addAll(args);
					args.clear();
				}
			}
		}

		// check if all required elements are set

		for (Method m : specification.getMethods()) {
			Config cfg = m.getAnnotation(Config.class);
			if (cfg != null && cfg.required() && !line.containsKey(getName(m))) {
				out.format("Required option --%s not set", getName(m));
			}
		}

		line.put(".", value);
		line.put(".command", this);
		line.put(".properties", properties);
		return Configurable.createConfigurable(specification, line);
	}

	/**
	 * Assign an option, must handle flags, parameters, and parameters that can
	 * happen multiple times.
	 * 
	 * @param paramaters
	 *            The command line map
	 * @param args
	 *            the args input
	 * @param i
	 *            where we are
	 * @param m
	 *            the selected method for this option
	 * @param last
	 *            if this is the last in a multi single character option
	 * @return
	 */
	public String assign(Map<String, Object> paramaters, Method m, List<String> args, boolean last) {

		if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
			// We have a flag, e.g. an option not
			// followed by an argument
			// if the flag is present then we set it to
			// true.
			paramaters.put(getName(m), true);
		} else {
			if (!last)
				return "Not last in a set of 1-letter options but requires parameter";

			if (args.isEmpty())
				return "Option requires an argument but it is the last in the command line";

			String parameter = args.remove(0);

			// The option is followed by an argument
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				@SuppressWarnings("unchecked") Collection<Object> set = (Collection<Object>) paramaters
						.get(m.getName());
				if (set == null) {
					set = new ArrayList<Object>();
					paramaters.put(getName(m), set);
				}
				set.add(parameter);
			} else {
				if (paramaters.containsKey(m.getName()))
					return "The option can only occur once";

				paramaters.put(getName(m), parameter);
			}
			return null;
		}
		return null;
	}

	private String getName(Method m) {
		Config cfg = m.getAnnotation(Config.class);
		if (cfg == null || cfg.id() == null || cfg.id().equals(Config.NULL))
			return m.getName();

		return cfg.id();
	}

	/**
	 * Provide a help text.
	 */

	public void help(Class<?> specification) {
		out.format("%s\n", lastPart(specification.getName()));
		for (Method m : specification.getMethods()) {
			if (m.getName().startsWith("_"))
				continue;

			if (m.getName() == "help") {
				out.format("   [ --help ]\n");
				continue;
			}
			if (m.getName() == "_") {
				out.format("     ... \n");
				continue;
			}
			if (m.getName() == "__") {
				continue;
			}

			Config cfg = m.getAnnotation(Config.class);
			boolean required = cfg != null && cfg.required();
			boolean flag = m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class;
			String description = cfg != null ? cfg.description() : "";
			String name = getName(m);

			String type = "";
			if (!flag) {
				type = "<" + lastPart(m.getReturnType().getName().toLowerCase()) + ">";
			}
			out.format("   %s -%s, --%-20s %-12s%s %s\n",
			//
					required ? " " : "[", //
					name.charAt(0), //
					name, //
					type, //
					required ? " " : "]",//
					description);
		}
	}

	static Pattern	LAST_PART	= Pattern.compile(".*[\\$\\.]([^\\$\\.]+)");

	private static String lastPart(String name) {
		Matcher m = LAST_PART.matcher(name);
		if (m.matches())
			return m.group(1);
		else
			return name;
	}

	@SuppressWarnings("unchecked") public Object execute(Object target, String arg,
			List<String> args) throws Exception {

		if (!arg.equals("help")) {
			Method m = findMethod(arg, target);
			if (m != null) {
				Class<? extends Options> c = (Class<? extends Options>) m.getParameterTypes()[0];
				Options options = getopt(args, c);
				if (options == null)
					return null;

				m.setAccessible(true);
				return m.invoke(target, options);
			}
			out.format("Cannot find subcommand: %s\n", arg);
			help(target, null);
		} else {
			help(target, args);
		}
		return null;
	}

	Method findMethod(String arg, Object target) {
		arg = "_" + arg;
		for (Method m : target.getClass().getDeclaredMethods()) {
			if (m.getParameterTypes().length == 1 && m.getName().equals(arg)) {
				if (Options.class.isAssignableFrom(m.getParameterTypes()[0])) {
					return m;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked") public void help(Object target, Collection<String> cmds) {
		if (cmds == null || cmds.isEmpty()) {
			out.format("Available commands: ");
			String del = "";
			for (Method m : target.getClass().getMethods()) {
				if (m.getParameterTypes().length == 1 && m.getName().startsWith("_")) {
					out.format("%s%s", del, getCommandName(m));
					del = ", ";
				}
			}
			out.format("\n");
		} else {
			for (String cmd : cmds) {
				Method m = findMethod(cmd, target);
				if (m == null)
					out.format("No such command: %s\n", getCommandName(m));
				else {
					Class<? extends Options> c = (Class<? extends Options>) m.getParameterTypes()[0];
					help(c);
				}
			}
		}
	}

	public String getCommandName(Method m) {
		assert m.getName().startsWith("_");
		return m.getName().substring(1);
	}
}
