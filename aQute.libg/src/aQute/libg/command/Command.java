package aQute.libg.command;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import aQute.libg.reporter.*;

public class Command {

	boolean				trace;
	Reporter			reporter;
	List<String>		arguments	= new ArrayList<String>();
	long				timeout		= 0;
	File				cwd			= new File("").getAbsoluteFile();
	static Timer		timer		= new Timer();
	Process				process;
	volatile boolean	timedout;

	public int execute(Appendable stdout, Appendable stderr) throws Exception {
		return execute((InputStream) null, stdout, stderr);
	}

	public int execute(String input, Appendable stdout, Appendable stderr) throws Exception {
		InputStream in = new ByteArrayInputStream(input.getBytes("UTF-8"));
		return execute(in, stdout, stderr);
	}

	public int execute(InputStream in, Appendable stdout, Appendable stderr) throws Exception {
		int result;
		if (reporter != null) {
			reporter.trace("executing cmd: %s", arguments);
		}

		String args[] = arguments.toArray(new String[arguments.size()]);

		process = Runtime.getRuntime().exec(args, null, cwd);

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
		final InputStreamHandler handler = in != null ? new InputStreamHandler(in, stdin) : null;
		
		if (timeout != 0) {
			timer = new TimerTask() {
				public void run() {
					timedout = true;
					process.destroy();
					if (handler != null)
						handler.interrupt();
				}
			};
			Command.timer.schedule(timer, timeout);
		}

		InputStream out = process.getInputStream();
		try {
			InputStream err = process.getErrorStream();
			try {
				new Collector(out, stdout).start();
				new Collector(err, stdout).start();
				if (handler != null)
					handler.start();

				result = process.waitFor();
			} finally {
				err.close();
			}
		} finally {
			out.close();
			if (timer != null)
				timer.cancel();
			Runtime.getRuntime().removeShutdownHook(hook);
			if (handler != null)
				handler.interrupt();
		}
		if (reporter != null)
			reporter.trace("cmd %s executed with result=%d, result: %s/%s", arguments, result,
					stdout, stderr);

		if( timedout )
			return Integer.MIN_VALUE;
		byte exitValue = (byte) process.exitValue();
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

		public Collector(InputStream inputStream, Appendable sb) {
			this.in = inputStream;
			this.sb = sb;
		}

		public void run() {
			try {
				int c = in.read();
				while (c >= 0) {
					sb.append((char) c);
					c = in.read();
				}
			} catch (Exception e) {
				try {
					sb.append("\n**************************************\n");
					sb.append(e.toString());
					sb.append("\n**************************************\n");
				} catch (IOException e1) {
				}
				if (reporter != null) {
					reporter.trace("cmd exec: %s", e);
				}
			}
		}
	}

	class InputStreamHandler extends Thread {
		final InputStream	in;
		final OutputStream	stdin;

		public InputStreamHandler(InputStream in, OutputStream stdin) {
			this.stdin = stdin;
			this.in = in;
		}

		public void run() {
			try {
				int c = in.read();
				while (c >= 0) {
					stdin.write(c);
					stdin.flush();
					c = in.read();
				}
			} catch (InterruptedIOException e) {
				// Ignore here
			} catch (Exception e) {
				// Who cares?
			}
		}
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
