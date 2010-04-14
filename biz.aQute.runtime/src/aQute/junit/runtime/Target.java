package aQute.junit.runtime;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

import aQute.junit.osgi.*;

public class Target {
    List                   testNames  = new ArrayList();
    List                   flattened;
    int                    port       = -1;
    boolean                keepAlive;
    String                 target;
    boolean                deferred   = false;
    boolean                clear      = false;
    boolean                verbose    = true;
    final Properties       properties = new Properties();
    final GenericFramework framework  = new GenericFramework(properties);
    String                 reportName = "test-report.xml";
    boolean                waitForEver;

    /*
     * -version 3 -port 55310 -testLoaderClass
     * org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader
     * -loaderpluginname org.eclipse.jdt.junit.runtime -testNameFile
     * /tmp/testNames18041.txt
     */

    public static void main(String[] args) {
        Target target = new Target();

        int result = target.run(args);
        if (!target.keepAlive)
            System.exit(result);
    }

    int run(String args[]) {
        try {
            init(args);
            if (framework.activate()) {
                boolean report = properties.containsKey("report");
                if (report)
                    framework.report(System.out);

                Bundle targetBundle = framework.getBundle(target);
                if (targetBundle == null)
                    error("No target specified", null);
                else {
                    if (port == -1) {
                        XMLReport xmlreport = new XMLReport(reportName);
                        try {
                            addXML(targetBundle, xmlreport);
                            return doTesting(xmlreport, targetBundle);
                        } finally {
                            printSummary(xmlreport);
                            xmlreport.close();
                        }
                    } else
                        return doTesting(new JUnitReport(port), targetBundle);
                }
            }
        } catch (Throwable e) {
            error("bnd runtime", e);
            framework.report(System.out);
        }
        return -1;
    }

    private void printSummary(XMLReport xmlreport) {
        // Report the summary info.
        System.out.println("SUMMARY");
        String clazz = null;
        for (Iterator i = xmlreport.logs.iterator(); i.hasNext();) {
            XMLReport.LogEntry le = (XMLReport.LogEntry) i.next();
            if (!le.clazz.equals(clazz))
                System.out.println(le.clazz);
            clazz = le.clazz;
            System.out.print("   " + le.name);
            for (int n = le.name.length(); n < 50; n++)
                System.out.print(" ");
            System.out.println(le.message);
        }
    }

    private void addXML(Bundle targetBundle, XMLReport xmlreport) {
        String header = (String) targetBundle.getHeaders().get(
                "Bnd-AddXMLToTest");
        if (header == null)
            return;

        System.out.println("bnd-AddXMLToTest " + header);
        try {
            StringTokenizer st = new StringTokenizer(header, " ,");

            while (st.hasMoreTokens()) {
                String resource = st.nextToken();
                URL url = targetBundle.getEntry(resource);
                System.out.println("bnd-AddXMLToTest " + resource + " " + url);
                if (url != null) {
                    String name = url.getFile();
                    int n = name.lastIndexOf('/');
                    if (n < 0)
                        n = 0;
                    else
                        n = n + 1;

                    if (name.endsWith(".xml"))
                        name = name.substring(n, name.length() - 4);
                    else
                        name = name.substring(n, name.length()).replace('.',
                                '_');
                    xmlreport.addXML(name, url);
                } else {
                    error("Can't find indicated XML to add to test report: "
                            + resource, null);
                }
            }
        } catch (Exception e) {
            error("Trying to add extra html for test report: ", e);
        }
    }

