package aQute.junit;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

public class JunitXmlReport implements TestReporter {
	Tag					testsuite	= new Tag("testsuite");
	Tag					testcase;
	static String		hostname;
	static DateFormat	df			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	long				startTime;
	long				testStartTime;
	int					tests		= 0;
	PrintWriter			out;
	boolean				finished;
	boolean				progress;
	Bundle				bundle;
	BasicTestReport		basic;

	public class LogEntry {
		String	clazz;
		String	name;
		String	message;
	}

	public JunitXmlReport(Writer report, Bundle bundle, BasicTestReport basic) throws Exception {
		if (hostname == null)
			hostname = InetAddress.getLocalHost().getHostName();
		out = new PrintWriter(report);
		this.bundle = bundle;
		this.basic = basic;
	}

	public void setProgress(boolean progress) {
		this.progress = progress;
	}

	public void setup(Bundle fw, Bundle targetBundle) {
		startTime = System.currentTimeMillis();

		testsuite.addAttribute("hostname", hostname);
		if (targetBundle != null) {
			testsuite.addAttribute("name", "test." + targetBundle.getSymbolicName());
			testsuite.addAttribute("target", targetBundle.getLocation());
		} else {
			testsuite.addAttribute("name", "test.run");

		}
		testsuite.addAttribute("timestamp", df.format(new Date()));
		testsuite.addAttribute("framework", fw);
		testsuite.addAttribute("framework-version", fw.getVersion());

		Tag properties = new Tag("properties");
		testsuite.addContent(properties);

		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
			Tag property = new Tag(properties, "property");
			property.addAttribute("name", entry.getKey());
			property.addAttribute("value", entry.getValue());
		}

		Tag bundles = new Tag(testsuite, "bundles");
		Bundle bs[] = fw.getBundleContext().getBundles();

		for (int i = 0; i < bs.length; i++) {
			Tag bundle = new Tag("bundle");
			bundle.addAttribute("location", bs[i].getLocation());
			bundle.addAttribute("modified", df.format(new Date(bs[i].getLastModified())));
			bundle.addAttribute("state", bs[i].getState());
			bundle.addAttribute("id", bs[i].getBundleId() + "");
			bundle.addAttribute("bsn", bs[i].getSymbolicName());
			bundle.addAttribute("version", bs[i].getVersion());

			if (bs[i].equals(targetBundle))
				bundle.addAttribute("target", "true");

			bundles.addContent(bundle);
		}
		if (bundle != null) {
			String header = (String) targetBundle.getHeaders().get("Bnd-AddXMLToTest");
			if (header != null) {
				StringTokenizer st = new StringTokenizer(header, " ,");

				while (st.hasMoreTokens()) {
					String resource = st.nextToken();
					URL url = targetBundle.getEntry(resource);

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
							name = name.substring(n, name.length()).replace('.', '_');

						testsuite.addContent(url);

					} else {
						Tag addxml = new Tag(testsuite, "error");
						addxml.addAttribute("reason", "no such resource: " + resource);
					}
				}
			}
		}
	}

	public void begin(List classNames, int realcount) {
	}

	public void end() {
		if (!finished) {
			finished = true;
			testsuite.addAttribute("tests", tests);
			testsuite.addAttribute("time",
					getFraction(System.currentTimeMillis() - startTime, 1000));
			testsuite.addAttribute("timestamp", df.format(new Date()));
			testsuite.print(0, out);
			out.close();
		}
	}

	private String getFraction(long l, int i) {
		return (l / 1000) + "." + (l % 1000);
	}

	// <testcase classname="test.AnnotationsTest" name="testComponentReader"
	// time="0.045" />
	public void startTest(Test test) {
		testcase = new Tag("testcase");
		testsuite.addContent(testcase);
		testcase.addAttribute("classname", test.getClass().getName());
		String nameAndClass = test.toString();
		String name = nameAndClass;

		int n = nameAndClass.indexOf('(');
		if (n > 0 && nameAndClass.endsWith(")")) {
			name = nameAndClass.substring(0, n);
		}

		testcase.addAttribute("name", name);
		testStartTime = System.currentTimeMillis();
		progress(name);
	}

	public void setTests(List<Test> flattened) {
	}

	// <testcase classname="test.AnalyzerTest" name="testMultilevelInheritance"
	// time="0.772">
	// <error type="java.lang.Exception">java.lang.Exception:
	// at test.AnalyzerTest.testMultilevelInheritance(AnalyzerTest.java:47)
	// </error>
	// </testcase>

	public void addError(Test test, Throwable t) {
		Tag error = new Tag("error");
		error.setCDATA();
		error.addAttribute("type", t.getClass().getName());
		error.addContent(getTrace(t));
		if (testcase == null)
			testsuite.addContent(error);
		else
			testcase.addContent(error);
		progress(" e");
	}

	private void progress(String s) {
	}

	private String getTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(t.toString());

		StackTraceElement ste[] = t.getStackTrace();
		for (int i = 0; i < ste.length; i++) {
			pw.println("at " + ste[i].toString().trim());
		}
		pw.close();
		return sw.toString();
	}

	// <testcase classname="test.AnalyzerTest" name="testFindClass"
	// time="0.0050">
	// <failure
	// type="junit.framework.AssertionFailedError">junit.framework.AssertionFailedError
	// at test.AnalyzerTest.testFindClass(AnalyzerTest.java:25)
	// </failure>
	// <testcase>
	//
	public void addFailure(Test test, AssertionFailedError t) {
		Tag failure = new Tag("failure");
		failure.setCDATA();
		failure.addAttribute("type", t.getClass().getName());
		failure.addContent(getTrace(t));
		testcase.addContent(failure);
		progress(" f");
	}

	public void endTest(Test test) {
		String[] outs = basic.getCaptured();
		if (outs[0] != null) {
			Tag sysout = new Tag(testcase, "sys-out");
			sysout.addContent(outs[0]);
		}

		if (outs[1] != null) {
			Tag sysout = new Tag(testcase, "sys-err");
			sysout.addContent(outs[1]);
		}

		testcase
				.addAttribute("time", getFraction(System.currentTimeMillis() - testStartTime, 1000));
	}

	public void close() {
		end();
	}

	public void aborted() {
		testsuite.addAttribute("aborted", "true");
		close();
	}

	public void addTag(Tag tag) {
		testsuite.addContent(tag);
	}

}