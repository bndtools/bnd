package aQute.lib.redirect;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import aQute.lib.exceptions.SupplierWithException;
import aQute.lib.io.IO;

/**
 * Utility to redirect the stdin/stdout/stderr when running a command
 */
public class Redirect {

	private final InputStream	stdin;
	private final OutputStream	stdout;
	private final OutputStream	stderr;
	private boolean				captureStdout;
	private boolean				captureStderr;
	private Capture				cstdout;
	private Capture				cstderr;

	/**
	 * Create a stdio redirector
	 *
	 * @param stdin the stdin read from or null for System.in
	 * @param stdout the stdout to write to or null for System.out
	 * @param stderr the stderr to write to or null for System.err
	 */
	public Redirect(InputStream stdin, OutputStream stdout, OutputStream stderr) {

		assert stdin != System.in;
		assert stdout != System.out;
		assert stderr != System.err;

		this.stdin = stdin;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	/**
	 * Create a stdio redirector
	 *
	 * @param stdin the stdin read from or null for System.in
	 * @param stdout the stdout to write to or null for System.out
	 * @param stderr the stderr to write to or null for System.err
	 */
	public Redirect(String stdin, OutputStream stdout, OutputStream stderr) {
		this(IO.stream(stdin), stdout, stderr);
	}

	/**
	 * Create a stdio redirector without any input
	 *
	 * @param stdout the stdout to write to or null for System.out
	 * @param stderr the stderr to write to or null for System.err
	 */
	public Redirect(OutputStream stdout, OutputStream stderr) {
		this((InputStream) null, stdout, stderr);
	}

	/**
	 * Capture stdout during an apply. Every apply will be captured seperately.
	 *
	 * @return this
	 */
	public Redirect captureStdout() {
		this.captureStdout = true;
		return this;
	}

	/**
	 * Capture stderr during an apply. Every apply will be captured seperately.
	 *
	 * @return this
	 */
	public Redirect captureStderr() {
		this.captureStderr = true;
		return this;
	}

	/**
	 * Call the supplier and return the result. While the supplier is active,
	 * the System streams are redirected as instructed by the constructor.
	 * Redirection & capture will only take place on the current thread. The
	 * original state will be introduced afterwards.
	 * <p>
	 * Although the system streams are redirected, the original output streams
	 * are still written to.
	 *
	 * @param <R> the type for the supplier
	 * @param f the supplier
	 * @return the return of the supplier.
	 * @throws Exception
	 */
	public <R> R apply(SupplierWithException<R> f) throws Exception {
		cstdout = captureStdout ? new Capture() : null;
		cstderr = captureStderr ? new Capture() : null;
		PrintStream teeStdout = build(System.out, this.stdout, cstdout);
		PrintStream teeStderr = build(System.err, this.stderr, cstderr);

		PrintStream orgStdout = System.out;
		PrintStream orgStderr = System.err;
		InputStream orgStdin = System.in;

		if (stdin != null)
			System.setIn(stdin);

		if (teeStdout != null)
			System.setOut(teeStdout);

		if (teeStderr != null)
			System.setErr(teeStderr);

		try {

			return f.get();

		} finally {
			if (stdin != null)
				System.setIn(orgStdin);

			if (teeStdout != null)
				System.setOut(orgStdout);

			if (teeStderr != null)
				System.setOut(orgStderr);
		}
	}

	private PrintStream build(PrintStream original, OutputStream redirect, Capture capture) {
		Thread thread = Thread.currentThread();

		int n = 0;
		if (redirect != null)
			n += 1;
		if (capture != null)
			n += 2;
		switch (n) {
			default :
			case 0 :
				return null;

			case 1 :
				return new PrintStream(new Tee(original, new Tee(thread, redirect)));
			case 2 :
				return new PrintStream(new Tee(original, new Tee(thread, capture)));
			case 3 :
				return new PrintStream(new Tee(original, new Tee(thread, redirect, capture)));
		}
	}

	public String getStderr() {
		if (cstderr == null)
			throw new IllegalArgumentException("did not capture stderr");

		return cstderr.toString();
	}

	public String getStdout() {
		if (cstdout == null)
			throw new IllegalArgumentException("did not capture stdout");

		return cstdout.toString();
	}

	public String getContent() {
		StringBuilder sb = new StringBuilder();
		if (cstdout != null)
			sb.append(cstdout);
		if (cstderr != null) {
			String err = cstderr.toString();
			if (err != null && !err.isEmpty()) {
				sb.append("\n---stderr--\n");
				sb.append(err);
			}
		}
		return sb.toString();
	}
}
