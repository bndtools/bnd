package aQute.tester.test.assertions.internal;

import java.util.List;

import org.opentest4j.MultipleFailuresError;

// This test class is not supposed to be run directly; see readme.md for more info.
public class CustomMultipleFailuresError extends MultipleFailuresError {
	private static final long serialVersionUID = 1L;

	public CustomMultipleFailuresError(String msg, List<Throwable> bucket) {
		super(msg, bucket);
	}
}
