package aQute.junit;

import static java.lang.invoke.MethodHandles.publicLookup;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

public class BasicTestReport implements TestListener, TestReporter {
	private int					errors;
	private final Tee			systemOut;
	private final Tee			systemErr;
	private int					fails;
	private Bundle				targetBundle;
	private final Activator		activator;
	private final TestResult	result;

	public BasicTestReport(Activator activator, Tee systemOut, Tee systemErr, TestResult result) {
		this.systemOut = systemOut;
		this.systemErr = systemErr;
		this.activator = activator;
		this.result = result;
	}

	@Override
	public void setup(Bundle fw, Bundle targetBundle) {
		this.targetBundle = targetBundle;
	}

	@Override
	public void begin(List<Test> tests, int realcount) {
		activator.trace(">>>> %s, tests %s", targetBundle, tests);
	}

	@Override
	public void addError(Test test, Throwable t) {
		if (activator.isTrace()) {
			activator.trace("  add error to %s : %s", test, t);
		} else {
			systemErr.capture(false);
			try {
				activator.message("", "TEST %s <<< ERROR: %s", test, t);
			} finally {
				systemErr.capture(true);
			}
		}
		check();
	}

	@Override
	public void addFailure(Test test, AssertionFailedError t) {
		if (activator.isTrace()) {
			activator.trace("  add failure to %s : %s", test, t);
		} else {
			systemErr.capture(false);
			try {
				activator.message("", "TEST %s <<< FAILURE: %s", test, t);
			} finally {
				systemErr.capture(true);
			}
		}
		check();
	}

	@Override
	public void startTest(Test test) {
		activator.trace("  >> %s", test);
		check();
		Bundle b = targetBundle;
		if (b == null) {
			b = FrameworkUtil.getBundle(test.getClass());
		}

		if (b != null) {
			BundleContext context = b.getBundleContext();
			activator.trace("Obtained bundle context %s from %s in state %s", context, b, b.getState());
			assert context != null;
			try {
				MethodHandle mh;
				try {
					Method m = test.getClass()
						.getMethod("setBundleContext", BundleContext.class);
					m.setAccessible(true);
					mh = publicLookup().unreflect(m);
					if (!Modifier.isStatic(m.getModifiers())) {
						mh = mh.bindTo(test);
					}
					activator.trace("setBundleContext method will be used to set BundleContext");
				} catch (NoSuchMethodException | IllegalAccessException e) {
					try {
						Field f = test.getClass()
							.getField("context");
						f.setAccessible(true);
						mh = publicLookup().unreflectSetter(f);
						if (!Modifier.isStatic(f.getModifiers())) {
							mh = mh.bindTo(test);
						}
						activator.trace("context field will be used to set BundleContext");
					} catch (NoSuchFieldException | IllegalAccessException e1) {
						mh = null;
					}
				}
				if (mh != null) {
					mh.invoke(context);
					activator.trace("BundleContext set in test");
				}
			} catch (Error e) {
				throw e;
			} catch (Throwable t) {
				// Ok, no problem
			}
		}

		fails = result.failureCount();
		errors = result.errorCount();
		systemOut.clear()
			.capture(true)
			.echo(true);
		systemErr.clear()
			.capture(true)
			.echo(true);
	}

	@Override
	public void endTest(Test test) {
		activator.trace("  << %s, fails=%s, errors=%s", test, result.failureCount(), result.errorCount());
		systemOut.capture(false);
		systemErr.capture(false);
		if ((result.failureCount() > fails) || (result.errorCount() > errors)) {
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

	@Override
	public void end() {
		activator.trace("<<<<");
	}

	@Override
	public void aborted() {
		activator.trace("ABORTED");
	}

	private void check() {
		if (!activator.active()) {
			result.stop();
		}
	}

	String[] getCaptured() {
		return new String[] {
			systemOut.getContent(), systemErr.getContent()
		};
	}

	TestResult getTestResult() {
		return result;
	}
}
