package org.bndtools.utils.progress;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Reporter;

public class ProgressMonitorReporter implements Reporter {

	private static final int		TOTAL			= 1000;

	private final IProgressMonitor	monitor;

	private final List<String>		warnings		= new LinkedList<>();
	private final List<String>		errors			= new LinkedList<>();

	private int						lastCumulWorked	= 0;

	public ProgressMonitorReporter(IProgressMonitor monitor, String taskName) {
		this.monitor = monitor;
		monitor.beginTask(taskName, TOTAL);
	}

	@Override
	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	@Override
	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	@Override
	public Location getLocation(String msg) {
		return null;
	}

	@Override
	public boolean isOk() {
		return !errors.isEmpty();
	}

	@Override
	public SetLocation error(String format, Object... args) {
		String message = Processor.formatArrays(format, args);
		errors.add(message);
		return null;
	}

	@Override
	public SetLocation warning(String format, Object... args) {
		String message = Processor.formatArrays(format, args);
		warnings.add(message);
		return null;
	}

	@Override
	@Deprecated
	public void trace(String format, Object... args) {
		// ignore
	}

	@Override
	@Deprecated
	public void progress(float progress, String format, Object... args) {
		int cumulWorked = (int) (TOTAL * progress);
		int worked = cumulWorked - lastCumulWorked;
		lastCumulWorked = cumulWorked;

		monitor.worked(worked);
	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		errors.add(t.getMessage());
		error(format, args);
		return null;
	}

	@Override
	public boolean isPedantic() {
		return false;
	}
}
