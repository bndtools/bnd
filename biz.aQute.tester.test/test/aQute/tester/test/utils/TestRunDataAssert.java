package aQute.tester.test.utils;

import static aQute.tester.test.utils.TestRunData.nameOf;
import static org.eclipse.jdt.internal.junit.model.ITestRunListener2.STATUS_ERROR;
import static org.eclipse.jdt.internal.junit.model.ITestRunListener2.STATUS_FAILURE;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.opentest4j.MultipleFailuresError;

public class TestRunDataAssert extends AbstractAssert<TestRunDataAssert, TestRunData> {

	public TestRunDataAssert(TestRunData actual) {
		super(actual, TestRunDataAssert.class);
	}

	public TestRunDataAssert hasFailedTest(Class<?> testClass, String testMethod) {
		return hasFailedTest(testClass, testMethod, null);
	}

	public TestRunDataAssert hasFailedTest(Class<?> testClass, String testMethod,
		Class<? extends AssertionError> errorClass) {
		hasUnsuccessfulTest(testClass, testMethod, errorClass, STATUS_FAILURE);
		return this;
	}

	public TestRunDataAssert hasFailedTest(Class<?> testClass, String testMethod,
		Class<? extends AssertionError> errorClass, String expected, String actual) {
		hasUnsuccessfulTest(testClass, testMethod, errorClass, STATUS_FAILURE, expected, actual);
		return this;
	}

	public TestRunDataAssert hasErroredTest(String test) {
		return hasErroredTest(test, (Throwable) null);
	}

	public TestRunDataAssert hasErroredTest(String test, Class<? extends Throwable> errorClass) {
		messagePrefix = String.format("Expecting test:\n%s\n", test);
		hasUnsuccessfulTest(test, errorClass, STATUS_ERROR, null, null);
		return this;
	}

	public TestRunDataAssert hasErroredTest(String test, Throwable error) {
		messagePrefix = String.format("Expecting test:\n%s\n", test);
		hasUnsuccessfulTest(test, error, STATUS_ERROR, null, null);
		return this;
	}

	public TestRunDataAssert hasErroredTest(Class<?> testClass, String testMethod) {
		return hasErroredTest(testClass, testMethod, null);
	}

	public TestRunDataAssert hasErroredTest(Class<?> testClass, String testMethod,
		Class<? extends Throwable> errorClass) {
		hasUnsuccessfulTest(testClass, testMethod, errorClass, STATUS_ERROR);
		return this;
	}

	final private TestFailure hasUnsuccessfulTest(Class<?> testClass, String testMethod,
		Class<? extends Throwable> errorClass, int status) {
		return hasUnsuccessfulTest(testClass, testMethod, errorClass, status, null, null);
	}

	private static String messagePrefix(Class<?> testClass, String testMethod) {
		return String.format("Expecting test:\n%s\nin class:\n%s\n", testMethod, testClass.getName());
	}

	private static String messagePrefix(Class<?> testClass) {
		return String.format("Expecting test class:\n%s\n", testClass.getName());
	}

	private void collectError(Runnable r, List<AssertionError> bucket) {
		try {
			r.run();
		} catch (AssertionError t) {
			bucket.add(t);
		}
	}

	private String messagePrefix;

	final private TestFailure hasUnsuccessfulTest(Class<?> testClass, String testMethod,
		Class<? extends Throwable> errorClass, int status, String expectedString, String actualString) {
		final String name = nameOf(testClass, testMethod);
		messagePrefix = messagePrefix(testClass, testMethod);
		return hasUnsuccessfulTest(name, errorClass, status, expectedString, actualString);
	}

	static String firstLine(String multiLine) {
		int index = multiLine.indexOf('\n');
		if (index != -1) {
			return multiLine.substring(0, index - 1);
		}
		return multiLine;
	}

	final private TestFailure hasUnsuccessfulTest(final String name, Throwable error, int status, String expectedString,
		String actualString) {
		return hasUnsuccessfulTest(name, (error == null ? null : firstLine(error.toString())), status, expectedString,
			actualString);
	}

