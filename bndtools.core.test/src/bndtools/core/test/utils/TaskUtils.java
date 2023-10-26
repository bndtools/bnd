package bndtools.core.test.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.deployer.repository.LocalIndexedRepo;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.SupplierWithException;
import aQute.bnd.osgi.Constants;
import aQute.lib.io.IO;
import bndtools.central.Central;

public class TaskUtils {

	static IResourceVisitor VISITOR = resource -> {
		IPath path = resource.getFullPath();
		log(path == null ? "null" : path.toString());
		return true;
	};

	private TaskUtils() {}

	public static void log(SupplierWithException<String> msg) {
		try {
			// System.err.println(System.currentTimeMillis() + ": " +
			// msg.get());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void log(String msg) {
		// log(() -> msg);
	}

	public static void dumpWorkspace() {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		try {
			ws.getRoot()
				.accept(VISITOR);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static boolean setAutobuild(boolean on) {
		try {
			IWorkspace eclipse = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description = eclipse.getDescription();
			boolean original = description.isAutoBuilding();
			description.setAutoBuilding(on);
			eclipse.setDescription(description);
			return original;
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static ICompilationUnit createEmptyTestClass(Project bndProject, String pack, String className) {
		return createTestCompilationUnit(bndProject, pack, className,
			"package " + pack + "; public class " + className + " {}");
	}

	public static ICompilationUnit createTestCompilationUnit(Project bndProject, String pack, String className,
		String source) {
		return createTestCompilationUnit(Central.getProject(bndProject)
			.get(), pack, className, source);
	}

	public static ICompilationUnit createTestCompilationUnit(IProject eclipseProject, String pack, String className,
		String source) {
		return createCompilationUnit(eclipseProject, "test", pack, className, source);
	}

	public static ICompilationUnit createEmptyClass(Project bndProject, String pack, String className) {
		return createCompilationUnit(bndProject, pack, className,
			"package " + pack + "; public class " + className + " {}");
	}

	public static ICompilationUnit createCompilationUnit(Project bndProject, String pack, String className,
		String source) {
		return createCompilationUnit(Central.getProject(bndProject)
			.get(), pack, className, source);
	}

	public static ICompilationUnit createCompilationUnit(IProject eclipseProject, String pack, String className,
		String source) {
		return createCompilationUnit(eclipseProject, "src", pack, className, source);
	}

	public static ICompilationUnit createCompilationUnit(IProject eclipseProject, String srcDir, String pack,
		String className, String source) {
		try {
			IFolder sourceFolder = eclipseProject.getFolder(srcDir);
			if (!sourceFolder.exists()) {
				sourceFolder.create(true, true,
					new LoggingProgressMonitor("create()ing source folder " + srcDir + " for " + eclipseProject));
			}

			IJavaProject javaProject = JavaCore.create(eclipseProject);
			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);
			IPackageFragment packFragment = root.createPackageFragment(pack, false,
				new LoggingProgressMonitor("createPackageFragment() \"" + pack + "\""));
			return createCompilationUnit(packFragment, className, source);
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static ICompilationUnit createCompilationUnit(IPackageFragment pack, String className, String source) {
		try {
			return pack.createCompilationUnit(className + ".java", source, true, null);
		} catch (JavaModelException e) {
			throw Exceptions.duck(e);
		}
	}

	public static void assertNoBndMarkers() {
		try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
			IMarker[] markers = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
			softly.assertThat(markers)
				.as("bnd markers")
				.isEmpty();
			markers = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMarkers(BndtoolsConstants.MARKER_BND_PATH_PROBLEM, true, IResource.DEPTH_INFINITE);
			softly.assertThat(markers)
				.as("bnd path markers")
				.isEmpty();
			markers = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMarkers(BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM, true, IResource.DEPTH_INFINITE);
			softly.assertThat(markers)
				.as("bnd workspace markers")
				.isEmpty();
			markers = ResourcesPlugin.getWorkspace()
				.getRoot()
				.findMarkers(BndtoolsConstants.MARKER_BND_BLOCKER, true, IResource.DEPTH_INFINITE);
			softly.assertThat(markers)
				.as("bnd blocker markers")
				.isEmpty();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static CompilationUnit buildAST(ICompilationUnit icu) {
		return buildAST(icu, false);
	}

	public static CompilationUnit buildAST(ICompilationUnit icu, boolean recoverBindings) {
		// First create our AST
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		// Map<String, String> options = JavaCore.getOptions();
		// // Need to set 1.5 or higher for the "import static" syntax to work.
		// // Need to set 1.8 or higher to test parameterized type usages.
		// JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		// parser.setCompilerOptions(options);
		parser.setSource(icu);
		// If you don't attempt to resolve bindings you don't get any errors
		// about missing types.
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(recoverBindings);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setUnitName(icu.getPath()
			.lastSegment());
		// parser.setProject(icu.getJavaProject());
		return (CompilationUnit) parser.createAST(null);
	}

	// Synchronously build the workspace
	public static void buildFull(String context) {
		try {
			ResourcesPlugin.getWorkspace()
				.build(IncrementalProjectBuilder.FULL_BUILD, new LoggingProgressMonitor(context));
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	// Synchronously build the workspace
	public static void buildClean(String context) {
		try {
			ResourcesPlugin.getWorkspace()
				.build(IncrementalProjectBuilder.CLEAN_BUILD, new LoggingProgressMonitor(context));
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static void buildIncremental(String context) {
		try {
			ResourcesPlugin.getWorkspace()
				.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new LoggingProgressMonitor(context));
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	public static void updateWorkspace(String context) throws Exception {
		log(context + ": Updating Bnd workspace");
		Workspace bndWs = Central.getWorkspace();
		bndWs.clear();
		bndWs.forceRefresh();
		bndWs.getPlugins();
	}

	public static void requestClasspathUpdate(String context) {
		try {
			log(context + ": Initiating classpath update");
			ClasspathContainerInitializer initializer = JavaCore
				.getClasspathContainerInitializer(BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
			if (initializer != null) {
				IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace()
					.getRoot());

				for (IJavaProject project : javaModel.getJavaProjects()) {
					log(() -> context + ": Updating classpath for " + project.getElementName());
					initializer.requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, project, null);
				}
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		log(context + ": done classpath update");
	}

	public static void requestClasspathUpdate(String context, String name) {
		IProject project = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(name);
		requestClasspathUpdate(context, JavaCore.create(project));
	}

	public static void requestClasspathUpdate(String context, IProject project) {
		requestClasspathUpdate(context, JavaCore.create(project));
	}

	public static void requestClasspathUpdate(String context, IJavaProject project) {
		try {
			log(context + ": Initiating classpath update");
			ClasspathContainerInitializer initializer = JavaCore
				.getClasspathContainerInitializer(BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
			if (initializer != null) {
				log(() -> context + ": Updating classpath for " + project.getElementName());
				initializer.requestClasspathContainerUpdate(BndtoolsConstants.BND_CLASSPATH_ID, project, null);
			}
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
		log(context + ": done classpath update");
	}

	public static void waitForBuild(String context) {
		log(context + ": Initiating build");
		buildIncremental(context);
		log(context + ": done waiting for build to complete");
	}

	public static void doAutobuildAndWait(String context) {
		log(context + ": turning on autobuild");
		setAutobuild(true);
		waitForAutobuild(context);
		log(context + ": turning off autobuild");
		setAutobuild(false);
	}

	/**
	 * Waits for autobuild to finish. Usually, you'll want to use
	 * {@link #waitForAutobuild(ICoreRunnable, String)} to avoid timing issues,
	 * otherwise this method can return immediately in the small window before
	 * autobuild starts.
	 * <p>
	 * Note: the autobuild job takes a second or so to respond to changes in the
	 * workspace before triggering the incremental build. This really slows down
	 * tests. Usually, it is best to call {@link #buildIncremental(String)}
	 * directly as it will not have this lag and usually finishes quickly.
	 *
	 * @param context
	 */
	public static void waitForAutobuild(String context) {
		final IJobManager jm = Job.getJobManager();
		try {
			// Wait for manual build to finish if running
			jm.join(ResourcesPlugin.FAMILY_MANUAL_BUILD,
				new LoggingProgressMonitor(context + ": Waiting for manual build"));
			// Wait for auto build to finish if running
			jm.join(ResourcesPlugin.FAMILY_AUTO_BUILD, new LoggingProgressMonitor(context + ": Waiting for autobuild"));
		} catch (InterruptedException e) {
			throw Exceptions.duck(e);
		}
	}

	public static boolean isAutobuild(Job job) {
		return job.belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD);
	}

	public static void waitForAutobuild(ICoreRunnable modTask, String context) {

		final IJobManager jm = Job.getJobManager();
		final CountDownLatch startFlag = new CountDownLatch(1);
		final CountDownLatch endFlag = new CountDownLatch(1);
		IJobChangeListener l = new JobChangeAdapter() {
			@Override
			public void running(IJobChangeEvent event) {
				if (isAutobuild(event.getJob())) {
					log(context + ": autobuild starting: " + event.getJob());
					startFlag.countDown();
					jm.removeJobChangeListener(this);
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace()
				.run(monitor -> {
					log(context + ": adding autobuild listener");
					jm.addJobChangeListener(l);
					log(context + ": autobuild listener added, running task");
					modTask.run(monitor);
				}, new LoggingProgressMonitor(context));
			if (startFlag.await(10000, TimeUnit.MILLISECONDS) == false) {
				throw new IllegalStateException("Autobuild didn't start within 10s: " + context);
			}
			waitForAutobuild(context);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		} finally {
			jm.removeJobChangeListener(l);
		}
		log(context + ": finished");
	}

	public static void addBundlesToBuildpath(Project bndProject, String... bundleNames) {
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();

			for (String bundleName : bundleNames) {
				model.addPath(new VersionedClause(bundleName, null), Constants.BUILDPATH);
			}
			model.saveChanges();
			Central.refresh(bndProject);
			bndProject.refresh();
			requestClasspathUpdate("addBundleToBuildpath()");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void addBundlesToTestpath(Project bndProject, String... bundleNames) {
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();

			for (String bundleName : bundleNames) {
				model.addPath(new VersionedClause(bundleName, null), Constants.TESTPATH);
			}
			model.saveChanges();
			Central.refresh(bndProject);
			bndProject.refresh();
			requestClasspathUpdate("addBundleToBuildpath()");
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static boolean clearBuildpath(Project bndProject) {
		log("clearing buildpath");
		try {
			BndEditModel model = new BndEditModel(bndProject);
			model.load();
			List<VersionedClause> buildPath = model.getBuildPath();
			if (buildPath != null && !buildPath.isEmpty()) {
				model.setBuildPath(Collections.emptyList());
				model.saveChanges();
				Central.refresh(bndProject);
				TaskUtils.requestClasspathUpdate("clearBuildpath()");
				return true;
			} else {
				log("buildpath was not set; not trying to clear it");
				return false;
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static void importFodder() throws Exception, IOException {
		final LocalIndexedRepo localRepo = (LocalIndexedRepo) Central.getWorkspace()
			.getRepository("Local Index");

		if (localRepo == null) {
			log("Central.getWorkspace(): " + Central.getWorkspace()
				.getBase());
			dumpWorkspace();
			throw new IllegalStateException("Could not find Local Index");
		}

		Path bundleRoot = Paths.get(System.getProperty("bndtools.core.test.dir"))
			.resolve("./generated/");
		log("Attempting to import fodder bundles from " + bundleRoot);
		Files.walk(bundleRoot, 1)
			.filter(x -> x.getFileName()
				.toString()
				.contains(".fodder."))
			.forEach(bundle -> {
				try {
					log("Adding fodder bundle to localRepo: " + bundle);
					localRepo.put(IO.stream(bundle), null);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
		log(() -> ("Local Index contains:\n\t" + localRepo.list("*")
			.stream()
			.collect(Collectors.joining(",\n\t"))));
		updateWorkspace("beforeAllBase()");
	}

	public static void createTestDirectories(String project) {
		try {
			File baseDir = new File(Central.getWorkspace()
				.getBase(), project);
			File dir = new File(baseDir, "test");
			log("Creating testsrc directory for " + project + ": " + dir);
			if (!dir.exists() && !dir.isDirectory() && !dir.mkdirs()) {
				throw new IllegalStateException("Couldn't create testsrc for " + project + ": " + dir);
			}
			// Make sure the directory is not empty or else the
			// WorkspaceSynchronizer will not create it.
			dir = new File(dir, ".dummy");
			log("Creating new file: " + dir);
			dir.createNewFile();

			dir = new File(baseDir, "bin_test");
			log("Creating testbin directory for " + project + ": " + dir);
			if (!dir.exists() && !dir.isDirectory() && !dir.mkdirs()) {
				throw new IllegalStateException("Couldn't create testbin for " + project + ": " + dir);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}
}
