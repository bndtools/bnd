package bndtools.core.test.utils;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;

public class LoggingProgressMonitor implements IProgressMonitor {

	int						totalWork	= -1;
	String					task;
	String					subTask;
	int						wip;
	boolean					canceled;
	final Supplier<String>	context;

	public LoggingProgressMonitor(String context) {
		this.context = () -> context;
	}

	public LoggingProgressMonitor(Supplier<String> context) {
		this.context = context;
	}

	private String getTaskProgress() {
		return task == null ? "<unnamed task>"
			: (subTask == null ? task : task + "/" + subTask) + ", " + wip + "/" + totalWork;
	}

	private void log(String msg) {
		TaskUtils.log(() -> context.get() + ": " + getTaskProgress() + ": " + msg);
	}

	private void log(Supplier<String> msg) {
		TaskUtils.log(() -> context.get() + ": " + getTaskProgress() + ": " + msg.get());
	}

	@Override
	public void beginTask(String name, int totalWork) {
		task = name;
		subTask = null;
		this.totalWork = totalWork;
		wip = 0;
		canceled = false;
		log("beginTask");
	}

	@Override
	public void done() {
		log("done");
	}

	@Override
	public void internalWorked(double work) {
		log(() -> "internalWorked(" + work + ")");
	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	@Override
	public void setCanceled(boolean value) {
		canceled = value;
		log(() -> "setCanceled(" + value + ")");
	}

	@Override
	public void setTaskName(String name) {
		subTask = null;
		task = name;
		log(() -> "setTaskName(" + name + ")");
	}

	@Override
	public void subTask(String name) {
		subTask = name;
		log(() -> "subTask(" + name + ")");
	}

	@Override
	public void worked(int work) {
		wip += work;
		log(() -> "worked(" + work + ")");
	}
}
