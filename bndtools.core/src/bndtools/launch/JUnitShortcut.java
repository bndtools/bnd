package bndtools.launch;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;


public class JUnitShortcut extends AbstractLaunchShortcut {
    public JUnitShortcut() {
        super(LaunchConstants.LAUNCH_ID_OSGI_JUNIT);
    }

    @Override
    ILaunchConfigurationWorkingCopy createConfiguration(IPath targetPath) throws Exception {
        ILaunchConfigurationWorkingCopy wc = super.createConfiguration(targetPath);
        wc.setAttribute(LaunchConstants.ATTR_JUNIT_REPORTER, LaunchConstants.DEFAULT_JUNIT_REPORTER);
        return wc;
    }
}