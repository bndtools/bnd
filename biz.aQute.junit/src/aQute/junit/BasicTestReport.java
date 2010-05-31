package aQute.junit;

import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class BasicTestReport implements TestListener, TestReporter {
    int            errors;
    boolean        verbose = true;


	public void begin(Bundle fw, Bundle targetBundle, List tests, int realcount) {
        if (verbose) {
            System.out
                    .println("====================================================================");
        }
    }

    public void addError(Test test, Throwable t) {
    	check();
        errors++;
        if (verbose) {
            System.out.println(test + " : ");
            t.printStackTrace(System.out);
            System.out.println();
        }
    }

    public void addFailure(Test test, AssertionFailedError t) {
    	check();
        errors++;
        if (verbose) {
            System.out.println();
            System.out.print(test + " : ");
            t.getMessage();
        }
    }

    public void endTest(Test test) {
    	check();
        if (verbose) {
            System.out.print("<< " + test + "\n");
        }
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
            System.out.println(">> " + test + "\n");
    }

    public void end() {
        if (verbose) {
            System.out
                    .println("-------------------------------------------------------------------------");
            if (errors == 0)
                System.out.println("No errors :-)");
            else if (errors == 1)
                System.out.println("One error :-|");
            else
                System.out.println(errors + " errors :-(");
            System.out.println();
            System.out.println();
        }
    }

    public void aborted() {
        if (verbose) {
            System.out.println();
            System.out
                    .println("-------------------------------------------------------------------------");
        }
        System.out.println("\nAborted ...");
    }

    protected void check() {
    	
    }
}
