package org.bndtools.builder.jobs.newproject;

import org.bndtools.api.IStartupParticipant;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

public class NewProjectJobsStartupParticipant implements IStartupParticipant {

	private final IResourceChangeListener listener = new NewProjectResourceListener();

	@Override
	public void start() {
		ResourcesPlugin.getWorkspace()
			.addResourceChangeListener(listener);
	}

	@Override
	public void stop() {
		ResourcesPlugin.getWorkspace()
			.removeResourceChangeListener(listener);
	}

}
