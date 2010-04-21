package bndtools.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

public class OSGiJUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate {

    private final OSGiLaunchDelegate delegate = new OSGiLaunchDelegate();

    @Override
    public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        delegate.generateLaunchPropsFile(configuration);

        super.launch(configuration, mode, launch, monitor);
    }

    @Override
    public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        return delegate.getMainTypeName(configuration);
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        return delegate.getProgramArguments(configuration);
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        return delegate.getClasspath(configuration);
    }
}