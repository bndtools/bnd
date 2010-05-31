package aQute.junit;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class BasicTestReport implements TestListener, TestReporter {
    int            errors;
    boolean        verbose = true;
    PrintStream out = System.out;

	public void begin(Bundle fw, Bundle targetBundle, List tests, int realcount) {
        if (verbose) {
            out
                    .println("====================================================================");
        }
    }

    public void addError(Test test, Throwable t) {
    	check();
        errors++;
        if (verbose) {
            out.println(test + " : ");
            t.printStackTrace(out);
            out.println();
        }
    }

    public void addFailure(Test test, AssertionFailedError t) {
    	check();
        errors++;
        if (verbose) {
        	out.println();
            out.print(test + " : ");
            t.getMessage();
        }
    }

    public void endTest(Test test) {
        if (verbose) {
            out.println("<< " + test + "\n");
        }
    	check();
    }

    public void startTest(Test test) {
    	check();
    	Bundle b = FrameworkUtil.getBundle(test.getClass());
    	BundleContext context = b.getBundleContext();
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
        if (verbose)
            out.println(">> " + test + "\n");
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
}