	final private TestFailure hasUnsuccessfulTest(final String name, Class<? extends Throwable> errorClass, int status,
		String expectedString, String actualString) {
		return hasUnsuccessfulTest(name, errorClass.getName(), status, expectedString, actualString);
	}

	final private TestFailure hasUnsuccessfulTest(final String name, String trace, int status, String expectedString,
		String actualString) {
		isNotNull();
		TestFailure failure = actual.getFailureByName(name);
		if (failure == null) {
			TestEntry e = actual.getNameMap()
				.get(name);
			final String failureType = (e == null) ? "to have run, but no entry existed"
				: "to have failed, but it succeeded";
			failWithMessage(messagePrefix + failureType);
		} else {
			List<AssertionError> errors = new ArrayList<>();
			if (failure.status != status) {
				collectError(
					() -> failWithMessage(
						messagePrefix + "to have failed with code:\n%d (%s),\nbut it had code:\n%d (%s)", status,
						TestFailure.statusToString(status), failure.status, TestFailure.statusToString(failure.status)),
					errors);
			}
			if (trace != null) {
				if (failure.trace == null) {
					collectError(
						() -> failWithMessage(messagePrefix + "to have trace starting with:\n%s\nbut had none", trace),
						errors);
				} else {
					int index = failure.trace.indexOf("\n");
					String firstLine = (index == -1 ? failure.trace : failure.trace.substring(0, index - 1) + " ...");
					if (!failure.trace.startsWith(trace)) {
						collectError(() -> failWithMessage(
							messagePrefix + "to have trace starting with:\n%s\nbut had trace:\n%s", trace, firstLine),
							errors);
					}
				}
			}
			if (expectedString != null) {
				if (!expectedString.equals(failure.expected)) {
					collectError(() -> failWithMessage(messagePrefix + "to have expected value:\n%s\nbut was:\n%s",
						expectedString, failure.expected), errors);
				}
			}
			if (actualString != null) {
				if (!actualString.equals(failure.actual)) {
					collectError(() -> failWithMessage(messagePrefix + "to have actual value:\n%s\nbut was:\n%s",
						actualString, failure.actual), errors);
				}
			}
			if (errors.size() == 1) {
				throw errors.get(0);
			} else if (errors.size() > 1) {
				throw new MultipleFailuresError(String.format("Multiple errors for %s", name), errors);
			}
		}

		return failure;
	}

	public TestRunDataAssert hasSuccessfulTest(Class<?> testClass, String testMethod) {
		messagePrefix = messagePrefix(testClass, testMethod);
		hasSuccessfulTest(nameOf(testClass, testMethod));
		return this;
	}

	public TestRunDataAssert hasSuccessfulTest(String name) {

		TestEntry e = actual.getNameMap()
			.get(name);
		if (e == null) {
			failWithMessage(messagePrefix + "to have been registered, but no entry existed");
		} else {
			TestFailure failure = actual.getFailureByName(name);
			if (failure != null) {
				failWithMessage(messagePrefix + "to have succeeded, but had failure:\n%s", failure);
			}
		}

		return this;
	}

	public TestRunDataAssert hasSkippedTest(Class<?> testClass, String reason) {
		messagePrefix = messagePrefix(testClass);
		return hasAbortedOrSkippedContainer(nameOf(testClass), "Skipped: " + reason);
	}

	public TestRunDataAssert hasSkippedTest(Class<?> testClass, String testMethod, String reason) {
		messagePrefix = messagePrefix(testClass, testMethod);
		return hasAbortedOrSkippedTest(nameOf(testClass, testMethod), "Skipped: " + reason);
	}

	public TestRunDataAssert hasAbortedTest(Class<?> testClass, Throwable cause) {
		messagePrefix = messagePrefix(testClass);
		return hasAbortedOrSkippedContainer(nameOf(testClass), firstLine(cause.toString()));
	}

	public TestRunDataAssert hasAbortedTest(Class<?> testClass, String testMethod, Throwable cause) {
		messagePrefix = messagePrefix(testClass, testMethod);
		return hasAbortedTest(nameOf(testClass, testMethod), cause);
	}

