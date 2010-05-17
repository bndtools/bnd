package aQute.bnd.launch;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.lib.osgi.*;

public class LaunchDelegate extends JavaLaunchDelegate {
    String   programArguments;
    String   vmArguments;
    String[] classpath;

    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {
        IJavaProject javaProject = getJavaProject(configuration);
        Project model = Activator.getDefault().getCentral().getModel(javaProject);
        try {
            ProjectLauncher launcher = new ProjectLauncher(model);
            List<String> vmArguments = new ArrayList<String>();
            List<String> programArguments = new ArrayList<String>();
//            launcher.getArguments(vmArguments, programArguments, false);
//            this.classpath = launcher.getClasspath();
            this.vmArguments = Processor.join(vmArguments, " ");
            this.programArguments = Processor.join(programArguments, " ");
            super.launch(configuration, mode, launch, monitor);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
