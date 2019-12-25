package aQute.junit;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.osgi.framework.Bundle;

import junit.framework.AssertionFailedError;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class JUnitEclipseReport implements TestReporter {
	private long							startTime;
	private List<Test>						tests;
	private final boolean					verbose	= false;
	private Connection<Reader, PrintWriter>	junit;

	private static final class Connection<IN extends Closeable, OUT extends Closeable> implements Closeable {
		final Socket	sock;
		final IN		in;
		final OUT		out;

		Connection(Socket sock, IN in, OUT out) {
			this.sock = sock;
			this.in = in;
			this.out = out;
		}

		@Override
		public void close() {
			safeClose(out);
			safeClose(in);
			safeClose(sock);
		}
	}

	static void safeClose(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {}
	}

	public JUnitEclipseReport(int port) throws Exception {
		try {
			Socket socket = connectRetry(port);
			junit = new Connection<>(socket, new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8)),
				new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true));
		} catch (ConnectException e) {
			System.err.println("Cannot open the JUnit Port: " + port + " " + e);
			System.exit(254);
			throw new AssertionError("unreachable");
		}
	}

	private Socket connectRetry(int port) throws Exception {
		ConnectException e = null;
		for (int i = 0; i < 30; i++) {
			try {
				return new Socket(InetAddress.getByName(null), port);
			} catch (ConnectException ce) {
				e = ce;
				Thread.sleep(i * 100);
			}
		}
		if (e != null) {
			throw e;
		}
		return null;
	}

	@Override
	public void setup(Bundle fw, Bundle targetBundle) {}

	@Override
	public void begin(List<Test> tests, int realcount) {
		this.tests = tests;
		message("%TESTC  ", Integer.toString(realcount)
			.concat(" v2"));
		report(tests);
		startTime = System.currentTimeMillis();
	}

	@Override
	public void end() {
		message("%RUNTIME", Long.toString(System.currentTimeMillis() - startTime));
		junit.out.flush();
		safeClose(junit);
	}

	@Override
	public void addError(Test test, Throwable t) {
		message("%ERROR  ", test);
		trace(t);
	}

	@Override
	public void addFailure(Test test, AssertionFailedError t) {
		message("%FAILED ", test);
		trace(t);
	}

	void trace(Throwable t) {
		message("%TRACES ", "");
		t.printStackTrace(junit.out);
		if (verbose) {
			t.printStackTrace(System.err);
		}
		junit.out.println();
		message("%TRACEE ", "");
	}

	@Override
	public void endTest(Test test) {
		message("%TESTE  ", test);
	}

	@Override
	public void startTest(Test test) {
		message("%TESTS  ", test);
	}

	private void message(String key, String payload) {
		if (key.length() != 8)
			throw new IllegalArgumentException(key + " is not 8 characters");

		String message = key.concat(payload);
		junit.out.println(message);
		if (verbose)
			System.err.println(message);
	}

	private String getTestName(Test test) {
		return String.valueOf(test);
	}

	private void message(String key, Test test) {
		StringBuilder sb = new StringBuilder();
		if (tests == null) {
			sb.append('?');
		} else {
			sb.append(tests.indexOf(test) + 1);
		}
		sb.append(',');
		copyAndEscapeText(getTestName(test), sb);
		String payload = sb.toString();
		message(key, payload);
	}

	private void report(List<Test> flattened) {
		for (int i = 0; i < flattened.size(); i++) {
			Test test = flattened.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i + 1)
				.append(',');
			copyAndEscapeText(getTestName(test), sb);
			sb.append(',')
				.append(test instanceof TestSuite || test instanceof JUnit4TestAdapter)
				.append(',');
			if (test instanceof JUnit4TestAdapter) {
				sb.append(((JUnit4TestAdapter) test).getTests()
					.size());
			} else {
				sb.append(test.countTestCases());
			}
			message("%TSTTREE", sb.toString());
		}
	}

	@Override
	public void aborted() {
		end();
	}

	public void close() {
		safeClose(junit);
	}

	private static void copyAndEscapeText(String s, StringBuilder sb) {
		if (s.isEmpty()) {
			return;
		}
		if ((s.indexOf(',') < 0) && (s.indexOf('\\') < 0) && (s.indexOf('\r') < 0) && (s.indexOf('\n') < 0)) {
			sb.append(s);
			return;
		}
		for (int i = 0, len = s.length(); i < len; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\r' :
					int j;
					if ((j = i + 1) < len && s.charAt(j) == '\n') {
						i = j;
					}
					sb.append(' ');
					break;
				case '\n' :
					sb.append(' ');
					break;
				case ',' :
				case '\\' :
					sb.append('\\');
					sb.append(c);
					break;
				default :
					sb.append(c);
					break;
			}
		}
	}

}
