package aQute.junit.osgi;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import aQute.junit.runtime.*;

public class Activator extends Thread implements BundleActivator {
    BundleContext    context;
    String           reportName;
    BundleTracker    tracker;
    volatile boolean active;

    public Activator() {
        super("bnd Runtime Test Bundle");
    }

    public void start(BundleContext context) throws Exception {
        System.out.println("Runtime started");
        this.context = context;
        active = true;
        start();
    }

    public void stop(BundleContext context) throws Exception {
        active = false;
        interrupt();
        join(10000);
    }

    public void run() {
        System.out.println("Thread run started");
        reportName = System.getProperty("aQute.runtime.report.name");
        if (reportName == null)
            reportName = "testreport-%s.xml";

        final List queue = new ArrayList();

        tracker = new BundleTracker(context, Bundle.ACTIVE, null) {
            public Object addingBundle(Bundle bundle, BundleEvent event) {
                String testcases = (String) bundle.getHeaders().get(
                        "Test-Cases");
                System.out.println("Found bundle " + bundle.getBundleId()
                        + ", Test-Cases: " + testcases);
                if (testcases == null)
                    return null;

                System.out.println("Enqueueing bundle " + bundle);
                queue.add(bundle);
                synchronized (queue) {
                    queue.notifyAll();
                }
                return bundle;
            }
        };
        tracker.open();

        System.out.println("Starting loop");
        try {
            outer: while (active) {
                Bundle bundle;
                synchronized (queue) {
                    System.out.println("Will wait");
                    while (queue.isEmpty() && active)
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            interrupt();
                            break outer;
                        }
                }
                bundle = (Bundle) queue.remove(0);
                System.out.println("Dequeueing bundle " + bundle);
                test(bundle);
                System.out.println("All tests done " + bundle);
            }
        } finally {
            System.out.println("Exiting loop");
            tracker.close();
        }
    }

    void test(Bundle bundle) {
        Bundle fw = context.getBundle(0);
        try {

            List names = new ArrayList();
            Target.checkTestCases(bundle, names);
            final TestResult result = new TestResult();
            BasicTestReport otl = new BasicTestReport() {
                public void startTest(Test test) {
                    if (!active)
                        throw new ThreadDeath();
                    super.startTest(test);
                }
            };

            String path = replace(reportName, "%s", bundle.getSymbolicName());
            JunitXmlReport report = new JunitXmlReport(path);

            try {
                TestSuite suite = Target.createSuite(bundle, names);
                List flattened = new ArrayList();
                int realcount = Target.flatten(flattened, suite);
                report.begin(fw, bundle, flattened, realcount);
                otl.begin(fw, bundle, flattened, realcount);
                result.addListener(report);
                result.addListener(otl);
                suite.run(result);
            } catch (ThreadDeath td) {
                report.aborted();
                otl.aborted();
            } catch (Throwable t) {
                result.addError(null, t);
            } finally {
                report.end();
                otl.end();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public String replace(String source, String symbol, String replace) {
        StringBuffer sb = new StringBuffer(source);
        int n = sb.indexOf(symbol, 0);
        while (n > 0) {
            sb.replace(n, n + 2, replace);
            n = sb.indexOf(replace, n + replace.length());
        }
        return sb.toString();
    }

    public static String[] split(String items, String splitter) {
        List list = new ArrayList();
        
        int n = items.indexOf(splitter);
        int last = 0;
        while ( n >= 0) {
            list.add(items.substring(last, n).trim());
            last = n+splitter.length();
            n = items.indexOf(splitter,last);
        }
        list.add(items.substring(last).trim());
        return (String[]) list.toArray( new String[list.size()]);
    }
}
