package aQute.junit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.osgi.framework.Bundle;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

public class JunitXmlReport implements TestReporter {
	private static final DateTimeFormatter	DATE_TIME_FORMATTER	= DateTimeFormatter.ISO_LOCAL_DATE_TIME
		.withLocale(Locale.ROOT)
		.withZone(ZoneId.systemDefault());
	private final Tag						testsuite	= new Tag("testsuite");
	private Tag								testcase;
	private static String					hostname;
	private long							startTime;
	private long							testStartTime;
	private final PrintWriter				out;
	private boolean							finished;
	private final BasicTestReport			basic;

	public JunitXmlReport(Writer report, Bundle bundle, BasicTestReport basic) throws Exception {
		try {
			if (hostname == null)
				hostname = InetAddress.getLocalHost()
					.getHostName();
		} catch (Exception e) {
			hostname = e.getMessage();
		}
		out = new PrintWriter(report);
		this.basic = basic;
	}

	public void setProgress(boolean progress) {}

	@Override
	public void setup(Bundle fw, Bundle targetBundle) {
		startTime = System.currentTimeMillis();

		testsuite.addAttribute("hostname", hostname);
		if (targetBundle != null) {
			testsuite.addAttribute("name", "test." + targetBundle.getSymbolicName());
			testsuite.addAttribute("target", targetBundle.getLocation());
		} else {
			testsuite.addAttribute("name", "test.run");

		}
		String timestamp = DATE_TIME_FORMATTER.format(Instant.now());
		testsuite.addAttribute("timestamp", timestamp);
		testsuite.addAttribute("framework", fw);
		testsuite.addAttribute("framework-version", fw.getVersion());

		Tag properties = new Tag("properties");
		testsuite.addContent(properties);

		for (String prop : System.getProperties()
			.stringPropertyNames()) {
			String value = System.getProperty(prop);
			if (value != null) {
				Tag property = new Tag(properties, "property");
				property.addAttribute("name", prop);
				property.addAttribute("value", value);
			}
		}

		if (targetBundle != null) {
			String header = targetBundle.getHeaders()
				.get(aQute.bnd.osgi.Constants.BND_ADDXMLTOTEST);
			if (header != null) {
				StringTokenizer st = new StringTokenizer(header, " ,");

				while (st.hasMoreTokens()) {
					String resource = st.nextToken();
					URL url = targetBundle.getEntry(resource);

					if (url != null) {
						testsuite.addContent(url);
					} else {
						Tag addxml = new Tag(testsuite, "error");
						addxml.addAttribute("message", "no such resource: " + resource);
					}
				}
			}
		}
	}

	@Override
	public void begin(List<Test> classNames, int realcount) {}

	@Override
	public void end() {
		if (!finished) {
			finished = true;
			testsuite.addAttribute("tests", basic.getTestResult()
				.runCount());
			testsuite.addAttribute("failures", basic.getTestResult()
				.failureCount());
			testsuite.addAttribute("errors", basic.getTestResult()
				.errorCount());
			testsuite.addAttribute("time", getFraction(System.currentTimeMillis() - startTime, 1000));
			String timestamp = DATE_TIME_FORMATTER.format(Instant.now());
			testsuite.addAttribute("timestamp", timestamp);
			testsuite.print(0, out);
			out.close();
		}
	}

	private String getFraction(long l, @SuppressWarnings("unused") int i) {
		return (l / 1000) + "." + (l % 1000);
	}

	// <testcase classname="test.AnnotationsTest" name="testComponentReader"
	// time="0.045" />
	private final static Pattern NAMEANDCLASS = Pattern.compile("(.*)\\((.*)\\)");

	@Override
	public void startTest(Test test) {
		String nameAndClass = test.toString();
		String name = nameAndClass;
		String clazz = test.getClass()
			.getName();

		if (test instanceof Describable) {
			Description description = ((Describable) test).getDescription();
			clazz = description.getClassName();
			if (description.getMethodName() != null)
				name = description.getMethodName();
		} else {
			Matcher m = NAMEANDCLASS.matcher(nameAndClass);
			if (m.matches()) {
				name = m.group(1);
				clazz = m.group(2);
			}
		}

		testcase = new Tag("testcase");
		testsuite.addContent(testcase);
		testcase.addAttribute("classname", clazz);

		int n = nameAndClass.indexOf('(');
		if (n > 0 && nameAndClass.endsWith(")")) {
			name = nameAndClass.substring(0, n);
		}

		testcase.addAttribute("name", name);
		testStartTime = System.currentTimeMillis();
		progress(name);
	}

	public void setTests(@SuppressWarnings("unused") List<Test> flattened) {}

	// <testcase classname="test.AnalyzerTest" name="testMultilevelInheritance"
	// time="0.772">
	// <error type="java.lang.Exception">java.lang.Exception:
	// at test.AnalyzerTest.testMultilevelInheritance(AnalyzerTest.java:47)
	// </error>
	// </testcase>

	@Override
	public void addError(Test test, Throwable t) {
		Tag error = new Tag("error");
		error.setCDATA();
		error.addAttribute("type", t.getClass()
			.getName());
		String message = t.getMessage();
		if (message != null) {
			error.addAttribute("message", message);
		}
		error.addContent(getTrace(t));
		if (testcase == null)
			testsuite.addContent(error);
		else
			testcase.addContent(error);
		progress(" e");
	}

	private void progress(@SuppressWarnings("unused") String s) {}

	private String getTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		return sw.toString()
			.replace('\t', ' ');
	}

	// <testcase classname="test.AnalyzerTest" name="testFindClass"
	// time="0.0050">
	// <failure
	// type="junit.framework.AssertionFailedError">junit.framework.AssertionFailedError
	// at test.AnalyzerTest.testFindClass(AnalyzerTest.java:25)
	// </failure>
	// <testcase>
	//
	@Override
	public void addFailure(Test test, AssertionFailedError t) {
		Tag failure = new Tag("failure");
		failure.setCDATA();
		failure.addAttribute("type", t.getClass()
			.getName());
		String message = t.getMessage();
		if (message != null) {
			failure.addAttribute("message", message);
		}
		failure.addContent(getTrace(t));
		testcase.addContent(failure);
		progress(" f");
	}

	@Override
	public void endTest(Test test) {
		String[] outs = basic.getCaptured();
		if (outs[0] != null) {
			Tag sysout = new Tag(testcase, "system-out");
			sysout.setCDATA();
			sysout.addContent(outs[0]);
		}

		if (outs[1] != null) {
			Tag syserr = new Tag(testcase, "system-err");
			syserr.setCDATA();
			syserr.addContent(outs[1]);
		}

		testcase.addAttribute("time", getFraction(System.currentTimeMillis() - testStartTime, 1000));
	}

	public void close() {
		end();
	}

	@Override
	public void aborted() {
		testsuite.addAttribute("aborted", "true");
		close();
	}

	public void addTag(Tag tag) {
		testsuite.addContent(tag);
	}

}
