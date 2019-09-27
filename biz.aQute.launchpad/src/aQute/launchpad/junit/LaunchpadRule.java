package aQute.launchpad.junit;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import aQute.launchpad.Launchpad;
import aQute.launchpad.LaunchpadBuilder;
import aQute.lib.exceptions.Exceptions;

public class LaunchpadRule extends TestWatcher {
	private String				name;
	private String				className;
	private LaunchpadBuilder	builder;
	private Launchpad			launchpad;

	public LaunchpadRule(LaunchpadBuilder builder) {
		this.builder = builder;
	}

	@Override
	protected void starting(Description d) {
		name = d.getMethodName();
		className = d.getClassName();
	}

	@Override
	protected void finished(Description description) {
		super.finished(description);
		if (launchpad == null) {
			return;
		}
		try {
			this.launchpad.close();
		} catch (Exception e) {
			Exceptions.duck(e);
		}
	}

	/**
	 * @return the name of the currently-running test method
	 */
	public String getMethodName() {
		return name;
	}

	public synchronized Launchpad getLaunchpad() {
		if (this.launchpad == null) {
			this.launchpad = builder.create(name, className);
		}
		return launchpad;
	}
}
