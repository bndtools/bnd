package aQute.bnd.main;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.justif.Justif;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.qtokens.QuotedTokenizer;
import jline.console.ConsoleReader;

public class Shell implements AutoCloseable {
	private bnd				bnd;
	private ShellOptions	options;

	public interface ShellOptions extends projectOptions {

	}

	public Shell(bnd bnd, ShellOptions options) {
		this.bnd = bnd;
		this.options = options;
	}

	/**
	 * Show the value of a macro
	 *
	 * @throws Exception
	 */
	@Description("Show macro value")
	public void loop() throws Exception {
		Processor domain = bnd.getProject(options.project());
		CommandLine cmdline = options._command();

		if (domain == null)
			domain = bnd.getWorkspace();

		if (domain == null) {
			domain = bnd;
		}

		ConsoleReader reader = new ConsoleReader();
		reader.setPrompt("> ");

		String line;
		PrintWriter out = new PrintWriter(reader.getOutput());
		out.println("Base " + domain);

		loop: while ((line = reader.readLine()) != null) {
			out.flush();

			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
				continue;

			QuotedTokenizer qt = new QuotedTokenizer(line, "= ,;", true);
			List<String> set = qt.getTokenSet();
			set.removeIf(arg -> arg.trim()
				.isEmpty());
			String[] args = set.toArray(new String[0]);

			switch (args[0]) {
				case "exit" :
				case "quit" :
					break loop;

				case "macros" : {
					Glob glob = new Glob(args.length == 1 ? "*" : args[1]);
					domain.getReplacer()
						.getCommands()
						.entrySet()
						.stream()
						.filter(e -> glob.matches(e.getKey()))
						.forEach(e -> {
							out.printf("%-20s %s%n", e.getKey(), e.getValue());
						});
				}
					break;

				case "commands" : {
					Glob glob = new Glob(args.length == 1 ? "*" : args[1]);
					cmdline.getCommands(bnd)
						.entrySet()
						.stream()
						.filter(e -> glob.matches(e.getKey()))
						.forEach(e -> {
							Method method = e.getValue();
							Description d = method.getAnnotation(Description.class);
							String help = d == null ? "" : d.value();
							out.printf("%-20s %s%n", e.getKey(), help);
						});
				}
					break;

				case "-help" :
				case "--help" :
				case "-h" :
				case "help" :
				case "?" :
				case "/help" :
					help(domain, out);
					out.println();
					Justif f = new Justif();
					cmdline.help(f.formatter(), bnd);
					out.println(f.wrap());
					break;

				default :
					List<String> input = new ArrayList<>(Arrays.asList(args));
					input.remove(0);

					if (args.length >= 3 && args[1].equals("=")) {
						String key = args[0];
						if (key.startsWith("$"))
							key = key.substring(1);
						input.remove(0);
						String value = Strings.join(" ", input);
						domain.setProperty(key, value);
					} else {
						if (cmdline.getCommands(bnd)
							.containsKey(args[0])) {
							try {
								String cmd = args[0];
								String help = cmdline.execute(bnd, cmd, input);
								if (help != null) {
									out.println(help);
								}
							} catch (Throwable t) {
								if (!(t instanceof Exception)) {
									throw Exceptions.duck(t);
								}
								t = Exceptions.unrollCause(t, InvocationTargetException.class);
								bnd.exception(t, "%s", t);
							}
						} else {
							bnd.out.println(process(domain.getReplacer(), line.trim()));
						}
					}
					break;
			}

			if (!domain.check() && !bnd.check()) {
				// help(domain, out);
			}
			domain.clear();
			bnd.clear();
		}

	}

	private void help(Processor domain, PrintWriter out) {
		out.printf("cmd args ...                      execute bnd command%n");
		out.printf("macro ( ';' arg )*                show a macro%n");
		out.printf("${macro ( ';' arg )*}             show a macro if macro name overlaps with bnd command%n");
		out.printf("<key>=<value>                     set a property%n");
		out.println();
		out.printf("exit                              exit%n");
		out.printf("macros [glob]                     show all macro names, optionally filtered by a glob%n");
		out.printf("commands [glob]                   show all bnd commands, optionally filtered by a glob%n");
		out.printf("help%n");
		out.println();
	}

	private String process(Macro r, String arg) {
		String s = arg;
		if (!s.startsWith("${")) {
			s = "${" + s;
		}
		if (!s.endsWith("}")) {
			s += "}";
		}
		s = s.replace(':', ';');
		String p = r.process(s);
		return p;
	}

	@Override
	public void close() throws Exception {}

}
