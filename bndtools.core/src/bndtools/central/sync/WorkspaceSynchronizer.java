package bndtools.central.sync;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Constants;
import aQute.lib.collections.Logic;
import aQute.lib.fileset.FileSet;
import bndtools.Plugin;
import bndtools.central.Central;

/**
 * A utility class to synchronize the bnd & Eclipse workspaces as well as build.
 */
public class WorkspaceSynchronizer {
	private static final Path[]		NO_PATHS	= new Path[] {};
	private static IWorkspace		eclipse		= ResourcesPlugin.getWorkspace();
	private static IWorkspaceRoot	wsroot		= eclipse.getRoot();

	/**
	 * Build the whole workspace.
	 *
	 * @param workspace the workspace
	 * @param monitor a monitor
	 */
	boolean build(Workspace workspace, IProgressMonitor monitor) throws CoreException {

		try {
			eclipse.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		} catch (OperationCanceledException e) {
			return true;
		} catch (Exception e) {
			About.logger.error("build failed {}", e, e);
		}
		return monitor.isCanceled();
	}

	/**
	 * Check if all the projects are imported. Make it as fast as possible if
	 * they are.
	 *
	 * @param refresh true if a refresh must be done
	 * @throws CoreException
	 */
	public void synchronize(boolean refresh, IProgressMonitor monitor, Runnable atend) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		boolean previous = setAutobuild(false);
		try {
			Workspace ws = Central.getWorkspace();
			if (ws == null || ws.isDefaultWorkspace())
				return;
			System.out.println("Syncing");

			Map<String, IProject> projects = new TreeMap<>();

			for (IProject project : wsroot.getProjects()) {
				if (project == null)
					continue;

				if (!project.exists())
					continue;

				if (!project.isAccessible())
					continue;

				IPath location = project.getLocation();
				if (location == null)
					continue;

				File projectDir = location.toFile();
				if (!projectDir.isDirectory())
					continue;

				projects.put(projectDir.getName(), project);
			}

			IProject cnf = wsroot.getProject(Workspace.CNFDIR);
			if (!cnf.exists()) {
				subMonitor.setTaskName("Creating cnf");
				// must be done inline
				createProject(ws.getFile(Workspace.CNFDIR), null, subMonitor.split(1));
			}

			List<String> models = ws.writeLocked(() -> {
				ws.refreshProjects();
				ws.forceRefresh();
				return ws.getBuildOrder()
					.stream()
					.filter(Project::isValid)
					.map(Project::getName)
					.collect(Collectors.toList());
			}, monitor::isCanceled);

			projects.remove(Workspace.CNFDIR);
			models.remove(Workspace.CNFDIR);

			System.out.println("Projects    " + projects.keySet());
			System.out.println("Models      " + new TreeSet<>(models));
			System.out.println("To create   " + new TreeSet<>(Logic.remove(models, projects.keySet())));

			for (String mm : models) {
				IProject project = projects.remove(mm);
				if (project != null)
					continue;

				System.out.println("creating " + mm);

				File dir = ws.getFile(mm);
				project = createProject(dir, null, subMonitor);
			}

			projects.values()
				.removeIf(project -> {
					File projectDir = project.getLocation()
						.toFile();

					boolean inWorkspace = ws.getBase()
						.equals(projectDir.getParentFile());
					if (!inWorkspace)
						return true;

					try {
						if (!project.hasNature(Plugin.BNDTOOLS_NATURE))
							return true;
					} catch (CoreException e) {
						return true;
					}

					return !isEmpty(project);

				});

			System.out.println("To delete   " + new TreeSet<>(projects.keySet()));

			for (Map.Entry<String, IProject> toBeDeleted : projects.entrySet()) {
				IProject project = toBeDeleted.getValue();
				System.out.println("deleting " + toBeDeleted);
				project.delete(true, subMonitor);
			}

			if (refresh && monitor != null) {
				try {
					monitor.subTask("Refresh workspace ");
					wsroot.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				} catch (Exception e) {
					// best effort
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			About.logger.error("Failed to sync {}", e, e);
		} finally {
			atend.run();
			setAutobuild(previous);
		}
	}

	private boolean isEmpty(IProject project) {

		IPath location = project.getLocation();
		if (location == null)
			return false;

		File folder = location.toFile();
		if (!folder.isDirectory())
			return true;

		File[] listFiles = folder.listFiles();
		if (listFiles.length == 0)
			return true;

		FileSet fs = new FileSet(folder, "**");
		Set<File> files = fs.getFiles();
		files.removeIf(f -> {
			return f.getName()
				.equals(".project");
		});

		return files.isEmpty();

	}

	private boolean setAutobuild(boolean on) throws CoreException {
		IWorkspaceDescription description = eclipse.getDescription();
		boolean original = description.isAutoBuilding();
		description.setAutoBuilding(on);
		eclipse.setDescription(description);
		return original;
	}

	public static void removeProject(IProject project, IProgressMonitor monitor) {
		try {
			project.delete(false, true, monitor);
		} catch (CoreException e) {
			About.logger.error("Unable to remove project {} : {}", project, e, e);
		}
	}

	public static IProject createProject(File directory, Project model, IProgressMonitor monitor) throws CoreException {
		IProject project = wsroot.getProject(model.getName());
		IProjectDescription description = ResourcesPlugin.getWorkspace()
			.newProjectDescription(project.getName());

		File projectBase = model.getBase();
		IPath location = Path.fromOSString(projectBase.getAbsolutePath());
		description.setLocation(location);

		project.create(description, monitor);
		project.open(monitor);

		IJavaProject javaProject = JavaCore.create(project);
		synchronizeProject(model, monitor);
		return project;
	}

	/**
	 * Ensure that the related IProject has the settings as defined by the model
	 * and workspace. It will set the Project builder, nature and the classpath
	 * entries.
	 *
	 * @param model the model
	 * @param monitor
	 */
	public static IProject synchronizeProject(Project model, IProgressMonitor monitor, boolean preserve)
		throws CoreException {
		try {
			IProject project = wsroot.getProject(model.getName());
			IJavaProject javaProject = JavaCore.create(project);
			if (!javaProject.exists())
				return project;

			String release = model.getProperty(Constants.JAVAC_RELEASE,
				model.getProperty(Constants.JAVAC_SOURCE, "17"));
			javaProject.setOption(JavaCore.COMPILER_RELEASE, release);

			IProjectDescription description = project.getDescription();

			description.setNatureIds(new String[] {
				"org.eclipse.jdt.core.javanature", Plugin.BNDTOOLS_NATURE
			});

			ICommand javaBuilder = description.newCommand();
			javaBuilder.setBuilderName("org.eclipse.jdt.core.javabuilder<");
			ICommand bndBuilder = description.newCommand();
			bndBuilder.setBuilderName("bndtools.core.bndbuilder");
			description.setBuildSpec(new ICommand[] {
				javaBuilder, bndBuilder
			});
			project.setDescription(description, monitor);


			List<IClasspathEntry> cpes = new ArrayList<>();

			for (File f : model.getSourcePath()) {
				IFolder src = project.getFolder(relative(model.getBase(), f));
				IFolder bin = project.getFolder(relative(model.getBase(), model.getSrcOutput()));
				IClasspathEntry sourceEntry = JavaCore.newSourceEntry(src.getFullPath(), NO_PATHS, bin.getFullPath());
				cpes.add(sourceEntry);
			}
			File f = model.getTestSrc();
			IFolder src = project.getFolder(relative(model.getBase(), f));
			IFolder bin = project.getFolder(relative(model.getBase(), model.getTestOutput()));
			IClasspathAttribute testAttr = JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true");

			IClasspathEntry testEntry = JavaCore.newSourceEntry(src.getFullPath(), NO_PATHS, NO_PATHS,
				bin.getFullPath(), new IClasspathAttribute[] {
					testAttr
				});
			assert testEntry.isTest();

			return project;
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static String relative(File base, File f) {
		java.nio.file.Path relative = base.toPath()
			.relativize(f.toPath());
		assert !relative.isAbsolute();
		return relative.toString();
	}

	private static IFolder toIFolder(IProject project, File base, File srcOutput) {
		return null;
	}

	private static <T> T[] concat(T[] array, T newMember) {
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) Array.newInstance(array.getClass()
			.getComponentType(), array.length + 1);
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = newMember;
		return newArray;
	}

}
