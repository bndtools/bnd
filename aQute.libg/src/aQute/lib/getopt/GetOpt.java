package aQute.lib.getopt;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.configurable.*;

public class GetOpt {
	public static <T extends IGetOpt> T getopt(String args[], int first, Class<T> specification)
			throws SecurityException {
		Collection<String> value = new ArrayList<String>();
		Map<String, Object> line = new HashMap<String, Object>();
		argloop: for (int i = first; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
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
						for (++i; i < args.length; i++)
							value.add(args[i]);
						break argloop;
					}

					if ("--help".equals(arg) || "-h".equals(arg) || "-?".equals(arg)) {
						line.put("help", getHelp(specification));
						break argloop;
					}

					// Full named option, e.g. --output
					try {
						Method m = specification.getMethod(arg.substring(2));
						i = assign(line, m, args, i, true);
					} catch (NoSuchMethodException nsme) {
						throw new GetOptException("No such option " + arg, null, args, i);
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
								i = assign(line, m, args, i, (j + 1) >= arg.length());
								continue charloop;
							}
						}
						throw new GetOptException("No such option -" + c, null, args, i);
					}
				}
			} else
				value.add(arg);
		}

		// check if all required elements are set

		for (Method m : specification.getMethods()) {
			Config cfg = m.getAnnotation(Config.class);
			if (cfg != null && cfg.required() && !line.containsKey(getName(m)))
				throw new GetOptException("Required option not found in command", m, args, 0);
		}

		line.put(".", value);
		return Configurable.createConfigurable(specification, line);
	}

	/**
	 * Assign an option, must handle flags, parameters, and parameters that can
	 * happen multiple times.
	 * 
	 * @param line
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
	public static int assign(Map<String, Object> line, Method m, String args[], int i, boolean last) {

		if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
			// We have a flag, e.g. an option not
			// followed by an argument
			// if the flag is present then we set it to
			// true.
			line.put(getName(m), true);
		} else {
			if (!last)
				throw new GetOptException("Not last in a set of options but requires parameter", m,
						args, i);

			if (i >= args.length-1)
				throw new GetOptException(
						"Option requires an argument but it is the last in the command line", m,
						args, i);

			String parameter = args[++i];

			// The option is followed by an argument
			if (Collection.class.isAssignableFrom(m.getReturnType())) {
				@SuppressWarnings("unchecked") Collection<Object> set = (Collection<Object>) line
						.get(m.getName());
				if (set == null) {
					set = new ArrayList<Object>();
					line.put(getName(m), set);
				}
				set.add(parameter);
			} else {
				if (line.containsKey(m.getName()))
					throw new GetOptException("The option can only occur once", m, args, i);

				line.put(getName(m), parameter);
			}
			return i;
		}
		return i;
	}

	private static String getName(Method m) {
		Config cfg = m.getAnnotation(Config.class);
		if (cfg == null || cfg.id() == null || cfg.id().equals(Config.NULL))
			return m.getName();

		return cfg.id();
	}

	/**
	 * Provide a help text.
	 */

	static public String getHelp(Class<?> specification) {
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);

		f.format("%s\n", lastPart(specification.getName()));
		for (Method m : specification.getMethods()) {
			if (m.getName() == "help") {
				f.format("   [ --help ]\n");
				continue;
			}
			if (m.getName() == "_") {
				f.format(" ... \n");
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
			f.format("   %s -%s, --%-20s %-12s%s %s\n",
			//
					required ? " " : "[", //
					name.charAt(0), //
					name, //
					type, //
					required ? " " : "]",//
					description);
		}
		return sb.toString();
	}

	static Pattern	LAST_PART	= Pattern.compile(".*[\\$\\.]([^\\$\\.]+)");

	private static String lastPart(String name) {
		Matcher m = LAST_PART.matcher(name);
		if (m.matches())
			return m.group(1);
		else
			return name;
	}

	@SuppressWarnings("unchecked") public static Object subcmd(Object target, String[] args,
			int start, Appendable out) throws Exception {
		if (start < args.length) {
			String arg = args[start++];
			if (!arg.equals("help")) {
				arg = "_" + arg;
				for (Method m : target.getClass().getDeclaredMethods()) {
					if (m.getParameterTypes().length == 1 && m.getName().equals(arg)) {
						Class<? extends IGetOpt> specification = (Class<? extends IGetOpt>) m
								.getParameterTypes()[0];
						IGetOpt getopt = getopt(args, start, specification);
						if (getopt.help() != null) {
							out.append(getopt.help());
							out.append("\n");
							return null;
						} else
							return m.invoke(target, getopt);
					}
				}
				out.append("Cannot find subcommand: ");
				out.append(arg);
				out.append("\n");
			}

		}
		out.append("Available sub commands: ");
		String del = "";
		for (Method m : target.getClass().getDeclaredMethods()) {
			if (m.getParameterTypes().length == 1 && m.getName().startsWith("_")) {
				out.append(del);
				out.append(m.getName().toLowerCase().substring(1));
				del = ", ";
			}
		}
		out.append("\n");
		return null;
	}
}
