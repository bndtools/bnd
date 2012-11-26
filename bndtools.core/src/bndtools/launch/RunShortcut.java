package bndtools.launch;

import bndtools.launch.api.AbstractLaunchShortcut;

public class RunShortcut extends AbstractLaunchShortcut {
    public RunShortcut() {
        super(LaunchConstants.LAUNCH_ID_OSGI_RUNTIME);
    }
}
