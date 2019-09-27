package aQute.libg.command;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class Command {
	private final static Logger	logger		= LoggerFactory.getLogger(Command.class);
	private final static int	TIMEDOUT	= 126 - 3;

	boolean						trace;
	Reporter					reporter;
	List<String>				arguments	= new ArrayList<>();
	Map<String, String>			variables	= new LinkedHashMap<>();
	long						timeout		= 0;
	File						cwd			= new File("").getAbsoluteFile();
	volatile Process			process;
	volatile boolean			timedout;
	String						fullCommand;
	private boolean				useThreadForInput;

	public Command(String fullCommand) {
		this.fullCommand = fullCommand;
	}

	public Command() {}

	public int execute(Appendable stdout, Appendable stderr) throws Exception {
		return execute((InputStream) null, stdout, stderr);
	}

	public int execute(String input, Appendable stdout, Appendable stderr) throws Exception {
		InputStream in = IO.stream(input, UTF_8);
		return execute(in, stdout, stderr);
	}

	public static boolean needsWindowsQuoting(String s) {
		int len = s.length();
		if (len == 0) // empty string have to be quoted
			return true;
		for (int i = 0; i < len; i++) {
			switch (s.charAt(i)) {
				case ' ' :
				case '\t' :
				case '\\' :
				case '"' :
					return true;
			}
		}
		return false;
	}

	public static String windowsQuote(String s) {
		if (!needsWindowsQuoting(s))
			return s;
		s = s.replaceAll("([\\\\]*)\"", "$1$1\\\\\"");
		s = s.replaceAll("([\\\\]*)\\z", "$1$1");
		return "\"" + s + "\"";
	}

	public int execute(final InputStream in, Appendable stdout, Appendable stderr) throws Exception {
		logger.debug("executing cmd: {}", arguments);

		ProcessBuilder p;
		if (fullCommand != null) {
			// TODO do proper splitting
			p = new ProcessBuilder(fullCommand.split("\\s+"));
		} else {
			// [cs] Arguments on windows aren't processed correctly. Thus the
			// below junk
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6511002

			if (System.getProperty("os.name")
				.startsWith("Windows")) {
				List<String> adjustedStrings = new LinkedList<>();
				for (String a : arguments) {
					adjustedStrings.add(windowsQuote(a));
				}
				p = new ProcessBuilder(adjustedStrings);
			} else {
				p = new ProcessBuilder(arguments);
			}
		}

		Map<String, String> env = p.environment();
		for (Entry<String, String> s : variables.entrySet()) {
			env.put(s.getKey(), s.getValue());
		}

		p.directory(cwd);
		if (in == System.in)
			p.redirectInput(ProcessBuilder.Redirect.INHERIT);
		Process process = this.process = p.start();

		// Make sure the command will not linger when we go
		Thread hook = new Thread(() -> process.destroy(), arguments.toString());
		Runtime.getRuntime()
			.addShutdownHook(hook);
		final OutputStream stdin = process.getOutputStream();
		Thread rdInThread = null;

		ScheduledExecutorService scheduler = null;
		if (timeout != 0) {
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.schedule(() -> {
				timedout = true;
				process.destroy();
			}, timeout, TimeUnit.MILLISECONDS);
		}

		final AtomicBoolean finished = new AtomicBoolean(false);
		try (InputStream out = process.getInputStream(); InputStream err = process.getErrorStream()) {
			Collector cout = new Collector(out, stdout);
			cout.start();
			Collector cerr = new Collector(err, stderr);
			cerr.start();

			if (in != null) {
				if (in == System.in || useThreadForInput) {
					rdInThread = new Thread("Read Input Thread") {
						@Override
						public void run() {
							try {
								while (!finished.get()) {
									int n = in.available();
									if (n == 0) {
										sleep(100);
									} else {
										int c = in.read();
										if (c < 0) {
											stdin.close();
											return;
										}
										stdin.write(c);
										if (c == '\n')
											stdin.flush();
									}
								}
							} catch (InterruptedIOException e) {
								// Ignore here
							} catch (Exception e) {
								// Who cares?
							} finally {
								IO.close(stdin);
							}
						}
					};
					rdInThread.setDaemon(true);
					rdInThread.start();
				} else {
					IO.copy(in, stdin);
					stdin.close();
				}
			}
			logger.debug("exited process");

			cerr.join();
			cout.join();
			logger.debug("stdout/stderr streams have finished");
		} finally {
			if (scheduler != null) {
				scheduler.shutdownNow();
			}
			Runtime.getRuntime()
				.removeShutdownHook(hook);
		}

		int exitValue = process.waitFor();
		finished.set(true);
		if (rdInThread != null) {
			if (in != System.in)
				IO.close(in);
			rdInThread.interrupt();
		}

		logger.debug("cmd {} executed with result={}, result: {}/{}, timedout={}", arguments, exitValue, stdout, stderr,
			timedout);

		if (timedout)
			return TIMEDOUT;

		return exitValue;
	}

	public void add(String arg) {
		arguments.add(arg);
	}

	public void add(String... args) {
		Collections.addAll(arguments, args);
	}

	public void addAll(Collection<String> args) {
		arguments.addAll(args);
	}

	public void setTimeout(long duration, TimeUnit unit) {
		timeout = unit.toMillis(duration);
	}

	public void setTrace() {
		this.trace = true;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public void setCwd(File dir) {
		if (!dir.isDirectory())
			throw new IllegalArgumentException("Working directory must be a directory: " + dir);

		this.cwd = dir;
	}

	public void cancel() {
		process.destroy();
	}

	class Collector extends Thread {
		final InputStream	in;
		final Appendable	sb;

		Collector(InputStream inputStream, Appendable sb) {
			this.in = inputStream;
			this.sb = sb;
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				int c = in.read();
				while (c >= 0) {
					sb.append((char) c);
					c = in.read();
				}
			} catch (IOException e) {
				// We assume the socket is closed
			} catch (Exception e) {
				try {
					sb.append("\n**************************************\n");
					sb.append(e.toString());
					sb.append("\n**************************************\n");
				} catch (IOException e1) {}
				logger.debug("cmd exec", e);
			}
		}
	}

	public Command var(String name, String value) {
		variables.put(name, value);
		return this;
	}

	public Command arg(String arg) {
		add(arg);
		return this;
	}

	public Command arg(String... args) {
		add(args);
		return this;
	}

	public Command full(String full) {
		fullCommand = full;
		return this;
	}

	public void inherit() {
		ProcessBuilder pb = new ProcessBuilder();
		for (Entry<String, String> e : pb.environment()
			.entrySet()) {
			var(e.getKey(), e.getValue());
		}
	}

	public String var(String name) {
		return variables.get(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String del = "";

		for (String argument : arguments) {
			sb.append(del);
			sb.append(argument);
			del = " ";
		}
		return sb.toString();
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setUseThreadForInput(boolean useThreadForInput) {
		this.useThreadForInput = useThreadForInput;
	}

	public void var(Map<String, String> env) {
		for (Map.Entry<String, String> e : env.entrySet()) {
			var(e.getKey(), e.getValue());
		}
	}
}
