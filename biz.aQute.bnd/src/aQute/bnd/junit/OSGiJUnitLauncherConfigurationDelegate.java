package aQute.bnd.junit;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.junit.launcher.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.bnd.test.*;

@SuppressWarnings("unchecked")
public class OSGiJUnitLauncherConfigurationDelegate extends
        JUnitLaunchConfigurationDelegate {

    ProjectLauncher launcher;
    
    public String verifyMainTypeName(ILaunchConfiguration configuration)
            throws CoreException {
        return "aQute.junit.runtime.Target";
    }

    // protected IMember[] evaluateTests(ILaunchConfiguration configuration,
    // IProgressMonitor monitor) throws CoreException {
    // System.out.println("Evaluate Tests");
    // return super.evaluateTests(configuration, monitor);
    // }

    protected void collectExecutionArguments(
            ILaunchConfiguration configuration, List/* String */vmArguments,
            List/* String */programArguments) throws CoreException {

        
        IJavaProject javaProject = getJavaProject(configuration);
        Project model = Activator.getDefault().getCentral().getModel(javaProject);
        model.clear();
        launcher = new ProjectLauncher(model);

        try {
            super.collectExecutionArguments(configuration, vmArguments,
                    programArguments);

            launcher.getArguments(vmArguments, programArguments, true);

            if (configuration.getAttribute(OSGiArgumentsTab.ATTR_KEEP, false))
                programArguments.add("-keep");

            if (launcher.isOk())
                return;

            String args = vmArguments + " " + programArguments + " "
                    + Arrays.toString(getClasspath(configuration));
            Activator.getDefault().report(true, false, launcher,
                    "Launching " + model, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(
                    new Status(IStatus.ERROR, "osgi.eclipse.junit",
                            "Building arguments for remote VM", e));
        }
        throw new CoreException(new Status(IStatus.ERROR, "osgi.eclipse.junit",
                "Building arguments for remote VM, project=" + model, null));
    }

    /**
     * Calculate the classpath. We include our own runtime.jar which includes
     * the test framework and we include the first of the test frameworks
     * specified.
     */
    public String[] getClasspath(ILaunchConfiguration configuration)
            throws CoreException {
        
        return launcher.getClasspath();
        /**
        try {
            IJavaProject javaProject = getJavaProject(configuration);
            Project model = Activator.getDefault().getCentral().getModel(javaProject);

            List<String> classpath = new ArrayList<String>();

            classpath.add(getRuntime().getAbsolutePath());

            for (Container c : model.getRunpath()) {
                if (c.getType() != Container.TYPE.ERROR) {
                    classpath.add(c.getFile().getAbsolutePath());
                } else {
                    abort("Invalid entry on the " + Constants.RUNPATH + ": "
                            + c, null, IStatus.ERROR);
                }
            }
            return classpath.toArray(new String[classpath.size()]);
        } catch (Exception e) {
            abort("Calculating class path", e, IStatus.ERROR);
        }
        return null;
        */
    }

    /**
     * Extract the runtime on the file system so we can refer to it. in the
     * remote VM.
     * 
     * @return
     */
    public File getRuntime() {
        return ProjectLauncher.getRuntime();
    }

}
