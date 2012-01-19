package aQute.junit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class BasicTestReport implements TestListener, TestReporter {
	private int				errors;
	private PrintStream		out;
	private final Tee		systemOut;
	private final Tee		systemErr;
	private int				fails;
	private Bundle			targetBundle;
	private final Activator	activator;

	public BasicTestReport(Activator activator, Tee systemOut, Tee systemErr) {
		this.systemOut = systemOut;
		this.systemErr = systemErr;
		this.activator = activator;
	}

	public void setup(Bundle fw, Bundle targetBundle) {
		this.targetBundle = targetBundle;
	}
	
	public void begin(List tests, int realcount) {
		activator.trace(">>>> %s, tests %s", targetBundle, tests);
	}

	public void addError(Test test, Throwable t) {
		activator.trace("  add error to %s : %s", test, t);
		check();
		fails++;
		errors++;
	}

	public void addFailure(Test test, AssertionFailedError t) {
		activator.trace("  add failure to %s : %s", test, t);
		check();
		fails++;
		errors++;
	}

	public void startTest(Test test) {
		activator.trace("  >> %s", test);
		check();
		Bundle b = targetBundle;
		if (b == null)
			b = FrameworkUtil.getBundle(test.getClass());

		if (b != null) {
			BundleContext context = b.getBundleContext();
			activator.trace("got bundle context %s from %s in state %s", context, b, b.getState()	);
			assert context != null;
			try {
				Method m = test.getClass().getMethod("setBundleContext",
						new Class[] { BundleContext.class });
				m.setAccessible(true);
				m.invoke(test, new Object[] { context });
				activator.trace("set context through setter");
			} catch (Exception e) {
				Field f;
				try {
					f = test.getClass().getField("context");
					f.set(test, context);
					activator.trace("set context in field");
				} catch (Exception e1) {
					// Ok, no problem
				}
			}
		}
		fails = 0;
		systemOut.clear().capture(true).echo(true);
		systemErr.clear().capture(true).echo(true);
	}

	public void endTest(Test test) {
		activator.trace("  << %s, fails=%s, errors=%s", test, fails, errors);
		systemOut.capture(false);
		systemErr.capture(false);
		if (fails > 0) {
			String sysout = systemOut.getContent();
			String syserr = systemErr.getContent();
			if (sysout != null)
				activator.trace("out: %s", sysout);
			if (syserr != null) {
				activator.trace("err: %s", syserr);
			}
		}
		check();
	}

	public void end() {
		activator.trace("<<<<");
	}

	public void aborted() {
		activator.trace("ABORTED");
		out.println("ABORTED");
	}

	protected void check() {

	}

	String[] getCaptured() {
		return new String[] { systemOut.getContent(), systemErr.getContent() };
	}
}