    void init(String args[]) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if ("-version".equals(args[i])) {
                if (!"3".equals(args[++i].trim()))
                    throw new RuntimeException(
                            "This target only works with JUnit protocol #3");
            } else if ("-port".equals(args[i]))
                port = Integer.parseInt(args[++i]);
            else if ("-deferred".equals(args[i])) {
                deferred = true;
            } else if ("-clear".equals(args[i])) {
                clear = true;
            } else if ("-testNameFile".equals(args[i]))
                processFile(new File(args[++i]));
            else if ("-testLoaderClass".equals(args[i])) // From old
                // interface
                i++;
            else if ("-loaderpluginname".equals(args[i])) // From
                // old
                // interface
                i++;
            else if ("-test".equals(args[i]))
                testNames.add(args[++i]);
            else if ("-classNames".equals(args[i]))
                testNames.add(args[++i]);
            else if ("-bundle".equals(args[i]))
                framework.addBundle(new File(args[++i]));
            else if ("-export".equals(args[i]))
                framework.addSystemPackage(args[++i]);
            else if ("-framework".equals(args[i]))
                framework.setFramework(args[++i]);
            else if ("-keepalive".equals(args[i]))
                keepAlive = true;
            else if ("-set".equals(args[i])) {
                properties.setProperty(args[++i], clean(args[++i]));
            } else if ("-target".equals(args[i])) {
                target = args[++i];
                framework.addBundle(new File(target));
            } else if ("-storage".equals(args[i])) {
                framework.setStorage(new File(args[++i]));
            } else if ("-report".equals(args[i]))
                reportName = args[++i];
            else if ("-verbose".equals(args[i])) {
                verbose = true;
                System.out.println("bnd OSGi Runtime");
            } else
                warning("Do not understand arg: " + args[i]);
        }
        System.getProperties().putAll(properties);
    }

    private String clean(String string) {
        string = string.trim();
        if ( string.startsWith("\""))
            string = string.substring(1);
        if ( string.endsWith("\""))
            string = string.substring(0, string.length()-1);
        return string;
    }

    /**
     * We are not started from JUnit. This means we start the environment,
     * install, the bundles, and run. If the target bundle has a Test-Cases
     * header, we will run JUnit tests on that class.
     */
    public static List checkTestCases(Bundle bundle, List testNames)
            throws Exception {
        String testcases = (String) bundle.getHeaders().get("Test-Cases");
        if (testcases == null)
            return testNames;

        String[] classes = Activator.split(testcases, ",");
        for (int i = 0; i < classes.length; i++)
            testNames.add(classes[i]);

        return testNames;
    }

    /**
     * Main test routine.
     * 
     * @param tl
     * @param framework
     * @param targetBundle
     * @param testNames
     * @return
     * @throws Throwable
     */
    private int doTesting(TestReporter tl, Bundle targetBundle)
            throws Throwable {

        // Verify if we have any test names set
        if (testNames.size() == 0)
            testNames = checkTestCases(targetBundle, testNames);

        if (testNames.size() == 0) {
            System.out
                    .println("No test cases to run, waiting for the framework to quit");
            framework.waitForStop(0);
            System.out.println("And the framework is gone!");
            return 0;
        }

        Bundle fw = framework.getFrameworkBundle();
        List names = testNames;

        TestResult result = new TestResult();
        BasicTestReport otl = new BasicTestReport();
        try {
            TestSuite suite = createSuite(targetBundle, names);
            List flattened = new ArrayList();
            int realcount = flatten(flattened, suite);
            tl.begin(fw, targetBundle, flattened, realcount);
            otl.begin(fw, targetBundle, flattened, realcount);
            result.addListener(tl);
            result.addListener(otl);
            suite.run(result);

            if (result.wasSuccessful())
                return 0;
            else
                return otl.errors;
        } catch (Throwable t) {
            result.addError(null, t);
            t.printStackTrace();
            throw t;
        } finally {
            tl.end();
            otl.end();
            if (properties.containsKey("wait")) {
                framework.waitForStop(10000000);
            }
            framework.deactivate();
        }
    }

    public static int flatten(List list, TestSuite suite) {
        int realCount = 0;
        for (Enumeration e = suite.tests(); e.hasMoreElements();) {
            Test test = (Test) e.nextElement();
            list.add(test);
            if (test instanceof TestSuite)
                realCount += flatten(list, (TestSuite) test);
            else
                realCount++;
        }
        return realCount;
    }

    /**
     * Convert the test names to a test suite.
     * 
     * @param tfw
     * @param testNames
     * @return
     * @throws Exception
     */
    public static TestSuite createSuite(Bundle tfw, List testNames)
            throws Exception {
        TestSuite suite = new TestSuite();
        for (Iterator i = testNames.iterator(); i.hasNext();) {
            String fqn = (String) i.next();
            int n = fqn.indexOf(':');
            if (n > 0) {
                String method = fqn.substring(n + 1);
                fqn = fqn.substring(0, n);
                Class clazz = tfw.loadClass(fqn);
                suite.addTest(TestSuite.createTest(clazz, method));
            } else {
                Class clazz = tfw.loadClass(fqn);
                suite.addTestSuite(clazz);
            }
        }
        return suite;
    }

    private void warning(String string) {
        System.out.println("warning: " + string);
    }

    private void error(String string, Throwable e) {
        if (e != null) {
            if (e instanceof BundleException)
                GenericFramework.report((BundleException) e, System.out);
            else {
                System.out.println(string + " : " + e);

                if (verbose)
                    e.printStackTrace();
            }
        } else
            System.out.println("ERROR: " + string);
    }

    private void processFile(File file) throws IOException {
        FileReader rdr = new FileReader(file);
        BufferedReader brdr = new BufferedReader(rdr);
        String line = brdr.readLine();
        while (line != null) {
            testNames.add(line.trim());
            line = brdr.readLine();
        }
        rdr.close();
    }

}
