package bndtools.wizards.project;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;

public class ImportBndWorkspaceWizard extends Wizard implements IImportWizard {

	private IWorkbench						workbench;

	private ImportBndWorkspaceWizardPageOne	mainPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		setWindowTitle("Import Bnd Workspace");
		setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(Plugin.getDefault()
			.getBundle()
			.getEntry("icons/bndtools-wizban.png")));

	}

	@Override
	public void addPages() {
		super.addPages();
		mainPage = new ImportBndWorkspaceWizardPageOne("Select");
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {

		final ImportSettings importSettings = new ImportSettings(mainPage.getSelectedFolder(),
			mainPage.isDeleteSettings(), mainPage.isInferExecutionEnvironment());
		// create the new project operation
		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					importProjects(importSettings, monitor);
				} catch (Exception e) {
					throw new CoreException(
						new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Error during import of Bnd workspace!", e));
				}
			}
		};

		Job importJob = new Job("Import Bnd Workspace") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.run(monitor);
				} catch (InvocationTargetException e) {
					Throwable t = e.getCause();
					if (t instanceof CoreException && ((CoreException) t).getStatus()
						.getException() != null) {
						// unwrap the cause of the CoreException
						t = ((CoreException) t).getStatus()
							.getException();
					}
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Could not finish import job for Bnd Workspace!",
						t);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};

		importJob.schedule();

		try {
			// Prompt to switch to the BndTools perspective
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			IPerspectiveDescriptor currentPerspective = window.getActivePage()
				.getPerspective();
			if (!"bndtools.perspective".equals(currentPerspective.getId())) {
				if (MessageDialog.openQuestion(getShell(), "Bndtools Perspective",
					"Switch to the Bndtools perspective?")) {
					this.workbench.showPerspective("bndtools.perspective", window);
				}
			}
		} catch (WorkbenchException e) {
			error("Unable to switch to BndTools perspective", e);
		}
		return true;
	}

	private void deleteOldProjectFiles(final Path projectPath) throws IOException {
		final Path settings = projectPath.resolve(".settings");
		if (Files.exists(settings)) {
			Files.walkFileTree(settings, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}

		final Path project = projectPath.resolve(".project");
		Files.deleteIfExists(project);
		final Path classpath = projectPath.resolve(".classpath");
		Files.deleteIfExists(classpath);
	}

	private boolean importProjects(final ImportSettings importSettings, IProgressMonitor monitor) throws Exception {
		Workspace bndWorkspace = Workspace.getWorkspace(importSettings.rootImportPath);

		int steps = bndWorkspace.getAllProjects()
			.size() + 2;

		monitor.beginTask("Importing Bnd workspace", steps);

		importConfigurationProject(importSettings, monitor);

		// Import Projects
		for (Project bndProject : bndWorkspace.getAllProjects()) {
			importBndProject(bndProject, bndWorkspace, importSettings, monitor);
		}

		// build
		monitor.subTask("Building workspace");
		ResourcesPlugin.getWorkspace()
			.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		monitor.worked(1);
		monitor.done();

		return true;
	}

	private void importConfigurationProject(final ImportSettings importSettings, IProgressMonitor monitor)
		throws Exception {
		monitor.subTask("Import configuration project 'cnf'.");
		final IWorkspace eclipseWorkspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot eclipseWorkspaceRoot = eclipseWorkspace.getRoot();
		final Workspace bndWorkspace = Workspace.getWorkspace(importSettings.rootImportPath);

		// Prepare Eclipse Workspace
		updateEclipseWorkspaceSettings(bndWorkspace);

		// create generic project
		final IProjectDescription cnfProjectDescription = eclipseWorkspace.newProjectDescription(Workspace.CNFDIR);
		final IProject project = eclipseWorkspaceRoot.getProject(Workspace.CNFDIR);

		if (importSettings.deleteSettings) {
			deleteOldProjectFiles(Paths.get(bndWorkspace.getBase()
				.toURI())
				.resolve(Workspace.CNFDIR));
		}

		// create JavaProject
		IPath path = URIUtil.toPath(bndWorkspace.getBuildDir()
			.toURI());
		if (Platform.getLocation()
			.isPrefixOf(path)) {
			cnfProjectDescription.setLocation(null);
		} else {
			cnfProjectDescription.setLocation(path);
		}

		if (!project.exists()) {
			project.create(cnfProjectDescription, monitor);
		}
		if (!project.isOpen()) {
			project.open(monitor);
		}

		monitor.worked(1);
	}

	private void importBndProject(final Project bndProject, final Workspace bndWorkspace,
		final ImportSettings importSettings, IProgressMonitor monitor) throws IOException, CoreException, Exception {
		monitor.subTask("Import Bnd project '" + bndProject.getName() + "'.");
		final IWorkspace eclipseWorkspace = ResourcesPlugin.getWorkspace();
		final IWorkspaceRoot eclipseWorkspaceRoot = eclipseWorkspace.getRoot();

		if (importSettings.deleteSettings) {
			deleteOldProjectFiles(Paths.get(bndWorkspace.getBaseURI())
				.resolve(bndProject.getName()));
		}
		// create generic project
		final IProjectDescription projectDescription = eclipseWorkspace.newProjectDescription(bndProject.getName());
		final IProject project = eclipseWorkspaceRoot.getProject(bndProject.getName());

		IPath path = URIUtil.toPath(bndProject.getBaseURI());
		if (Platform.getLocation()
			.isPrefixOf(path)) {
			projectDescription.setLocation(null);
		} else {
			projectDescription.setLocation(path);
		}

		if (!project.exists()) {
			project.create(projectDescription, monitor);
		}
		project.open(monitor);

		setNatures(project, monitor, JavaCore.NATURE_ID, Plugin.BNDTOOLS_NATURE);

		IJavaProject javaProject = JavaCore.create(project);
		if (!javaProject.isOpen()) {
			javaProject.open(monitor);
		}
		updateJavaProjectSettings(bndProject, javaProject, importSettings, monitor);

		importSourceAndOutputFolders(bndProject, project, javaProject, monitor);
	}

	private static final IClasspathAttribute TEST = JavaCore.newClasspathAttribute("test", Boolean.TRUE.toString());

	private void importSourceAndOutputFolders(Project bndProject, IProject workspaceProject, IJavaProject javaProject,
		IProgressMonitor monitor) throws Exception {
		// remove defaults
		removeClasspathDefaults(javaProject);

		// Output
		IFolder sourceOutput = workspaceProject.getFolder(URIUtil.toPath(bndProject.getSrcOutput()
			.toURI())
			.makeRelativeTo(workspaceProject.getLocation()));
		IPackageFragmentRoot outputRoot = javaProject.getPackageFragmentRoot(sourceOutput);

		// Source (multiple possible)
		for (File folder : bndProject.getSourcePath()) {
			IFolder source = workspaceProject.getFolder(URIUtil.toPath(folder.toURI())
				.makeRelativeTo(workspaceProject.getLocation()));
			// Now the created source folder should be added to the class
			// entries of the project, otherwise compilation
			// will fail
			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(source);
			List<IClasspathEntry> entries = new ArrayList<>(Arrays.asList(javaProject.getRawClasspath()));
			entries.add(JavaCore.newSourceEntry(root.getPath(), ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE,
				outputRoot.getPath(), ClasspathEntry.NO_EXTRA_ATTRIBUTES));
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[0]), monitor);
			createFolderIfNecessary(source, monitor);
		}
		// Test-Source
		javaProject.setOutputLocation(sourceOutput.getFullPath(), null);
		if (!bndProject.getSrcOutput()
			.equals(bndProject.getTestOutput())) {
			IFolder testOutput = workspaceProject.getFolder(URIUtil.toPath(bndProject.getTestOutput()
				.toURI())
				.makeRelativeTo(workspaceProject.getLocation()));
			IPackageFragmentRoot testOutputRoot = javaProject.getPackageFragmentRoot(testOutput);
			IFolder testSource = workspaceProject.getFolder(URIUtil.toPath(bndProject.getTestSrc()
				.toURI())
				.makeRelativeTo(workspaceProject.getLocation()));
			// Now the created source folder should be added to the class
			// entries of the project, otherwise compilation
			// will fail
			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(testSource);
			List<IClasspathEntry> entries = new ArrayList<>(Arrays.asList(javaProject.getRawClasspath()));
			entries.add(JavaCore.newSourceEntry(root.getPath(), ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE,
				testOutputRoot.getPath(), new IClasspathAttribute[] {
					TEST
				}));
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[0]), monitor);
			createFolderIfNecessary(testSource, monitor);
		}

		// Generated Artifact
		IFolder generated = workspaceProject.getFolder(URIUtil.toPath(bndProject.getTarget()
			.toURI())
			.makeRelativeTo(workspaceProject.getLocation()));
		createFolderIfNecessary(generated, monitor);
	}

	private void createFolderIfNecessary(IFolder folder, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			folder.create(true, true, monitor);
		}
	}

	/**
	 * The Java-Nature doesn't add a JRE-Container, so we add one
	 *
	 * @param javaProject the Java project which should get enhanced with
	 *            LibraryContainer
	 * @param javacTarget
	 * @param monitor current IProgressMonitor
	 * @throws JavaModelException
	 */
	private void addSystemLibraryContainer(final IJavaProject javaProject, final String javacTarget,
		final ImportSettings importSettings, IProgressMonitor monitor) throws JavaModelException {
		List<IClasspathEntry> entries = new ArrayList<>(Arrays.asList(javaProject.getRawClasspath()));
		// entries.addAll(newContainerEntries);
		// only add JRE-Container if none available
		boolean jreContainerAvailable = false;
		Iterator<IClasspathEntry> it = entries.iterator();
		while (it.hasNext()) {
			IClasspathEntry entry = it.next();
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && entry.getPath() != null && entry.getPath()
				.toString()
				.startsWith(JavaRuntime.JRE_CONTAINER)) {
				// remove existing entry if user want to infer EE
				if (importSettings.inferExecutionEnvironment) {
					it.remove();
				} else {
					jreContainerAvailable = true;
				}
				break;
			}
		}
		if (!jreContainerAvailable) {
			IClasspathEntry defaultJREContainerEntry = JavaRuntime.getDefaultJREContainerEntry();
			if (importSettings.inferExecutionEnvironment) {
				// fuzzy at the moment but better than nothing. We should find a
				// way to handle CDC
				IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager()
					.getEnvironment("J2SE-" + javacTarget);
				if (environment == null) {
					environment = JavaRuntime.getExecutionEnvironmentsManager()
						.getEnvironment("JavaSE-" + javacTarget);
				}
				if (environment != null) {
					entries.add(JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(environment)));
				} else {
					Plugin.getDefault()
						.getLog()
						.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
							String.format("Could not infer execution-environment in project '%s' for javac.target '%s'",
								javaProject.getElementName(), javacTarget),
							null));
					entries.add(defaultJREContainerEntry);
				}
			} else {
				entries.add(defaultJREContainerEntry);
			}
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[0]), monitor);
		}
	}

	/**
	 * Update Eclipse workspace with information from a Bnd workspace Currently
	 * only compiler-settings are matched
	 *
	 * @param bndWorkspace the imported Bnd workspace
	 */
	private void updateEclipseWorkspaceSettings(final Workspace bndWorkspace) {
		final String javacSource = bndWorkspace.getProperties()
			.getProperty(Constants.JAVAC_SOURCE);
		final String javacTarget = bndWorkspace.getProperties()
			.getProperty(Constants.JAVAC_TARGET);

		Hashtable<String, String> javaCoreOptions = JavaCore.getOptions();
		if (javacSource != null) {
			javaCoreOptions.put(JavaCore.COMPILER_SOURCE, javacSource);
		}
		if (javacTarget != null) {
			javaCoreOptions.put(JavaCore.COMPILER_COMPLIANCE, javacTarget);
			javaCoreOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, javacTarget);
		}
		JavaCore.setOptions(javaCoreOptions);
	}

	/**
	 * Updates the JavaProject with project-level settings from a Bnd project.
	 * Currently only compiler-settings are matched if they differ from the
	 * Eclipse workspace (which has been set prior), as well as the
	 * JRE-SystemLibrary.
	 *
	 * @param bndProject the imported BndProject
	 * @param javaProject the newly created JavaProject
	 * @throws JavaModelException
	 */
	private void updateJavaProjectSettings(final Project bndProject, final IJavaProject javaProject,
		final ImportSettings importSettings, IProgressMonitor monitor) throws JavaModelException {
		final String javacSource = bndProject.getProperties()
			.getProperty(Constants.JAVAC_SOURCE);
		final String javacTarget = bndProject.getProperties()
			.getProperty(Constants.JAVAC_TARGET);

		addSystemLibraryContainer(javaProject, javacTarget, importSettings, monitor);

		Map<String, String> projectOptions = javaProject.getOptions(false);
		// only update project-specific settings when different from workspace
		if (javacSource != null && !javacSource.equals(JavaCore.getOption(JavaCore.COMPILER_SOURCE))) {
			projectOptions.put(JavaCore.COMPILER_SOURCE, javacSource);
		}
		if (javacTarget != null && !javacTarget.equals(JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE))) {
			projectOptions.put(JavaCore.COMPILER_COMPLIANCE, javacTarget);
		}
		if (javacTarget != null && !javacTarget.equals(JavaCore.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM))) {
			projectOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, javacTarget);
		}
		javaProject.setOptions(projectOptions);
	}

	/**
	 * Creating a JavaProject has the effect that there is an default
	 * source-folder in the RawClasspath which uses the project-directory
	 * itself. Furthermore, the BndProjectNature causes the classpath container
	 * to be available if the Repositories-View is still populated from a prior
	 * Workspace-Setup.
	 *
	 * @param javaProject the Project which needs special treatment
	 * @throws JavaModelException
	 */
	private void removeClasspathDefaults(IJavaProject javaProject) throws JavaModelException {
		for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
			if (!root.isArchive()) {
				int jCoreFlags = IPackageFragmentRoot.NO_RESOURCE_MODIFICATION
					| IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH;
				root.delete(IResource.NONE, jCoreFlags, null);
			}
		}
	}

	private void setNatures(IProject project, IProgressMonitor monitor, String... natureIds) throws CoreException {
		IProjectDescription updatingDescription = project.getDescription();
		updatingDescription.setNatureIds(natureIds);
		project.setDescription(updatingDescription, monitor);
	}

	private void error(final String message, final Throwable t) {
		// Log error
		Plugin.getDefault()
			.getLog()
			.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, t));
		// build the error message and include the current stack trace
		final MultiStatus status = createMultiStatus(t);
		Runnable run = () -> ErrorDialog.openError(null, "Error", message, status);
		if (Display.getCurrent() == null) {
			Display.getDefault()
				.asyncExec(run);
		} else {
			run.run();
		}
	}

	/*
	 * TODO probably something to move to Plugin creates a MultiStatus including
	 * StackTrace
	 */
	private static MultiStatus createMultiStatus(Throwable t) {
		List<Status> childStatuses = new ArrayList<>();
		StackTraceElement[] stackTraces = Thread.currentThread()
			.getStackTrace();

		for (StackTraceElement stackTrace : stackTraces) {
			Status status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, stackTrace.toString());
			childStatuses.add(status);
		}

		MultiStatus ms = new MultiStatus(Plugin.PLUGIN_ID, IStatus.ERROR, childStatuses.toArray(new Status[] {}),
			t.toString(), t);
		return ms;
	}

	/**
	 * Wrapper class to hand user-settings for the import down the call-stack
	 */
	private static final class ImportSettings {
		final File		rootImportPath;
		final boolean	deleteSettings;
		final boolean	inferExecutionEnvironment;

		private ImportSettings(File rootImportPath, boolean deleteSettings, boolean inferExecutionEnvironment) {
			this.rootImportPath = rootImportPath;
			this.deleteSettings = deleteSettings;
			this.inferExecutionEnvironment = inferExecutionEnvironment;
		}
	}

}
