package aQute.junit;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class JUnitEclipseReport implements TestReporter {
    int            port;
    boolean        open;
    BufferedReader in;
    PrintWriter    out;
    long           startTime;
    Bundle         targetBundle;
    List           tests;
    boolean        verbose = false;
    Test           current;

    public JUnitEclipseReport(int port) throws Exception {
        Socket socket = null;
        for (int i = 0; socket == null && i < 10; i++) {
            try {
                socket = new Socket("127.0.0.1", port);
            } catch (ConnectException ce) {
                Thread.sleep(i * 100);
            }
        }
        if (socket == null) {
        	System.err.println("Cannot open the JUnit Port: " + port);
            System.exit(-2);
        }

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),
                "UTF-8"));
    }

    public void setup(Bundle fw, Bundle targetBundle) {
        this.targetBundle = targetBundle;    	
    }
    
    public void begin(List tests, int realcount) {
        this.tests = tests;
        message("%TESTC  ", realcount + " v2");
        report(tests);
        startTime = System.currentTimeMillis();
    }

    public void end() {
        message("%RUNTIME", "" + (System.currentTimeMillis() - startTime));
        out.flush();
        out.close();
        try {
            in.close();
        } catch (Exception ioe) {
            // ignore
        }
    }

    public void addError(Test test, Throwable t) {
        message("%ERROR  ", test);
        trace(t);
    }

    public void addFailure(Test test, AssertionFailedError t) {
        message("%FAILED ", test);
        trace(t);
    }

    void trace(Throwable t) {
        message("%TRACES ", "");
        t.printStackTrace(out);
        out.println();
        message("%TRACEE ", "");
    }

    public void endTest(Test test) {
        message("%TESTE  ", test);
    }

    public void startTest(Test test) {
        this.current = test;
        message("%TESTS  ", test);
        try {
            Method m = test.getClass().getMethod("setBundleContext",
                    new Class[] { BundleContext.class });
            m.invoke(test, new Object[] { targetBundle.getBundleContext() });
        } catch (Exception e) {

        }
    }

    private void message(String key, String payload) {
        if (key.length() != 8)
            throw new IllegalArgumentException(key + " is not 8 characters");

        out.print(key);
        out.println(payload);
        out.flush();
        if (verbose)
            System.out.println(key + payload);
    }

    private void message(String key, Test test) {
    	if ( tests == null )
            message(key, "?,"+test);
    	else
    		message(key, (tests.indexOf(test) + 1) + "," + test);
    }

    private void report(List flattened) {
        for (int i = 0; i < flattened.size(); i++) {
            StringBuffer sb = new StringBuffer();
            sb.append(i + 1);
            sb.append(",");
            Test test = (Test) flattened.get(i);
            sb.append(flattened.get(i));
            sb.append(",");
            sb.append(test instanceof TestSuite);
            sb.append(",");
            sb.append(test.countTestCases());
            message("%TSTTREE", sb.toString());
        }
    }

    public void aborted() {
       
        end();
    }

}
