package aQute.tester.junit.platform.test;

/**
 * This extends Error rather than SecurityException so that it can traverse the
 * catch(Exception) statements in the code-under-test.
 */

public class ExitCode extends Error {
	private static final long	serialVersionUID	= -1498037177123939551L;
	public final int			exitCode;
	public final StackTraceElement[]	stack;

	public ExitCode(int exitCode) {
		this.exitCode = exitCode;
		this.stack = Thread.currentThread()
			.getStackTrace();
	}
}
