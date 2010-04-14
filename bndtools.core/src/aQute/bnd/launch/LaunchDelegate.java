package aQute.bnd.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Project;
import aQute.bnd.plugin.Activator;
import aQute.bnd.test.ProjectLauncher;
import aQute.lib.osgi.Processor;
import bndtools.Plugin;

public class LaunchDelegate extends JavaLaunchDelegate {
    String   programArguments;
    String   vmArguments;
    String[] classpath;

    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        IJavaProject javaProject = getJavaProject(configuration);
        Project model = Activator.getDefault().getCentral().getModel(javaProject);
        try {
            synchronized (model) {
                ProjectLauncher launcher = new ProjectLauncher(model);
                List<String> vmArguments = new ArrayList<String>();
                List<String> programArguments = new ArrayList<String>();
                launcher.getArguments(vmArguments, programArguments, false);
                programArguments.add("-keepalive");
                this.classpath = launcher.getClasspath();
                this.vmArguments = Processor.join(vmArguments, " ");
                this.programArguments = Processor.join(programArguments, " ");
            }
            super.launch(configuration, mode, launch, monitor);
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Launch error", e));
        }
    }

    public String verifyMainTypeName(ILaunchConfiguration configuration)
            throws CoreException {
        return "aQute.junit.runtime.Target";
    }

    public String getVMArguments(ILaunchConfiguration c) {
        return vmArguments;
    }

    public String getProgramArguments(ILaunchConfiguration c) {
        return programArguments;
    }

    public String[] getClasspath(ILaunchConfiguration configuration) {
        return classpath;
    }
}
