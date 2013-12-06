package aQute.bnd.ant;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

import aQute.libg.reporter.*;
import aQute.service.reporter.*;

public class BaseTask extends Task implements Reporter {
	ReporterAdapter			reporter= new ReporterAdapter();
	
	List<String>			errors			= new ArrayList<String>();
	List<String>			warnings		= new ArrayList<String>();
	List<String>			progress		= new ArrayList<String>();
	boolean					pedantic;
	boolean					trace;
	String					onfail;
	final List<Property>	properties		= new LinkedList<Property>();
	final List<Property>	workspaceProps	= new LinkedList<Property>();
	final AntMessages		messages		= ReporterMessages.base(this, AntMessages.class);
	boolean					exceptions;


	protected boolean report() {
		return report(this);
	}

	protected boolean report(Reporter reporter) {
		int errCount = reporter.getErrors().size();
		if (errCount > 0) {
			System.err.printf("%d ERRORS%n", errCount);
			for (String e : reporter.getErrors()) {
				System.err.println(" " + e);
			}
			return true;
		}
		int warnCount = reporter.getWarnings().size();
		if (warnCount > 0) {
			System.err.printf("%d WARNINGS%n", warnCount);
			for (String w : reporter.getWarnings()) {
				System.err.println(" " + w);
			}
		}
		return false;
	}

	public static File getFile(File base, String file) {
		File f = new File(file);
		if (!f.isAbsolute()) {
			int n;

			f = base.getAbsoluteFile();
			while ((n = file.indexOf('/')) > 0) {
				String first = file.substring(0, n);
				file = file.substring(n + 1);
				if (first.equals(".."))
					f = f.getParentFile();
				else
					f = new File(f, first);
			}
			f = new File(f, file);
		}
		try {
			return f.getCanonicalFile();
		}
		catch (IOException e) {
			return f.getAbsoluteFile();
		}
	}

	protected List<String> split(String dependsOn, String string) {
		if (dependsOn == null)
			return new ArrayList<String>();

		return Arrays.asList(string.split("\\s*" + string + "\\s*"));
	}

	protected String join(Collection< ? > classpath, String string) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object name : classpath) {
			sb.append(del);
			sb.append(name);
			del = string;
		}
		return sb.toString();
	}

	public boolean isPedantic() {
		return pedantic;
	}

	public void setPedantic(boolean pedantic) {
		this.pedantic = pedantic;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	public boolean isTrace() {
		return trace;
	}

	public void trace(String s, Object... args) {
		System.err.printf("# " + s + "%n", args);
	}

	public void addProperty(Property property) {
		properties.add(property);
	}

	public void addWsproperty(Property property) {
		workspaceProps.add(property);
	}

	public boolean isExceptions() {
		return exceptions;
	}

	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	public Location getLocation(String msg) {
		return reporter.getLocation(msg);
	}

	public boolean isOk() {
		return reporter.isOk();
	}

	public SetLocation exception(Throwable t, String format, Object... args) {
		return reporter.exception(t, format, args);
	}

	public SetLocation error(String s, Object... args) {
		return reporter.error(s, args);
	}

	public List<String> getErrors() {
		return reporter.getErrors();
	}

	public List<String> getWarnings() {
		return reporter.getWarnings();
	}

	public void progress(float progress, String s, Object... args) {
		reporter.progress(progress, s, args);
	}

	public SetLocation warning(String s, Object... args) {
		return reporter.warning(s, args);
	}
}
