package aQute.junit.runtime;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class XMLReport implements TestReporter {
    final File        file;
    final PrintStream out;
    List             /* <Test> */tests;
    String            openElement = null;
    boolean           finished;
    Map               urls        = new HashMap();
    List              logs        = new ArrayList();
    LogEntry          current;

    public class LogEntry {
        String clazz;
        String name;
        String message;
    }

    public XMLReport(String reportName) throws FileNotFoundException {
        file = new File(reportName);
        out = new PrintStream(new FileOutputStream(file));
    }

    public void begin(Bundle fw, Bundle targetBundle, List classNames,
            int realcount) {
        finished = false;
        out.println("<?xml version='1.0'?>");
        out.println("<testreport");
        out.println("    target='" + targetBundle.getLocation() + "'");
        out.println("    time='" + new Date() + "' ");
        out.println("    framework='" + fw + "'>");
        Bundle[] bundles = fw.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            out.println("  <bundle location='" + bundles[i].getLocation()
                    + "' ");
            out
                    .println("     modified='" + bundles[i].getLastModified()
                            + "' ");
            out.println("     state='" + bundles[i].getState() + "' ");
            out.println("     id='" + bundles[i].getBundleId() + "' ");
            out.println("     bsn='" + bundles[i].getSymbolicName() + "' ");
            out.println("     version='"
                    + bundles[i].getHeaders().get("Bundle-Version") + "' ");
            out.println("  />");
        }
    }

    public void end() {
        if (!finished) {
            for (Iterator i = urls.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                URL url = (URL) urls.get(name);
                out.println("  <" + name + ">");
                try {
                    InputStream in = url.openStream();
                    BufferedReader rdr = new BufferedReader(
                            new InputStreamReader(in));
                    try {
                        String line = rdr.readLine();
                        if (line != null) {
                            while (line.trim().startsWith("<?"))
                                line = rdr.readLine();

                            while (line != null) {
                                out.println(line);
                                line = rdr.readLine();
                            }
                        }
                    } finally {
                        in.close();
                    }
                } catch (Exception e) {
                    System.out.println("Problems copying extra XML");
                }
                out.println("  </" + name + ">");
            }

            out.println("</testreport>");
            out.close();
        }
        finished = true;
    }

    public void setTests(List flattened) {
        this.tests = flattened;
    }

    public void startTest(Test test) {

        String nameAndClass = test.toString();
        String name = nameAndClass;
        String clazz = "";

        int n = nameAndClass.indexOf('(');
        if (n > 0 && nameAndClass.endsWith(")")) {
            name = nameAndClass.substring(0, n);
            clazz = nameAndClass.substring(n + 1, nameAndClass.length() - 1);
        }
        out.print("  <test name='" + name + "' class='" + clazz + "'>");
        current = new LogEntry();
        current.name = name;
        current.clazz = clazz;
        current.message = "ok";
        logs.add(current);
    }

    public void addError(Test test, Throwable t) {
        current.message = t.getLocalizedMessage() + ": " + getTrace(t);
        out.println(" <error name='" + test + "' type='" + escape(t.toString())
                + "'>");
        out.println("<![CDATA[");
        if (t != null)
            t.printStackTrace(out);
        out.println("]]>");
        out.println(" </error>");
    }

    private String getTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public void addFailure(Test test, AssertionFailedError t) {
        current.message = t.getLocalizedMessage();
        out.println(" <failure name='" + test.toString() + "' type='"
                + t.getClass().getName() + "' message='"
                + escape(t.getMessage()) + "'>");
        out.println("<![CDATA[");
        t.printStackTrace(out);
        out.println("]]>");
        out.println(" </failure>");
    }

    private String escape(String message) {
        StringBuffer sb = new StringBuffer();
        if (message == null)
            return "";

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            switch (c) {
            case '>':
                sb.append("&gt;");
                break;

            case '<':
                sb.append("&lt;");
                break;

            case '&':
                sb.append("&amp;");
                break;

            case '\'':
                sb.append("&apos;");
                break;
            case '"':
                sb.append("&quot;");
                break;

            default:
                sb.append(c);

            }
        }
        return sb.toString();
    }

    public void endTest(Test test) {
        out.println("  </test>");
        out.flush();
    }

    public void aborted() {
        out.println("  </test>");
        out.println("<aborted/>");
        out.flush();
    }

    public void close() {
        end();
    }

    public void addXML(String string, URL resource) {
        if (resource == null)
            return;
        urls.put(string, resource);
    }
}
