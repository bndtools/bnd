package aQute.libg.command;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import aQute.libg.reporter.*;

public class Command {

	boolean				trace;
	Reporter			reporter;
	List<String>		arguments	= new ArrayList<String>();
	Map<String,String>	variables	= new LinkedHashMap<String,String>();
	long				timeout		= 0;
	File				cwd			= new File("").getAbsoluteFile();
	static Timer		timer		= new Timer(Command.class.getName(), true);
	Process				process;
	volatile boolean	timedout;
	String				fullCommand;

	public Command(String fullCommand) {
		this.fullCommand = fullCommand;
	}

	public Command() {}

	public int execute(Appendable stdout, Appendable stderr) throws Exception {
		return execute((InputStream) null, stdout, stderr);
	}

	public int execute(String input, Appendable stdout, Appendable stderr) throws Exception {
		InputStream in = new ByteArrayInputStream(input.getBytes("UTF-8"));
		return execute(in, stdout, stderr);
	}

	public int execute(InputStream in, Appendable stdout, Appendable stderr) throws Exception {
		if (reporter != null) {
			reporter.trace("executing cmd: %s", arguments);
		}

		String args[] = arguments.toArray(new String[arguments.size()]);
		String vars[] = new String[variables.size()];
		int i = 0;
		for (Entry<String,String> s : variables.entrySet()) {
			vars[i++] = s.getKey() + "=" + s.getValue();
		}

		if (fullCommand == null)
			process = Runtime.getRuntime().exec(args, vars.length == 0 ? null : vars, cwd);
		else
			process = Runtime.getRuntime().exec(fullCommand, vars.length == 0 ? null : vars, cwd);

		// Make sure the command will not linger when we go
		Runnable r = new Runnable() {
			public void run() {
				process.destroy();
			}
		};
		Thread hook = new Thread(r, arguments.toString());
		Runtime.getRuntime().addShutdownHook(hook);
		TimerTask timer = null;
		OutputStream stdin = process.getOutputStream();

		if (timeout != 0) {
			timer = new TimerTask() {
				public void run() {
					timedout = true;
					process.destroy();
				}
			};
			Command.timer.schedule(timer, timeout);
		}

		InputStream out = process.getInputStream();
		try {
			InputStream err = process.getErrorStream();
			try {
				Collector cout = new Collector(out, stdout);
				cout.start();
				Collector cerr = new Collector(err, stderr);
				cerr.start();

				try {
					int c = in.read();
					while (c >= 0) {
						stdin.write(c);
						if (c == '\n')
							stdin.flush();
						c = in.read();
					}
				}
				catch (InterruptedIOException e) {
					// Ignore here
				}
				catch (Exception e) {
					// Who cares?
				}
				finally {
					stdin.close();
				}

				if (reporter != null)
					reporter.trace("exited process");

				cerr.join();
				cout.join();
				if (reporter != null)
					reporter.trace("stdout/stderr streams have finished");
			}
			finally {
				err.close();
			}
		}
		finally {
			out.close();
			if (timer != null)
				timer.cancel();
			Runtime.getRuntime().removeShutdownHook(hook);
		}

		byte exitValue = (byte) process.waitFor();
		if (reporter != null)
			reporter.trace("cmd %s executed with result=%d, result: %s/%s, timedout=%s", arguments, exitValue, stdout,
					stderr, timedout);

		if (timedout)
			return Integer.MIN_VALUE;

		return exitValue;
	}

	public void add(String... args) {
		for (String arg : args)
			arguments.add(arg);
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

		public void run() {
			try {
				int c = in.read();
				while (c >= 0) {
					sb.append((char) c);
					c = in.read();
				}
			}
			catch (IOException e) {
				// We assume the socket is closed
			}
			catch (Exception e) {
				try {
					sb.append("\n**************************************\n");
					sb.append(e.toString());
					sb.append("\n**************************************\n");
				}
				catch (IOException e1) {}
				if (reporter != null) {
					reporter.trace("cmd exec: %s", e);
				}
			}
		}
	}

	public Command var(String name, String value) {
		variables.put(name, value);
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
		for (Entry<String,String> e : pb.environment().entrySet()) {
			var(e.getKey(), e.getValue());
		}
	}

	public String var(String name) {
		return variables.get(name);
	}

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
}
