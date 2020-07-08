package aQute.bnd.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.reporter.ReporterMessages;
import aQute.service.reporter.Reporter;

public class BaseTask extends Task implements Reporter {
	private final static Logger	logger			= LoggerFactory.getLogger(BaseTask.class);
	ReporterAdapter				reporter		= new ReporterAdapter();

	List<String>				errors			= new ArrayList<>();
	List<String>				warnings		= new ArrayList<>();
	List<String>				progress		= new ArrayList<>();
	boolean						pedantic;
	boolean						trace;
	String						onfail;
	final List<Property>		properties		= new LinkedList<>();
	final List<Property>		workspaceProps	= new LinkedList<>();
	final AntMessages			messages		= ReporterMessages.base(this, AntMessages.class);
	boolean						exceptions;

	static {
		Workspace.setDriver(Constants.BNDDRIVER_ANT);
		Workspace.addGestalt(Constants.GESTALT_BATCH, null);
	}

	protected boolean report() {
		return report(this);
	}

	protected boolean report(Reporter reporter) {
		int errCount = reporter.getErrors()
			.size();
		if (errCount > 0) {
			System.err.printf("%d ERRORS%n", errCount);
			for (String e : reporter.getErrors()) {
				System.err.println(" " + e);
			}
			return true;
		}
		int warnCount = reporter.getWarnings()
			.size();
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
		} catch (IOException e) {
			return f.getAbsoluteFile();
		}
	}

	protected List<String> split(String dependsOn, String string) {
		if (dependsOn == null)
			return new ArrayList<>();

		return Arrays.asList(string.split("\\s*" + string + "\\s*"));
	}

	protected String join(Collection<?> classpath, String string) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object name : classpath) {
			sb.append(del);
			sb.append(name);
			del = string;
		}
		return sb.toString();
	}

	@Override
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

	/**
	 * @deprecated Use SLF4J Logger.debug instead.
	 */
	@Override
	@Deprecated
	public void trace(String s, Object... args) {
		if (logger.isDebugEnabled()) {
			logger.debug("{}", Strings.format(s, args));
		}
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

	@Override
	public Location getLocation(String msg) {
		return reporter.getLocation(msg);
	}

	@Override
	public boolean isOk() {
		return reporter.isOk();
	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		return reporter.exception(t, format, args);
	}

	@Override
	public SetLocation error(String s, Object... args) {
		return reporter.error(s, args);
	}

	@Override
	public List<String> getErrors() {
		return reporter.getErrors();
	}

	@Override
	public List<String> getWarnings() {
		return reporter.getWarnings();
	}

	/**
	 * @deprecated Use SLF4J Logger.info() instead.
	 */
	@Override
	@Deprecated
	public void progress(float progress, String s, Object... args) {
		if (logger.isInfoEnabled()) {
			String message = Strings.format(s, args);
			if (progress > 0)
				logger.info("[{}] {}", (int) progress, message);
			else
				logger.info("{}", message);
		}
	}

	@Override
	public SetLocation warning(String s, Object... args) {
		return reporter.warning(s, args);
	}
}
