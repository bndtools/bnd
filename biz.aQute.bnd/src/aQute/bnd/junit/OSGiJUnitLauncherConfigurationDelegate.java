package aQute.bnd.junit;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.junit.launcher.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.bnd.service.*;
import aQute.lib.osgi.*;

@SuppressWarnings("unchecked") public class OSGiJUnitLauncherConfigurationDelegate extends
		JUnitLaunchConfigurationDelegate {

	private ProjectTester	tester;

	private ProjectTester getTester(ILaunchConfiguration configuration) throws CoreException {
		try {
			if (tester == null) {

				IJavaProject javaProject = getJavaProject(configuration);
				Project project = Activator.getDefault().getCentral().getModel(javaProject);
				project.clear();

				tester = project.getProjectTester();
				if (tester == null) {

					throw new IllegalArgumentException("Launching " + project
							+ ". Cannot determine launcher configuration from -runpath: "
							+ project.getProperty(Constants.RUNPATH));
				}

			}
			return tester;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to obtain launcher", e));
		}
	}

	public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return getTester(configuration).getProjectLauncher().getMainTypeName();
	}

	// protected IMember[] evaluateTests(ILaunchConfiguration configuration,
	// IProgressMonitor monitor) throws CoreException {
	// System.out.println("Evaluate Tests");
	// return super.evaluateTests(configuration, monitor);
	// }

	protected void collectExecutionArguments(ILaunchConfiguration configuration,
			List/* String */vmArguments, List/* String */programArguments) throws CoreException {

		try {
			super.collectExecutionArguments(configuration, vmArguments, programArguments);

			ProjectTester tester = getTester(configuration);
			ProjectLauncher launcher = tester.getProjectLauncher();

			int port = -1;
			// Pretty ugly :-(
			for (int i = 0; i < programArguments.size(); i++) {
				if (programArguments.get(i).equals("-port")) {
					port = Integer.parseInt((String) programArguments.get(++i));
				} else if (programArguments.get(i).equals("-testNameFile")) {
					File testNameFile = tester.getProject().getFile(
							(String) programArguments.get(++i));
					processFile(tester, testNameFile);
				} else if (programArguments.get(i).equals("-test")) {
					tester.addTest((String)programArguments.get(++i));
				}else if (programArguments.get(i).equals("-classNames")) {
					tester.addTest((String)programArguments.get(++i));
				}
			}

			if (tester instanceof EclipseJUnitTester) {
				EclipseJUnitTester etester = (EclipseJUnitTester) tester;
				etester.setPort(port);
			}

			programArguments.addAll(launcher.getArguments());
			vmArguments.addAll(launcher.getRunVM());

			if (configuration.getAttribute(OSGiArgumentsTab.ATTR_KEEP, false))
				programArguments.add("-keep");

			if (tester.getProject().isOk()) {
				tester.prepare();
				return;
			}

			String args = vmArguments + " " + programArguments + " "
					+ Arrays.toString(getClasspath(configuration));
			Activator.getDefault().report(true, false, tester.getProject(),
					"Launching " + tester.getProject(), args);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Building arguments for remote VM", e));
		}
		throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
				"Building arguments for remote VM"));
	}

	private void processFile(ProjectTester tester, File file) throws IOException {
		FileReader rdr = new FileReader(file);
		BufferedReader brdr = new BufferedReader(rdr);
		String line = brdr.readLine();
		while (line != null) {
			tester.addTest(line.trim());
			line = brdr.readLine();
		}
		rdr.close();
	}

	/**
	 * Calculate the classpath. We include our own runtime.jar which includes
	 * the test framework and we include the first of the test frameworks
	 * specified.
	 */
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		return getTester(configuration).getProjectLauncher().getClasspath().toArray(new String[0]);
	}

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			super.launch(configuration, mode, launch, monitor);
		} finally {
			tester = null;
		}
	}
}
