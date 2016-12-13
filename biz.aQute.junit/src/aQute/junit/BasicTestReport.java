package aQute.junit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class BasicTestReport implements TestListener, TestReporter {
	private int				errors;
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

	public void begin(List<Test> tests, int realcount) {
		activator.trace(">>>> %s, tests %s", targetBundle, tests);
	}

	public void addError(Test test, Throwable t) {
		activator.trace("  add error to %s : %s", test, t);
		check();
		errors++;
	}

	public void addFailure(Test test, AssertionFailedError t) {
		activator.trace("  add failure to %s : %s", test, t);
		check();
		fails++;
	}

	public void startTest(Test test) {
		activator.trace("  >> %s", test);
		check();
		Bundle b = targetBundle;
		if (b == null)
			b = FrameworkUtil.getBundle(test.getClass());

		if (b != null) {
			BundleContext context = b.getBundleContext();
			activator.trace("got bundle context %s from %s in state %s", context, b, b.getState());
			assert context != null;
			try {
				Method m = test.getClass().getMethod("setBundleContext", new Class[] {
						BundleContext.class
				});
				m.setAccessible(true);
				m.invoke(test, new Object[] {
						context
				});
				activator.trace("set context through setter");
			} catch (Exception e) {
				Field f;
				try {
					f = test.getClass().getField("context");
					f.setAccessible(true);
					f.set(test, context);
					activator.trace("set context in field");
				} catch (Exception e1) {
					// Ok, no problem
				}
			}
		}
		fails = 0;
		errors = 0;
		systemOut.clear().capture(true).echo(true);
		systemErr.clear().capture(true).echo(true);
	}

	public void endTest(Test test) {
		activator.trace("  << %s, fails=%s, errors=%s", test, fails, errors);
		systemOut.capture(false);
		systemErr.capture(false);
		if ((fails > 0) || (errors > 0)) {
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
	}

	protected void check() {

	}

	String[] getCaptured() {
		return new String[] {
				systemOut.getContent(), systemErr.getContent()
		};
	}
}
