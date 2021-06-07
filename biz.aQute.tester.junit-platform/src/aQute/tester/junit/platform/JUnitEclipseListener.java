package aQute.tester.junit.platform;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
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

public class JUnitEclipseListener implements TestExecutionListener, Closeable {

	static final Predicate<Throwable>	JUNIT3_COMPARISON_FAILURE;
	static final Predicate<Throwable>	JUNIT4_COMPARISON_FAILURE;
	static final Predicate<Throwable>	ASSERTION_FAILED_ERROR;
	static final Predicate<Throwable>	MULTIPLE_FAILURES_ERROR;

	@SuppressWarnings("unchecked")
	static Predicate<Throwable> tryLoad(String name) {
		try {
			return Class.forName(name)::isInstance;
		} catch (ClassNotFoundException cnfe) {
			return x -> false;
		}
	}

	static {
		JUNIT3_COMPARISON_FAILURE = tryLoad("junit.framework.ComparisonFailure");
		JUNIT4_COMPARISON_FAILURE = tryLoad("org.junit.ComparisonFailure");
		ASSERTION_FAILED_ERROR = tryLoad("org.opentest4j.AssertionFailedError");
		MULTIPLE_FAILURES_ERROR = tryLoad("org.opentest4j.MultipleFailuresError");
	}

	static class ExpectedActualBuilder implements Predicate<Throwable> {

		StringBuilder	expectedBuilder	= new StringBuilder();
		StringBuilder	actualBuilder	= new StringBuilder();
		boolean			first			= true;

		void addExpectedActual(String expected, String actual) {
			if (first) {
				first = false;
			} else {
				expectedBuilder.append("\n\n");
				actualBuilder.append("\n\n");
			}
			expectedBuilder.append(expected);
			actualBuilder.append(actual);
		}

		@Override
		public boolean test(Throwable exception) {
			// NOTE:
			// The code in this method is carefully structured to avoid
			// NoClassDefFoundError. The null test for each if first
			// confirms that the class is available before proceeding into
			// the block, so that by that stage it knows that the class
			// is available and that NCDFE will not be thrown. On the other
			// hand, if the class is not on the classpath then the code
			// will never be executed and the thread context classloader
			// will not attempt to load the class, so again no NCDFE.
			//
			// This is particularly important for the JUnit 3/4 comparison
			// failure assertions, because the OpenTest4J ones will be on the
			// classpath courtesy of junit-platform-launcher's dependencies.
			if (ASSERTION_FAILED_ERROR.test(exception)) {
				AssertionFailedError assertionFailedError = (AssertionFailedError) exception;
				if (assertionFailedError.isExpectedDefined() && assertionFailedError.isActualDefined()) {
					addExpectedActual(assertionFailedError.getExpected()
						.getStringRepresentation(),
						assertionFailedError.getActual()
							.getStringRepresentation());
					return true;
				}
				return false;
			} else if (JUNIT4_COMPARISON_FAILURE.test(exception)) {
				ComparisonFailure comparisonFailure = (ComparisonFailure) exception;
				String expected = comparisonFailure.getExpected();
				String actual = comparisonFailure.getActual();
				if ((expected != null) && (actual != null)) {
					addExpectedActual(expected, actual);
					return true;
				}
				return false;
			} else if (JUNIT3_COMPARISON_FAILURE.test(exception)) {
				junit.framework.ComparisonFailure comparisonFailure = (junit.framework.ComparisonFailure) exception;
				String expected = comparisonFailure.getExpected();
				String actual = comparisonFailure.getActual();
				if ((expected != null) && (actual != null)) {
					addExpectedActual(expected, actual);
					return true;
				}
				return false;
			} else if (MULTIPLE_FAILURES_ERROR.test(exception)) {
				List<Throwable> failures = ((MultipleFailuresError) exception).getFailures();
				return failures.stream()
					.filter(this)
					.count() > 0;
			}
			return false;
		}

		public CharSequence getExpected() {
			return expectedBuilder;
		}

		public CharSequence getActual() {
			return actualBuilder;
		}
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		info("JUnitEclipseListener: dynamicTestRegistered: %s", testIdentifier);
		visitEntry(testIdentifier, true);
	}

