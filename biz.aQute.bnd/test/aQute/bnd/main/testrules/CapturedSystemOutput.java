package aQute.bnd.main.testrules;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.rules.ExternalResource;

public class CapturedSystemOutput extends ExternalResource {
	private PrintStream		originalStdOut;
	private PrintStream		originalStdErr;

	private OutputStream	osStdOut		= new ByteArrayOutputStream();
	private OutputStream	osStdErr		= new ByteArrayOutputStream();

	private PrintStream		capturedStdOut	= new PrintStream(osStdOut);
	private PrintStream		capturedStdErr	= new PrintStream(osStdErr);

	@Override
	protected void before() throws Throwable {
		// save
		originalStdOut = System.out;
		originalStdErr = System.err;
		System.setOut(capturedStdOut);
		System.setErr(capturedStdErr);
	}

	@Override
	protected void after() {
		// recover
		System.setErr(originalStdErr);
		System.setOut(originalStdOut);
	}

	public PrintStream getSystemOut() {
		return originalStdOut;
	}

	public PrintStream getSystemErr() {
		return originalStdErr;
	}

	public String getSystemOutContent() {
		return osStdOut.toString()
			.trim();
	}

	public String getSystemErrContent() {
		return osStdErr.toString()
			.trim();
	}
}
