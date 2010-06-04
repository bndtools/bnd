package aQute.junit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class BasicTestReport implements TestListener, TestReporter {
	private int errors;
	private boolean verbose = true;
	private PrintStream out;
	private final Tee systemOut;
	private final Tee systemErr;
	private int fails;
	private Bundle targetBundle;
	
	public BasicTestReport(Tee systemOut, Tee systemErr) {
		this.systemOut = systemOut;
		this.systemErr = systemErr;
		out = systemOut.oldStream;
	}

	public void begin(Bundle fw, Bundle targetBundle, List tests, int realcount) {
		this.targetBundle = targetBundle;
		if (verbose) {
			out
					.println("====================================================================");
		}
	}

	public void addError(Test test, Throwable t) {
		check();
		fails++;
		errors++;
		if (verbose) {
			out.println(test + " : ");
			t.printStackTrace(out);
			out.println();
		}
	}

	public void addFailure(Test test, AssertionFailedError t) {
		check();
		fails++;
		errors++;
		if (verbose) {
			out.println();
			out.print(test + " : ");
			t.getMessage();
		}
	}

	public void startTest(Test test) {
		check();
		Bundle b = targetBundle;
		if ( b== null)
			b = FrameworkUtil.getBundle(test.getClass());
		
		if (b != null) {
			BundleContext context = b.getBundleContext();
			assert context != null;
			try {
				Method m = test.getClass().getMethod("setBundleContext",
						new Class[] { BundleContext.class });
				m.setAccessible(true);
				m.invoke(test, new Object[] { context });
			} catch (Exception e) {
				Field f;
				try {
					f = test.getClass().getField("context");
					f.set(test, context);
				} catch (Exception e1) {
					// Ok, no problem
				}
			}
		}
		if (verbose)
			out.print(">> " + test);

		fails = 0;
		systemOut.clear().capture(true).echo(false);
		systemErr.clear().capture(true).echo(false);
	}

	public void endTest(Test test) {
		systemOut.capture(false);
		systemErr.capture(false);
		if (verbose) {
			if (fails > 0) {
				out.println();
				String sysout = systemOut.getContent();
				String syserr = systemErr.getContent();
				if (sysout != null)
					out.println(sysout);
				if (syserr != null) {
					out.println("*** syserr *** ");
					out.println(syserr);
				}
				out.println("<<" + test);
			} else {
				out.println(" <<");
			}
		}
		check();
	}

	public void end() {
		if (verbose) {
			out
					.println("-------------------------------------------------------------------------");
			out.println();
			out.println();
		}
	}

	public void aborted() {
		if (verbose) {
			out.println();
			out
					.println("-------------------------------------------------------------------------");
		}
		out.println("\nAborted ...");
	}

	protected void check() {

	}

	String[] getCaptured() {
		return new String[] { systemOut.getContent(), systemErr.getContent() };
	}

}
