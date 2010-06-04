package aQute.bnd.launch;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.lib.osgi.*;

public class LaunchDelegate extends JavaLaunchDelegate {
    ProjectLauncher launcher;
    
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {
        IJavaProject javaProject = getJavaProject(configuration);
        Project project = Activator.getDefault().getCentral().getModel(javaProject);
        try {
            launcher = project.getProjectLauncher();
            super.launch(configuration, mode, launch, monitor);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String verifyMainTypeName(ILaunchConfiguration configuration)
            throws CoreException {
        return launcher.getMainTypeName();
    }

    public String getVMArguments(ILaunchConfiguration c) {
        return Processor.join(launcher.getRunVM(), " ");
    }

    public String getProgramArguments(ILaunchConfiguration c) {
        return Processor.join(launcher.getArguments(), " ");
    }

    public String[] getClasspath(ILaunchConfiguration configuration) {
        return launcher.getClasspath().toArray(new String[0]);
    }
}
