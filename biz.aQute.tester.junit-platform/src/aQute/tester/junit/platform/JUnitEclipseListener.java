package aQute.tester.junit.platform;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.ComparisonFailure;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.ValueWrapper;

public class JUnitEclipseListener implements TestExecutionListener, Closeable {
	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		visitEntry(testIdentifier, true);
	}

	// This is called if the execution is skipped without it having begun.
	// This contrasts to an assumption failure, which happens during a test
	// execution that has already started.
	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		info("JUnitEclipseListener: testPlanSkipped: " + testIdentifier + ", reason: " + reason);
		if (testIdentifier.isContainer() && testPlan != null) {
			testPlan.getChildren(testIdentifier)
				.forEach(identifier -> executionSkipped(identifier,
					"ancestor \"" + testIdentifier.getDisplayName() + "\" was skipped"));
		}
		// This is a departure from the Eclipse built-in JUnit 5 tester in two
		// ways (hopefully both improvements):
		// 1. Eclipse's version doesn't send skip notifications for containers.
		// I found that doing so causes Eclipse's JUnit GUI to show the
		// container as skipped - Eclipse's version doesn't show the container
		// as skipped, only the children.
		// 2. Eclipse handles "Ignore/Skip" and "AssumptionFailure" differently.
		// Reporting them all as assumption failures triggers the GUI to display
		// the skip reason in the failure trace, which the Eclipse
		// implementation doesn't do.
		if (!testIdentifier.isContainer()) {
			message("%TESTS  ", testIdentifier);
		}
		message("%FAILED ", testIdentifier, "@AssumptionFailure: ");
		message("%TRACES ");
		info(() -> "JUnitEclipseListener: Skipped: " + reason);
		out.println("Skipped: " + reason);
		message("%TRACEE ");
		if (!testIdentifier.isContainer()) {
			message("%TESTE  ", testIdentifier);
		}
	}

	private final Socket			controlSock;
	private final OutputStream		controlOut;
	private final DataInputStream	controlIn;
	private Socket					sock;
	private BufferedReader			in;
	private PrintWriter				out;
	private long					startTime;
	private TestPlan				testPlan;
	private boolean					verbose	= false;

	public JUnitEclipseListener(int port) throws Exception {
		this(port, false);
	}

	private Socket connectRetry(int port) throws Exception {
		Socket socket = null;
		ConnectException e = null;
		for (int i = 0; socket == null && i < 30; i++) {
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

	public JUnitEclipseListener(int port, boolean rerunIDE) throws Exception {
		try {
			Socket socket = connectRetry(port);

			if (rerunIDE) {
				controlSock = socket;
				controlIn = new DataInputStream(socket.getInputStream());
				controlOut = socket.getOutputStream();
			} else {
				controlSock = null;
				controlIn = null;
				controlOut = null;
				setupJUnitSocket(socket);
			}
		} catch (ConnectException e) {
			info("JUnitEclipseListener: Cannot open the JUnit control Port: " + port + " " + e);
			System.exit(254);
			throw new AssertionError("unreachable");
		}
	}

	private void setupJUnitSocket(Socket junitSocket) throws IOException {
		info("Opening streams");
		sock = junitSocket;
		in = new BufferedReader(new InputStreamReader(junitSocket.getInputStream(), UTF_8));
		out = new PrintWriter(new OutputStreamWriter(junitSocket.getOutputStream(), UTF_8));
	}

	private void setNullStreams() {
		in = new BufferedReader(new Reader() {
			@Override
			public int read(char[] cbuf, int off, int len) {
				return -1;
			}

			@Override
			public void close() {}
		});
		out = new PrintWriter(new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) {}

			@Override
			public void flush() throws IOException {}

			@Override
			public void close() throws IOException {}
		});
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		info("testPlanExecution started, closing existing connection (if any)");
		if (controlSock != null) {
			safeClose(in);
			safeClose(out);
			safeClose(sock);
			try {
				info("Notifying controller that we're starting a new session");
				controlOut.write(1);
				controlOut.flush();
				info("Retrieving new JUnit port");
				int junitPort = controlIn.readInt();
				info(() -> "Attempting to connect to port: " + junitPort);
				Socket junitSocket = connectRetry(junitPort);

				setupJUnitSocket(junitSocket);
			} catch (Exception e) {
				System.err.println("Error trying to connect to JUnit session: " + e);
				info("Setting up dummy io");
				setNullStreams();
			}
		}

		final long realCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);
		info("JUnitEclipseListener: testPlanExecutionStarted: " + testPlan + ", realCount: " + realCount);
		message("%TESTC  ", realCount + " v2");
		this.testPlan = testPlan;
		for (TestIdentifier root : testPlan.getRoots()) {
			for (TestIdentifier child : testPlan.getChildren(root)) {
				visitEntry(child);
			}
		}
		startTime = System.currentTimeMillis();
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		message("%RUNTIME", "" + (System.currentTimeMillis() - startTime));
		out.flush();
		if (controlSock == null) {
			setNullStreams();
		}
	}

	private void sendTrace(Throwable t) {
		message("%TRACES ");
		t.printStackTrace(out);
		if (verbose) {
			t.printStackTrace(System.err);
		}
		out.println();
		message("%TRACEE ");
	}

	private void sendExpectedAndActual(CharSequence expected, CharSequence actual) {
		message("%EXPECTS");
		out.println(expected);
		info(expected);
		message("%EXPECTE");

		message("%ACTUALS");
		out.println(actual);
		info(actual);
		message("%ACTUALE");
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		info("JUnitEclipseListener: Execution started: " + testIdentifier);
		if (testIdentifier.isTest()) {
			message("%TESTS  ", testIdentifier);
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		info("JUnitEclipseListener: Execution finished: " + testIdentifier);
		Status result = testExecutionResult.getStatus();
		if (testIdentifier.isTest()) {
			if (result != Status.SUCCESSFUL) {
				final boolean assumptionFailed = result == Status.ABORTED;
				info("JUnitEclipseListener: assumption failed: " + assumptionFailed);
				Optional<Throwable> throwableOp = testExecutionResult.getThrowable();
				if (throwableOp.isPresent()) {
					Throwable exception = throwableOp.get();
					info("JUnitEclipseListener: throwable: " + exception);

					if (assumptionFailed || exception instanceof AssertionError) {
						info(() -> "JUnitEclipseListener: failed: " + exception + " assumptionFailed: "
							+ assumptionFailed);
						message("%FAILED ", testIdentifier, (assumptionFailed ? "@AssumptionFailure: " : ""));

						sendExpectedAndActual(exception);

					} else {
						info("JUnitEclipseListener: error");
						message("%ERROR  ", testIdentifier);
					}
					sendTrace(exception);
				}
			}
			message("%TESTE  ", testIdentifier);
		} else { // container
			if (result != Status.SUCCESSFUL) {
				message("%ERROR  ", testIdentifier);
				Optional<Throwable> throwableOp = testExecutionResult.getThrowable();
				if (throwableOp.isPresent()) {
					sendTrace(throwableOp.get());
				}
			}
		}
	}

	private static void appendString(StringBuilder b, String s) {
		if (b.length() > 0) {
			b.append("\n\n");
		}
		b.append(s);
	}

	private boolean sendExpectedAndActual(Throwable exception, StringBuilder expectedBuilder,
		StringBuilder actualBuilder) {
		BooleanSupplier action;
		// NOTE:
		// 1. switch is based on the class name rather than using instanceof
		// or class literals, to avoid hard dependency on the assertion types.
		// 2. the individual case blocks are lambdas rather than inlined code -
		// this too is done on purpose to make sure that JUnitEclipseListener
		// doesn't have a hard dependency on any of the assertion classes (the
		// JUnit 3/4 comparison failure assertions in particular).
		switch (exception.getClass()
			.getName()) {
			case "org.opentest4j.AssertionFailedError" :
				action = () -> {
					AssertionFailedError assertionFailedError = (AssertionFailedError) exception;
					ValueWrapper expected = assertionFailedError.getExpected();
					ValueWrapper actual = assertionFailedError.getActual();
					if (expected == null || actual == null) {
						return false;
					}
					appendString(expectedBuilder, expected.getStringRepresentation());
					appendString(actualBuilder, actual.getStringRepresentation());
					return true;
				};
				break;
			case "org.junit.ComparisonFailure" :
				action = () -> {
					ComparisonFailure comparisonFailure = (ComparisonFailure) exception;
					String expected = comparisonFailure.getExpected();
					String actual = comparisonFailure.getActual();
					if (expected == null || actual == null) {
						return false;
					}
					appendString(expectedBuilder, expected);
					appendString(actualBuilder, actual);
					return true;
				};
				break;
			case "junit.framework.ComparisonFailure" :
				action = () -> {
					junit.framework.ComparisonFailure comparisonFailure = (junit.framework.ComparisonFailure) exception;
					String expected = comparisonFailure.getExpected();
					String actual = comparisonFailure.getActual();
					if (expected == null || actual == null) {
						return false;
					}
					appendString(expectedBuilder, expected);
					appendString(actualBuilder, actual);
					return true;
				};
				break;
			case "org.opentest4j.MultipleFailuresError" :
				action = () -> {
					List<Throwable> failures = ((MultipleFailuresError) exception).getFailures();
					return failures.stream()
						.filter(failure -> sendExpectedAndActual(failure, expectedBuilder, actualBuilder))
						.count() > 0;
				};
				break;
			default :
				return false;
		}
		return action.getAsBoolean();
	}

	private void sendExpectedAndActual(Throwable exception) {
		final StringBuilder expected = new StringBuilder();
		final StringBuilder actual = new StringBuilder();
		if (sendExpectedAndActual(exception, expected, actual)) {
			sendExpectedAndActual(expected, actual);
		}
	}

	private void message(String key) {
		message(key, "");
	}

	private void message(String key, CharSequence payload) {
		if (key.length() != 8)
			throw new IllegalArgumentException(key + " is not 8 characters");

		out.print(key);
		out.println(payload);
		out.flush();
		info("JUnitEclipseListener: " + key + payload);
	}

	private AtomicInteger		counter	= new AtomicInteger(1);
	private Map<String, String>	idMap	= new HashMap<>();

	private String getTestId(String junitId) {
		String id = idMap.computeIfAbsent(junitId, k -> Integer.toString(counter.getAndIncrement()));
		return id;
	}

	private String getTestId(TestIdentifier test) {
		return getTestId(test.getUniqueId());
	}

	private String getTestName(TestIdentifier test) {
		return test.getSource()
			.map(this::getTestName)
			.orElseGet(test::getDisplayName);
	}

	private String getTestName(TestSource testSource) {
		if (testSource instanceof ClassSource) {
			return ((ClassSource) testSource).getJavaClass()
				.getName();
		}
		if (testSource instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) testSource;
			return MessageFormat.format("{0}({1})", methodSource.getMethodName(), methodSource.getClassName());
		}
		return null;
	}

	private String getTestParameterTypes(TestSource testSource) {
		if (testSource instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) testSource;
			return methodSource.getMethodParameterTypes();
		}
		return "";
	}

	private void message(String key, TestIdentifier test) {
		message(key, test, "");
	}

	// namePrefix is used as a special case to signal ignored and aborted tests.
	private void message(String key, TestIdentifier test, String namePrefix) {
		final StringBuilder sb = new StringBuilder(100);
		sb.append(getTestId(test))
			.append(',');
		copyAndEscapeText(namePrefix + getTestName(test), sb);
		message(key, sb);
	}

	// This is mostly copied from
	// org.eclipse.jdt.internal.junit.runner.RemoteTestRunner except that
	// StringBuffer local has been replaced with StringBuilder parameter.
	public static void copyAndEscapeText(String s, StringBuilder sb) {
		if ((s.indexOf(',') < 0) && (s.indexOf('\\') < 0) && (s.indexOf('\r') < 0) && (s.indexOf('\n') < 0)) {
			sb.append(s);
			return;
		}
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ',') {
				sb.append("\\,"); //$NON-NLS-1$
			} else if (c == '\\') {
				sb.append("\\\\"); //$NON-NLS-1$
			} else if (c == '\r') {
				if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
					i++;
				}
				sb.append(' ');
			} else if (c == '\n') {
				sb.append(' ');
			} else {
				sb.append(c);
			}
		}
	}

	private void visitEntry(TestIdentifier test) {
		visitEntry(test, false);
	}

	private void visitEntry(TestIdentifier test, boolean isDynamic) {
		StringBuilder treeEntry = new StringBuilder();
		treeEntry.append(getTestId(test))
			.append(',');
		copyAndEscapeText(getTestName(test), treeEntry);
		if (test.isTest()) {
			treeEntry.append(",false,1,")
				.append(isDynamic)
				.append(',')
				.append(test.getParentId()
					.map(this::getTestId)
					.orElse("-1"))
				.append(',');
			copyAndEscapeText(test.getDisplayName(), treeEntry);
			treeEntry.append(',');
			copyAndEscapeText(test.getSource()
				.map(this::getTestParameterTypes)
				.orElse(""), treeEntry);
			treeEntry.append(',');
			copyAndEscapeText(test.getUniqueId(), treeEntry);
			message("%TSTTREE", treeEntry);
		} else {
			final Set<TestIdentifier> children = testPlan.getChildren(test);
			treeEntry.append(",true,")
				.append(children.size())
				.append(',')
				.append(isDynamic)
				.append(',')
				.append(test.getParentId()
					.map(this::getTestId)
					.orElse("-1"))
				.append(',');
			copyAndEscapeText(test.getDisplayName(), treeEntry);
			treeEntry.append(',');
			copyAndEscapeText(test.getSource()
				.map(this::getTestParameterTypes)
				.orElse(""), treeEntry); //$NON-NLS-1$
			treeEntry.append(',');
			copyAndEscapeText(test.getUniqueId(), treeEntry);
			message("%TSTTREE", treeEntry);
			for (TestIdentifier child : children) {
				visitEntry(child, isDynamic);
			}
		}
	}

	private void safeClose(Closeable io) {
		if (io == null) {
			return;
		}
		try {
			io.close();
		} catch (IOException e) {}
	}

	@Override
	public void close() {
		info(() -> idMap.entrySet()
			.stream()
			.map(entry -> entry.getKey() + " => " + entry.getValue())
			.collect(Collectors.joining(",\n")));
		safeClose(in);
		safeClose(out);
		safeClose(sock);
		safeClose(controlIn);
		safeClose(controlOut);
		safeClose(controlSock);
	}

	private void info(Supplier<CharSequence> message) {
		if (verbose) {
			info(message.get());
		}
	}

	private void info(CharSequence message) {
		if (verbose) {
			System.err.println(message);
		}
	}
}
