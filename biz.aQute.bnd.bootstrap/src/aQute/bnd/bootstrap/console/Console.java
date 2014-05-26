package aQute.bnd.bootstrap.console;

import java.io.*;
import java.util.concurrent.*;

import jline.console.*;

import org.apache.felix.service.command.*;

import aQute.bnd.annotation.component.*;

@Component(properties="main.thread=true")
public class Console implements Callable<Integer> {
	private CommandProcessor processor;
	private InputStream stdin = System.in;
	private PrintStream stdout = System.out;
	private PrintStream stderr = System.err;
	private boolean stopping;

	@Override
	public Integer call() throws Exception {
		final CommandSession session = processor.createSession(stdin, stdout,
				stderr);
		boolean quit = false;
		session.put("prompt", new Object() {
			public String toString() {
				return "bootstrap[" + session.get("_cwd") + "]$ ";
			};
		});
		String prompt;

		ConsoleReader consoleReader = new ConsoleReader(stdin, stdout);
		session.put("_console", consoleReader);

		while (!stopping) {
			try {
				Object promptObject = session.get("prompt");
				if (promptObject != null) {
					prompt = session.format(promptObject, Converter.LINE)
							.toString();
				} else {
					prompt = "bootstrap! ";
				}
				consoleReader.setPrompt(prompt);
				stdout.print(prompt);
				String line = consoleReader.readLine();
				Object result = session.execute(line);
				session.put("_", result); // set $_ to last result
				if (result != null) {
					stdout.println(session.format(result, Converter.INSPECT));
				}
			} catch (Exception e) {
				if (!quit) {
					session.put("exception", e);
					Object loc = session.get(".location");

					if (null == loc || !loc.toString().contains(":")) {
						loc = "bootstrap";
					}

					stdout.println(loc + ": " + e.getClass().getSimpleName()
							+ ": " + e.getMessage());
				}
			}
		}
		System.out.println("Stopped jline shell");
		consoleReader.shutdown();
		session.close();
		return 0;
	}

	@Reference
	void setCommand(CommandProcessor cp) {
		this.processor = cp;
	}
}