	public TestRunDataAssert hasAbortedTest(String name, Throwable cause) {
		return hasAbortedOrSkippedTest(name, firstLine(cause.toString()));
	}

	private TestRunDataAssert hasAbortedOrSkippedTest(final String name, String reason) {
		if (actual == null) {
			failWithMessage(messagePrefix + ": actual was null");
		}
		TestEntry e = actual.getNameMap()
			.get(name);
		if (e == null) {
			failWithMessage(messagePrefix + "to have been registered, but no entry existed");
		} else {
			List<AssertionError> errors = new ArrayList<>();
			final String expected = "@AssumptionFailure: " + name;
			final String startStr = actual.getStartName(e.testId);
			if (!name.equals(startStr)) {
				collectError(
					() -> failWithMessage(messagePrefix + "to have start test name:\n%s\nbut was:\n%s", name, startStr),
					errors);
			}
			TestFailure failure = actual.getFailure(e.testId);
			if (failure == null) {
				collectError(() -> failWithMessage(messagePrefix + "to have failure\nbut didn't"), errors);
			} else {
				final String failStr = failure.testName;
				if (!expected.equals(failStr)) {
					collectError(() -> failWithMessage(messagePrefix + "to have failed test name:\n%s\nbut was:\n%s",
						expected, failStr), errors);
				}
				if (reason != null) {
					if (failure.trace == null || !failure.trace.startsWith(reason)) {
						collectError(() -> failWithMessage(
							messagePrefix + "to have failure trace starting with:\n%s\nbut was:\n%s", reason,
							failure.trace), errors);
					}
				}
			}
			final String endStr = actual.getEndName(e.testId);
			if (!name.equals(endStr)) {
				collectError(
					() -> failWithMessage(messagePrefix + "to have end test name:\n%s\nbut was:\n%s", name, endStr),
					errors);
			}
			if (errors.size() == 1) {
				throw errors.get(0);
			} else if (errors.size() > 1) {
				throw new MultipleFailuresError(String.format("Multiple errors for %s", name), errors);
			}

		}
		return this;
	}

	public TestRunDataAssert hasAbortedOrSkippedContainer(final String name, String reason) {
		if (actual == null) {
			failWithMessage(messagePrefix + ": actual was null");
		}
		TestEntry e = actual.getNameMap()
			.get(name);
		if (e == null) {
			failWithMessage(messagePrefix + "to have been registered, but no entry existed");
		} else {
			List<AssertionError> errors = new ArrayList<>();
			final String expected = "@AssumptionFailure: " + name;
			final String startStr = actual.getStartName(e.testId);
			if (startStr != null) {
				collectError(
					() -> failWithMessage(messagePrefix + "to have no start test name\nbut was:\n%s", startStr),
					errors);
			}
			TestFailure failure = actual.getFailure(e.testId);
			if (failure == null) {
				collectError(() -> failWithMessage(messagePrefix + "to have failure\nbut didn't"), errors);
			} else {
				final String failStr = failure.testName;
				if (!expected.equals(failStr)) {
					collectError(() -> failWithMessage(messagePrefix + "to have failed test name:\n%s\nbut was:\n%s",
						expected, failStr), errors);
				}
				if (reason != null) {
					if (failure.trace == null || !failure.trace.startsWith(reason)) {
						collectError(() -> failWithMessage(
							messagePrefix + "to have failure trace starting with:\n%s\nbut was:\n%s", reason,
							failure.trace), errors);
					}
				}
			}
			final String endStr = actual.getEndName(e.testId);
			if (endStr != null) {
				collectError(() -> failWithMessage(messagePrefix + "to have no end test name\nbut was:\n%s", endStr),
					errors);
			}
			if (errors.size() == 1) {
				throw errors.get(0);
			} else if (errors.size() > 1) {
				throw new MultipleFailuresError(String.format("Multiple errors for %s", name), errors);
			}

		}
		return this;
	}
}
