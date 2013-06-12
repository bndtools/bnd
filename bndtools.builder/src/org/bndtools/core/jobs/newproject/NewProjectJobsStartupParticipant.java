package org.bndtools.core.jobs.newproject;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

import bndtools.api.IStartupParticipant;

public class NewProjectJobsStartupParticipant implements IStartupParticipant {

    private final IResourceChangeListener listener = new NewProjectResourceListener();

    public void start() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
    }

    public void stop() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
    }

}
