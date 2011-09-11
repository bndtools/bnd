package bndtools.launch;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate2;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.service.EclipseJUnitTester;
import bndtools.Plugin;
import bndtools.utils.BundleUtils;

public class OSGiJUnitLaunchDelegate extends AbstractOSGiLaunchDelegate implements ILaunchConfigurationDelegate2 {

    private static final String JDT_JUNIT_BSN = "org.eclipse.jdt.junit";
    private static final String ATTR_JUNIT_PORT = "org.eclipse.jdt.junit.PORT";

    private ProjectTester bndTester;
    private EclipseJUnitTester bndEclipseTester;

    // A couple of hacks to make sure the JUnit plugin is active and notices our launch.
    @Override
    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
        // start the JUnit plugin
        try {
            Bundle jdtJUnitBundle = BundleUtils.findBundle(Plugin.getDefault().getBundleContext(), JDT_JUNIT_BSN, null);
            if(jdtJUnitBundle == null)
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bundle \"{0}\" was not found. Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
            jdtJUnitBundle.start();
        } catch (BundleException e) {
            throw new  CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error starting bundle \"{0}\". Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
        }

        // JUnit plugin ignores the launch unless attribute "org.eclipse.jdt.launching.PROJECT_ATTR" is set.
        ILaunchConfigurationWorkingCopy modifiedConfig = configuration.getWorkingCopy();
        String launchTarget = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
        if(launchTarget != null) {
            IResource launchResource = ResourcesPlugin.getWorkspace().getRoot().findMember(launchTarget);
            IProject launchProject = launchResource.getProject();
            modifiedConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, launchProject.getName());
        }

        return super.getLaunch(modifiedConfig.doSave(), mode);
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        waitForBuilds(progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        try {
            Project project = LaunchUtils.getBndProject(configuration);
            synchronized (project) {
                bndTester = project.getProjectTester();
            }

            if (bndTester instanceof EclipseJUnitTester)
                bndEclipseTester = (EclipseJUnitTester) bndTester;

            configureTester(configuration, launch);
            bndTester.prepare();

        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error obtaining OSGi project tester.", e));
        }

        super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    private void configureTester(ILaunchConfiguration configuration, ILaunch launch) throws CoreException {
        assertBndEclipseTester();

        // Find free socket for JUnit protocol
        int port = SocketUtil.findFreePort();
        bndEclipseTester.setPort(port);
        launch.setAttribute(ATTR_JUNIT_PORT, Integer.toString(port));

        // Enable tracing?
        bndTester.getProjectLauncher().setTrace(enableTraceOption(configuration));

        // Keep alive?
        bndTester.setContinuous(enableKeepAlive(configuration));

        // Check if we were asked to re-run a specific test class/method
        String testClass = configuration.getAttribute("org.eclipse.jdt.launching.MAIN_TYPE", (String) null);
        String testMethod = configuration.getAttribute("org.eclipse.jdt.junit.TESTNAME", (String) null);
        if (testClass != null) {
            String testName = testClass;
            if (testMethod != null)
                testName += ":" + testMethod;
            bndTester.addTest(testName);
        }
    }

    private void assertBndEclipseTester() throws CoreException {
        if (bndEclipseTester == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd/Eclipse tester was not initialised.", null));
    }

    @SuppressWarnings("deprecation")
    private boolean enableKeepAlive(ILaunchConfiguration configuration) throws CoreException {
        boolean keepAlive = configuration.getAttribute(LaunchConstants.ATTR_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
        if (keepAlive == LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE) {
            keepAlive = configuration.getAttribute(LaunchConstants.ATTR_OLD_JUNIT_KEEP_ALIVE, LaunchConstants.DEFAULT_JUNIT_KEEP_ALIVE);
        }
        return keepAlive;
    }

    @Override
    protected ProjectLauncher getProjectLauncher() throws CoreException {
        if (bndTester == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd tester was not initialised.", null));
         return bndTester.getProjectLauncher();
    }

}