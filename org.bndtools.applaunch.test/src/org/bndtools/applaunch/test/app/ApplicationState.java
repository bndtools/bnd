package org.bndtools.applaunch.test.app;

import java.util.List;

import org.eclipse.equinox.app.IApplicationContext;

/**
 * What the {@link HeadlessApplication} observed when it was started. Registered
 * as a service so the tests can inspect it.
 */
public final class ApplicationState {
	private final String				threadName;
	private final List<String>			applicationArgs;
	private final IApplicationContext	applicationContext;

	ApplicationState(String threadName, List<String> applicationArgs, IApplicationContext applicationContext) {
		this.threadName = threadName;
		this.applicationArgs = List.copyOf(applicationArgs);
		this.applicationContext = applicationContext;
	}

	/** The name of the thread {@code IApplication.start} was called on. */
	public String threadName() {
		return threadName;
	}

	/** The {@code IApplicationContext.APPLICATION_ARGS} handed to the application. */
	public List<String> applicationArgs() {
		return applicationArgs;
	}

	public IApplicationContext applicationContext() {
		return applicationContext;
	}
}