	// This is called if the execution is skipped without it having begun.
	// This contrasts to an assumption failure, which happens during a test
	// execution that has already started.
	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		info("JUnitEclipseListener: testPlanSkipped: %s, reason: %s", testIdentifier, reason);
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
		synchronized (client.out) {
			if (!testIdentifier.isContainer()) {
				message("%TESTS  ", testIdentifier);
			}
			message("%FAILED ", testIdentifier, "@AssumptionFailure: ");
			message("%TRACES ");
			info("JUnitEclipseListener: Skipped: %s", reason);
			client.out.println("Skipped: " + reason);
			message("%TRACEE ");
			if (!testIdentifier.isContainer()) {
				message("%TESTE  ", testIdentifier);
			}
		}
	}

	private final Connection<DataInputStream, OutputStream>	control;
	private Connection<Reader, PrintWriter>					client;
	private long											startNanos;
	private TestPlan										testPlan;
	private final boolean									verbose	= false;

	private static final class Connection<IN, OUT> implements Closeable {
		final SocketChannel	channel;
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

	public JUnitEclipseListener(int port) throws Exception {
		this(port, false);
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

	public JUnitEclipseListener(int port, boolean rerunIDE) throws Exception {
		try {
			SocketChannel channel = connectRetry(port);
			if (rerunIDE) {
				control = new Connection<>(channel, new DataInputStream(Channels.newInputStream(channel)),
					Channels.newOutputStream(channel));
				client = null;
			} else {
				control = null;
				client = junitConnection(channel);
			}
		} catch (IOException e) {
			info("JUnitEclipseListener: Cannot open the JUnit Port: %s %s", port, e);
			System.exit(254);
			throw new AssertionError("unreachable");
		}
	}

	private Connection<Reader, PrintWriter> junitConnection(SocketChannel channel) throws IOException {
		info("JUnitEclipseListener: Opening streams for client connection %s", channel);
		return new Connection<>(channel, new BufferedReader(Channels.newReader(channel, UTF_8.newDecoder(), -1)),
			new PrintWriter(Channels.newWriter(channel, UTF_8.newEncoder(), -1)));
	}

	private Connection<Reader, PrintWriter> nullConnection() {
		return new Connection<>(null, new Reader() {
			@Override
			public int read(char[] cbuf, int off, int len) {
				return -1;
			}

			@Override
			public void close() {}
		}, new PrintWriter(new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) {}

			@Override
			public void flush() {}

			@Override
			public void close() {}
		}));
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		if (control != null) {
			info("JUnitEclipseListener: testPlanExecutionStarted: Closing client connection, if any");
			safeClose(client);
			try {
				info("Notifying controller that we're starting a new session");
				control.out.write(1);
				control.out.flush();
				info("Retrieving new JUnit port");
				int junitPort = control.in.readInt();
				info("Attempting to connect to port: %s", junitPort);
				SocketChannel channel = connectRetry(junitPort);
				client = junitConnection(channel);
			} catch (Exception e) {
				System.err.println("Error trying to connect to JUnit session: " + e);
				info("Setting up dummy io");
				client = nullConnection();
			}
		}

		final long realCount = testPlan.countTestIdentifiers(TestIdentifier::isTest);
		synchronized (client.out) {
			info("JUnitEclipseListener: testPlanExecutionStarted: %s, realCount: %s", testPlan, realCount);
			message("%TESTC  ", Long.toString(realCount)
				.concat(" v2"));
			this.testPlan = testPlan;
			for (TestIdentifier root : testPlan.getRoots()) {
				for (TestIdentifier child : testPlan.getChildren(root)) {
					visitEntry(child, false);
				}
			}
		}
		startNanos = System.nanoTime();
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		message("%RUNTIME", Long.toString(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)));
		info("JUnitEclipseListener: testPlanExecutionFinished: Waiting .25 seconds");
		try {
			Thread.sleep(250L);
		} catch (InterruptedException e) {
			Thread.currentThread()
				.interrupt();
		}
		if (control == null) {
			info("JUnitEclipseListener: testPlanExecutionFinished: Closing client connection");
			safeClose(client);
			client = nullConnection();
		}
	}

	private void sendTrace(Throwable t) {
		message("%TRACES ");
		t.printStackTrace(client.out);
		if (verbose) {
			t.printStackTrace(System.err);
		}
		client.out.println();
		message("%TRACEE ");
	}

	private void sendExpectedAndActual(CharSequence expected, CharSequence actual) {
		message("%EXPECTS");
		client.out.println(expected);
		info(expected);
		message("%EXPECTE");

		message("%ACTUALS");
		client.out.println(actual);
		info(actual);
		message("%ACTUALE");
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		info("JUnitEclipseListener: Execution started: %s", testIdentifier);
		if (testIdentifier.isTest()) {
			message("%TESTS  ", testIdentifier);
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		synchronized (client.out) {
			info("JUnitEclipseListener: Execution finished: %s", testIdentifier);
			Status result = testExecutionResult.getStatus();
			if (testIdentifier.isTest()) {
				if (result != Status.SUCCESSFUL) {
					final boolean assumptionFailed = result == Status.ABORTED;
					info("JUnitEclipseListener: assumption failed: %s", assumptionFailed);
					Optional<Throwable> throwableOp = testExecutionResult.getThrowable();
					throwableOp.ifPresent(exception -> {
						info("JUnitEclipseListener: throwable: %s", exception);
						if (assumptionFailed || exception instanceof AssertionError) {
							info("JUnitEclipseListener: failed: %s assumptionFailed: %s", exception, assumptionFailed);
							message("%FAILED ", testIdentifier, (assumptionFailed ? "@AssumptionFailure: " : ""));
							sendExpectedAndActual(exception);
						} else {
							info("JUnitEclipseListener: error");
							message("%ERROR  ", testIdentifier);
						}
						sendTrace(exception);
					});
				}
				message("%TESTE  ", testIdentifier);
			} else { // container
				if (result != Status.SUCCESSFUL) {
					message("%ERROR  ", testIdentifier);
					Optional<Throwable> throwableOp = testExecutionResult.getThrowable();
					throwableOp.ifPresent(this::sendTrace);
				}
			}
		}
	}

	private void sendExpectedAndActual(Throwable exception) {
		ExpectedActualBuilder eab = new ExpectedActualBuilder();
		if (eab.test(exception)) {
			sendExpectedAndActual(eab.getExpected(), eab.getActual());
		}
	}

	private void message(String key) {
		message(key, "");
	}

	private void message(String key, CharSequence payload) {
		if (key.length() != 8)
			throw new IllegalArgumentException(key + " is not 8 characters");

		String message = key.concat(payload.toString());
		synchronized (client.out) {
			client.out.println(message);
			client.out.flush();
			info("JUnitEclipseListener: %s", message);
		}
	}

	private final AtomicInteger					counter	= new AtomicInteger();
	private final ConcurrentMap<String, String>	idMap	= new ConcurrentHashMap<>();

	private String getTestId(String uniqueId) {
		String testId = idMap.computeIfAbsent(uniqueId, id -> Integer.toString(counter.incrementAndGet()));
		return testId;
	}

	private String getTestId(TestIdentifier testIdentifier) {
		return getTestId(testIdentifier.getUniqueId());
	}

	private String getParentTestId(TestIdentifier testIdentifier) {
		return testPlan.getParent(testIdentifier)
			.map(this::getTestId)
			.orElse("-1");
	}

	private String getTestName(TestIdentifier testIdentifier) {
		return testIdentifier.getSource()
			.map(this::getTestName)
			.orElseGet(testIdentifier::getDisplayName);
	}

	private String getTestName(TestSource testSource) {
		if (testSource instanceof ClassSource) {
			ClassSource classSource = (ClassSource) testSource;
			return classSource.getClassName();
		}
		if (testSource instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) testSource;
			return String.format(Locale.ROOT, "%s(%s)", methodSource.getMethodName(), methodSource.getClassName());
		}
		return null;
	}

	private String getTestParameterTypes(TestIdentifier testIdentifier) {
		return testIdentifier.getSource()
			.map(this::getTestParameterTypes)
			.orElse("");
	}

	private String getTestParameterTypes(TestSource testSource) {
		if (testSource instanceof MethodSource) {
			MethodSource methodSource = (MethodSource) testSource;
			return methodSource.getMethodParameterTypes();
		}
		return null;
	}

	private void message(String key, TestIdentifier testIdentifier) {
		message(key, testIdentifier, "");
	}

	// namePrefix is used as a special case to signal ignored and aborted tests.
	private void message(String key, TestIdentifier testIdentifier, String namePrefix) {
		StringBuilder sb = new StringBuilder(getTestId(testIdentifier)).append(',');
		appendEscaped(namePrefix, sb);
		appendEscaped(getTestName(testIdentifier), sb);
		message(key, sb);
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

	private void visitEntry(TestIdentifier testIdentifier, boolean isDynamic) {
		StringBuilder treeEntry = new StringBuilder(getTestId(testIdentifier)).append(',');
		appendEscaped(getTestName(testIdentifier), treeEntry);
		treeEntry.append(',');
		Set<TestIdentifier> children;
		if (testIdentifier.isTest()) {
			children = Collections.emptySet();
			treeEntry.append(false)
				.append(',')
				.append('1');
		} else {
			children = testPlan.getChildren(testIdentifier);
			treeEntry.append(true)
				.append(',')
				.append(children.size());
		}
		treeEntry.append(',')
			.append(isDynamic)
			.append(',')
			.append(getParentTestId(testIdentifier))
			.append(',');
		appendEscaped(testIdentifier.getDisplayName(), treeEntry);
		treeEntry.append(',');
		appendEscaped(getTestParameterTypes(testIdentifier), treeEntry);
		treeEntry.append(',');
		appendEscaped(testIdentifier.getUniqueId(), treeEntry);
		synchronized (client.out) {
			message("%TSTTREE", treeEntry);
			for (TestIdentifier child : children) {
				visitEntry(child, isDynamic);
			}
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

	@Override
	public void close() {
		info(() -> idMap.entrySet()
			.stream()
			.map(entry -> entry.getKey() + " => " + entry.getValue())
			.collect(Collectors.joining(",\n")));
		safeClose(client);
		safeClose(control);
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

	private void info(String format, Object... args) {
		if (verbose) {
			System.err.printf(format.concat("%n"), args);
		}
	}
}
