package bndtools.launch;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.utils.osgi.BundleUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.service.EclipseJUnitTester;
import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.launch.util.LaunchUtils;

public class OSGiJUnitLaunchDelegate extends AbstractOSGiLaunchDelegate {
    static String JNAME_S = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    static Pattern FAILURES_P = Pattern.compile("^(" + JNAME_S + ")   # method name\n" //
        + "\\(" //
        + "     (" + JNAME_S + "(?:\\." + JNAME_S + ")*)          # fqn class name\n" //
        + "\\)$                                                   # close\n", //
        Pattern.UNIX_LINES + Pattern.MULTILINE + Pattern.COMMENTS);
    public static final String ORG_BNDTOOLS_TESTNAMES = "org.bndtools.testnames";
    private static final String JDT_JUNIT_BSN = "org.eclipse.jdt.junit";
    private static final String ATTR_JUNIT_PORT = "org.eclipse.jdt.junit.PORT";

    private int junitPort;
    private ProjectTester bndTester;
    private EclipseJUnitTester bndEclipseTester;

    @Override
    protected void initialiseBndLauncher(ILaunchConfiguration configuration, Project model) throws Exception {
        synchronized (model) {
            bndTester = model.getProjectTester();
        }

        if (bndTester instanceof EclipseJUnitTester)
            bndEclipseTester = (EclipseJUnitTester) bndTester;
        junitPort = configureTester(configuration);
        bndTester.prepare();
    }

    @Override
    public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
        boolean result = super.finalLaunchCheck(configuration, mode, monitor);

        // Trigger opening of the JUnit view
        Status junitStatus = new Status(IStatus.INFO, Plugin.PLUGIN_ID, LaunchConstants.LAUNCH_STATUS_JUNIT, "", null);
        IStatusHandler handler = DebugPlugin.getDefault()
            .getStatusHandler(junitStatus);
        if (handler != null)
            handler.handleStatus(junitStatus, null);

        return result;
    }

    @Override
    protected IStatus getLauncherStatus() {
        return createStatus("Problem(s) preparing the runtime environment.", bndTester.getProjectLauncher()
            .getErrors(),
            bndTester.getProjectLauncher()
                .getWarnings());
    }

    // A couple of hacks to make sure the JUnit plugin is active and notices our
    // launch.
    @Override
    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
        // start the JUnit plugin
        try {
            Bundle jdtJUnitBundle = BundleUtils.findBundle(Plugin.getDefault()
                .getBundleContext(), JDT_JUNIT_BSN, null);
            if (jdtJUnitBundle == null)
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bundle \"{0}\" was not found. Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
            jdtJUnitBundle.start(Bundle.START_TRANSIENT);
        } catch (BundleException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error starting bundle \"{0}\". Cannot report JUnit results via the Workbench.", JDT_JUNIT_BSN), null));
        }

        // JUnit plugin ignores the launch unless attribute
        // "org.eclipse.jdt.launching.PROJECT_ATTR" is set.
        ILaunchConfigurationWorkingCopy modifiedConfig = configuration.getWorkingCopy();

        IResource launchResource = LaunchUtils.getTargetResource(configuration);
        if (launchResource != null) {
            String launchProjectName = LaunchUtils.getLaunchProjectName(launchResource);

            IProject launchProject = ResourcesPlugin.getWorkspace()
                .getRoot()
                .getProject(launchProjectName);

            if (!launchProject.exists()) {
                launchProjectName = launchResource.getProject()
                    .getName();
            }

            modifiedConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, launchProjectName);
        }

        return super.getLaunch(modifiedConfig.doSave(), mode);
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert(monitor, 2);

        try {
            launch.setAttribute(ATTR_JUNIT_PORT, Integer.toString(junitPort));
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error obtaining OSGi project tester.", e));
        }

        super.launch(configuration, mode, launch, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
    }

    private int configureTester(ILaunchConfiguration configuration) throws CoreException, IOException {
        assertBndEclipseTester();

        // Find free socket for JUnit protocol
        int port = SocketUtil.findFreePort();
        bndEclipseTester.setPort(port);

        // Enable tracing?
        enableTraceOptionIfSetOnConfiguration(configuration, bndTester.getProjectLauncher());

        // Keep alive?
        bndTester.setContinuous(enableKeepAlive(configuration));

        //
        // The JUnit runner can set a file with names of failed tests
        // that are requested to rerun
        //

        String failuresFileName = configuration.getAttribute("org.eclipse.jdt.junit.FAILURENAMES", (String) null);
        if (failuresFileName != null) {
            File failuresFile = new File(failuresFileName);
            if (failuresFile.isFile()) {
                String failures = IO.collect(failuresFile);
                Matcher m = FAILURES_P.matcher(failures);
                while (m.find()) {
                    bndTester.addTest(m.group(2) + ":" + m.group(1));
                }
            }
        }

        //
        // Check if we were asked to re-run a specific test class/method
        //

        String testClass = configuration.getAttribute("org.eclipse.jdt.launching.MAIN_TYPE", (String) null);
        String testMethod = configuration.getAttribute("org.eclipse.jdt.junit.TESTNAME", (String) null);

        if (testClass != null) {
            String testName = testClass;
            if (testMethod != null)
                testName += ":" + testMethod;
            bndTester.addTest(testName);
        } else {
            // We're not being asked to run a specific class and/or method, so use
            String tests = configuration.getAttribute(ORG_BNDTOOLS_TESTNAMES, (String) null);
            if (tests != null && !tests.trim()
                .isEmpty()) {
                for (String test : tests.trim()
                    .split("\\s+")) {
                    bndTester.addTest(test);
                }
            }
        }

        // if (bndTester.getTests().isEmpty()) {
        // if (!bndTester.getContinuous() || bndTester.getProject().getProperty(Constants.TESTCASES) == null) {
        // throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "No tests are selected. " //
        // + "This starts the tester who will then wait " //
        // + "for bundles with the Test-Cases header listing the test cases. To enable this, set " +
        // Constants.TESTCONTINUOUS + " to true", null));
        // }
        // }

        return port;
    }

    private void assertBndEclipseTester() throws CoreException {
        if (bndEclipseTester == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd/Eclipse tester was not initialised.", null));
    }

    @SuppressWarnings("deprecation")
    private static boolean enableKeepAlive(ILaunchConfiguration configuration) throws CoreException {
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
