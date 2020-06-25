package bndtools.core.test.utils;

import static aQute.lib.exceptions.RunnableWithException.asRunnable;
import static bndtools.core.test.utils.TaskUtils.countDownMonitor;
import static bndtools.core.test.utils.TaskUtils.log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Jupiter extension for setting up a test workspace. This extension does the
 * following:
 * 
 * <ol>
 * <li>Waits for the workbench initialisation to complete</li>
 * <li>Ensures that bndtools.core has been started properly</li>
 * <li>Clears the current workspace</li>
 * <li>Imports the template workspace (if specified by {@codee @Workbench}) to
 * the current workspace</li>
 * </ol>
 * 
 * @see WorkbenchTest
 */
public class WorkbenchExtension implements BeforeAllCallback {
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		BundleContext bc = FrameworkUtil.getBundle(context.getRequiredTestClass()).getBundleContext();

		// The bndtools.core plugin must be initialized on the UI thread or else it will
		// cause an NPE. Because BundleEngine/Jupiter will probably not be running on
		// the main thread, we cannot rely on the automatic OSGi class loading to
		// load the bundle the first time that the class is accessed, as then the bundle
		// will be started by the tester thread and we'll get the NPE. So we check if
		// it's already running, and if not then we manually start it on the UI thread.

		// Make sure bndtools.core is started, if not we start it on the UI thread
		Bundle bndtoolsCore = Stream.of(bc.getBundles())
				.filter(bundle -> bundle.getSymbolicName().equals("bndtools.core")).findAny()
				.orElseThrow(() -> new IllegalStateException("Couldn't find bndtools.core bundle"));

		if ((bndtoolsCore.getState() & Bundle.ACTIVE) == 0) {
			Display.getDefault().syncExec(asRunnable(() -> bndtoolsCore.start()));
		}

		WorkbenchTest test = context.getRequiredTestClass().getAnnotation(WorkbenchTest.class);

		String resourcePath = (test != null && !"".equals(test.value())) ? test.value()
				: classToPath(context.getRequiredTestClass());

		String workspacesRoot = System.getProperty("bndtools.core.test.dir");
		if (workspacesRoot == null) {
			throw new IllegalArgumentException("bndtools.core.test.dir not set");
		}
		Path srcRoot = Paths.get(workspacesRoot).resolve("resources/workspaces");
		Path ourRoot = srcRoot.resolve(resourcePath);
		log("Using template workspace: " + ourRoot);

		// Clean the workspace
		IWorkspaceRoot wsr = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = wsr.getProjects();
		for (IProject project : projects) {
			// I tried to use the IProgressMonitor instance to
			project.delete(true, true, null);
		}

		// Now copy the workspace from our resourcePath into the active workspace.
		IOverwriteQuery overwriteQuery = file -> IOverwriteQuery.ALL;

		List<Path> sourceProjects = Files.walk(ourRoot, 1).filter(x -> !x.equals(ourRoot)).collect(Collectors.toList());
		CountDownLatch importFlag = new CountDownLatch(sourceProjects.size());

		for (Path sourceProject : sourceProjects) {
			String projectName = sourceProject.getFileName().toString();
			IProject project = wsr.getProject(projectName);
			ImportOperation importOperation = new ImportOperation(project.getFullPath(), sourceProject.toFile(),
					FileSystemStructureProvider.INSTANCE, overwriteQuery);
			importOperation.setCreateContainerStructure(false);
			importOperation.run(countDownMonitor(importFlag));
		}

		log("About to wait for imports to complete " + sourceProjects.size());
		boolean finished = importFlag.await(10000, TimeUnit.MILLISECONDS);
		if (!finished) {
			throw new IllegalStateException("Import of workspace " + resourcePath + " did not complete within 10s");
		}
		log("done waiting for import to complete");
	}

	private static String classToPath(Class<?> requiredTestClass) {
		return requiredTestClass.getPackage().getName().replace("bndtools.core.test.", "").replace('.', '/');
	}
}