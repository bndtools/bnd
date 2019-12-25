package aQute.junit;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
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
	private Connection<Reader, PrintWriter>	client;

	private static final class Connection<IN, OUT> implements Closeable {
		final SocketChannel	channel;
		@SuppressWarnings("unused")
		final IN			in;
		final OUT			out;

		Connection(SocketChannel channel, IN in, OUT out) {
			this.channel = channel;
			this.in = in;
			this.out = out;
		}

		@Override
		public void close() {
			safeClose(channel);
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
			SocketChannel channel = connectRetry(port);
			if (verbose) {
				System.err.println("Opening streams for client connection " + channel);
			}
			client = new Connection<>(channel, new BufferedReader(Channels.newReader(channel, UTF_8.newDecoder(), -1)),
				new PrintWriter(Channels.newWriter(channel, UTF_8.newEncoder(), -1)));
		} catch (IOException e) {
			System.err.println("Cannot open the JUnit Port: " + port + " " + e);
			System.exit(254);
			throw new AssertionError("unreachable");
		}
	}

	private SocketChannel connectRetry(int port) throws Exception {
		IOException e = null;
		for (int i = 0; i < 30; i++) {
			try {
				SocketAddress address = new InetSocketAddress(InetAddress.getByName(null), port);
				SocketChannel channel = SocketChannel.open(address);
				channel.finishConnect();
				return channel;
			} catch (IOException ce) {
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
		client.out.flush();
		if (verbose) {
			System.err.println("Test run ended; waiting .25 seconds");
		}
		try {
			Thread.sleep(250L);
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
		}
		if (verbose) {
			System.err.println("Closing client connection");
		}
		safeClose(client);
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
		t.printStackTrace(client.out);
		if (verbose) {
			t.printStackTrace(System.err);
		}
		client.out.println();
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
		client.out.println(message);
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
		appendEscaped(getTestName(test), sb);
		String payload = sb.toString();
		message(key, payload);
	}

	private void report(List<Test> flattened) {
		for (int i = 0; i < flattened.size(); i++) {
			Test test = flattened.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i + 1)
				.append(',');
			appendEscaped(getTestName(test), sb);
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
		safeClose(client);
	}

	private static void appendEscaped(String s, StringBuilder sb) {
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
