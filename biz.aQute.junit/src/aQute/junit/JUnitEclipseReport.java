package aQute.junit;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
	private final BufferedReader	in;
	private final PrintWriter		out;
	private long					startTime;
	private List<Test>				tests;
	private boolean					verbose	= false;

	public JUnitEclipseReport(int port) throws Exception {
		Socket socket = null;
		ConnectException e = null;
		for (int i = 0; socket == null && i < 30; i++) {
			try {
				socket = new Socket(InetAddress.getByName(null), port);
			} catch (ConnectException ce) {
				e = ce;
				Thread.sleep(i * 100);
			}
		}
		if (socket == null) {
			System.err.println("Cannot open the JUnit Port: " + port + " " + e);
			System.exit(254);
			throw new AssertionError("unreachable");
		}

		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
	}

	@Override
	public void setup(Bundle fw, Bundle targetBundle) {}

	@Override
	public void begin(List<Test> tests, int realcount) {
		this.tests = tests;
		message("%TESTC  ", realcount + " v2");
		report(tests);
		startTime = System.currentTimeMillis();
	}

	@Override
	public void end() {
		message("%RUNTIME", "" + (System.currentTimeMillis() - startTime));
		out.flush();
		out.close();
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
		t.printStackTrace(out);
		out.println();
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

		out.print(key);
		out.println(payload);
		out.flush();
		if (verbose)
			System.err.println(key + payload);
	}

	private void message(String key, Test test) {
		if (tests == null)
			message(key, "?," + test);
		else
			message(key, (tests.indexOf(test) + 1) + "," + test);
	}

	private void report(List<Test> flattened) {
		for (int i = 0; i < flattened.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(i + 1);
			sb.append(",");
			Test test = flattened.get(i);
			if (test instanceof TestSuite || test instanceof JUnit4TestAdapter) {
				sb.append(test);
				sb.append(",");
				sb.append(true);
			} else {
				sb.append(test);
				sb.append(",");
				sb.append(false);

			}
			sb.append(",");
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
		try {
			in.close();
		} catch (Exception ioe) {
			// ignore
		}
	}

}
