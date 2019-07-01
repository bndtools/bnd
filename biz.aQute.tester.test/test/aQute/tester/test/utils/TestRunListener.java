package aQute.tester.test.utils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;

public class TestRunListener implements ITestRunListener2 {

	private volatile TestRunData	data;
	private CountDownLatch			flag	= new CountDownLatch(1);
	private SoftAssertions			softly;
	private boolean					verbose	= false;

	public TestRunListener(SoftAssertions softly, boolean verbose) {
		this.softly = softly;
		this.verbose = verbose;
	}

	public void clearData() {
		data = null;
	}

	void info(Supplier<String> msg) {
		if (verbose) {
			System.err.println(msg.get());
		}
	}

	@Override
	public void testRunStarted(int testCount) {
		data = new TestRunData();
		softly.assertThat(data.idMap)
			.as("testRunStarted: tests run")
			.isEmpty();
		softly.assertThat(data.started)
			.as("testRunStarted: started")
			.isEmpty();
		this.data.testCount = testCount;
		info(() -> "=========>testRunStarted");
	}

	public void waitForClientToFinish(long timeout) throws InterruptedException {
		flag.await(timeout, TimeUnit.MILLISECONDS);
	}

	private void notifyTestEnded() {
		flag.countDown();
	}

	@Override
	public void testRunEnded(long elapsedTime) {
		// Could be greater than the initial reported test count if dynamic
		// tests were encountered
		softly.assertThat(data.idMap.size())
			.as("testRunEnded: total run")
			.isGreaterThanOrEqualTo(data.testCount);
		info(() -> "=========>testRunEnded");
		softly.assertThat(data.started)
			.as("testRunEnded: start/end pairs")
			.isEmpty();
		notifyTestEnded();
		info(() -> "failure map: " + data.failureMap.entrySet()
			.stream()
			.map(Map.Entry::toString)
			.collect(Collectors.joining(",\n")));
	}

	@Override
	public void testRunStopped(long elapsedTime) {
		info(() -> "=========>testRunStopped");
		softly.fail("shouldn't be called");
		notifyTestEnded();
	}

	@Override
	public void testStarted(String testId, String testName) {
		final String msgPrefix = "testStarted(" + testId + "," + testName + "): ";
		data.executionMap.add(Optional.ofNullable(data.started.pollLast())
			.orElse(""), testId);
		data.startNameMap.add(testId, testName);

		info(() -> "=========>" + msgPrefix);
		TestEntry test = data.idMap.get(testId);
		info(() -> "Checking that tree entry has already been set");
		if (test != null) {
			softly.assertThat(testName)
				.as(msgPrefix + "testName")
				.endsWith(test.testName);
		} else {
			softly.fail("No existing test entry for testId: " + testId);
		}
		softly.assertThat(data.started)
			.as(msgPrefix + "shouldn't be called more than once")
			.doesNotContain(testId);
		data.started.addLast(testId);
	}

	@Override
	public void testEnded(String testId, String testName) {
		info(() -> "=========>testEnded: " + testId);
		data.endNameMap.add(testId, testName);
		softly.assertThat(data.started.getLast())
			.as("testEnded: must be the last test started")
			.isEqualTo(testId);
		TestEntry test = data.getById(testId);
		if (test != null) {
			softly.assertThat(testName)
				.as("testEnded: testName")
				.endsWith(test.testName);
		}
		if (data.started.getLast()
			.equals(testId)) {
			data.started.removeLast();
		}
	}

	@Override
	public void testRunTerminated() {
		info(() -> "=========>testRunTerminated");
		softly.fail("shouldn't be called");
		notifyTestEnded();
	}

	@Override
	public void testTreeEntry(String description) {
		info(() -> "=========>testTreeEntry");
		try {
			TestEntry entry = new TestEntry(description);
			softly.assertThat(data.idMap)
				.as("testTreeEntry: ID shouldn't already be there")
				.doesNotContainKey(entry.testId);
			data.idMap.put(entry.testId, entry);
			data.nameMap.put(entry.testName, entry);
			data.parentMap.add(Optional.ofNullable(entry.parentId)
				.orElse(""), entry.testId);
		} catch (Exception e) {
			softly.fail("Error trying to parse tree entry string:\n" + description, e);
		}
	}

	@Override
	public void testFailed(int status, String testId, String testName, String trace, String expected, String actual) {
		info(() -> "=========>testFailed[" + testId + "]: status: " + status + ", testId: " + testId + ", testName: "
			+ testName);
		final String prefix = "testFailed[" + testId + "]: %s";
		softly.assertThat(status)
			.as(prefix, "status")
			.isIn(STATUS_FAILURE, STATUS_ERROR);
		TestEntry test = data.getById(testId);
		if (test == null) {
			softly.fail(prefix, "test tree entry should already exist");
		} else {
			softly.assertThat(testName)
				.as(prefix, "testName")
				.endsWith(test.testName);

			// Containers can get a failure notification even without a
			// start/end pair.
			if (!test.isSuite) {
				softly.assertThat(data.started)
					.as(prefix, "must have already been started")
					.isNotEmpty();
				if (!data.started.isEmpty()) {
					softly.assertThat(data.started.getLast())
						.as(prefix, "must be the last test started")
						.isEqualTo(testId);
				}
			}
		}
		data.failureMap.put(testId, new TestFailure(testId, testName, status, trace, expected, actual));
	}

	@Override
	public void testReran(String testId, String testClass, String testName, int status, String trace, String expected,
		String actual) {
		info(() -> "=========>testReran");
		softly.fail("shouldn't be called");
	}

	public void checkRunTime() {
		if (data == null) {
			softly.assertThat(data)
				.as("reportedRuntime")
				.isNotNull();
		} else {
			softly.assertThat(data.reportedRunTime)
				.as("reportedRunTime")
				.isLessThanOrEqualTo(data.actualRunTime);
		}
	}

	public TestRunData getLatestRunData() {
		return data;
	}
}
