package bndtools.launch;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunMode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Run;
import bndtools.central.Central;
import bndtools.launch.util.LaunchUtils;

public class BndDependencySourceContainer extends CompositeSourceContainer {
	private static final ILogger	logger	= Logger.getLogger(BndDependencySourceContainer.class);

	public static final String		TYPE_ID	= "org.bndtools.core.launch.sourceContainerTypes.bndDependencies";

	private Run						lastRun	= null;

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BndDependencySourceContainer;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	protected ILaunchConfiguration getLaunchConfiguration() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.getLaunchConfiguration();
		}
		return null;
	}

	@Override
	public String getName() {
		return "Bnd Dependencies";
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		List<ISourceContainer> result = new LinkedList<>();

		ILaunchConfiguration config = getLaunchConfiguration();
		Set<String> projectsAdded = new HashSet<>();
		try {
			if (lastRun != null) {
				LaunchUtils.endRun(lastRun);
			}

			Run run = LaunchUtils.createRun(config, RunMode.SOURCES);
			if (run != null) {
				Collection<Container> runbundles = run.getRunbundles();
				for (Container runbundle : runbundles) {
					if (runbundle.getType() == TYPE.PROJECT) {
						String targetProjName = runbundle.getProject()
							.getName();
						if (projectsAdded.add(targetProjName)) {
							IProject targetProj = ResourcesPlugin.getWorkspace()
								.getRoot()
								.getProject(targetProjName);
							if (targetProj != null) {
								IJavaProject targetJavaProj = JavaCore.create(targetProj);
								result.add(new JavaProjectSourceContainer(targetJavaProj));
							}
						}
					} else if (runbundle.getType() == TYPE.REPO) {
						IPath bundlePath = Central.toPath(runbundle.getFile());
						IFile bundleFile = null;
						if (bundlePath != null) {
							bundleFile = ResourcesPlugin.getWorkspace()
								.getRoot()
								.getFile(bundlePath);
						}
						if (bundleFile != null) {
							ISourceContainer sourceContainer = null;

							// check to see if this archive came from a repo
							// that encodes the source project name
							final String sourceProjectName = runbundle.getAttributes()
								.get("sourceProjectName");

							if (sourceProjectName != null) {
								try {
									IProject sourceProject = ResourcesPlugin.getWorkspace()
										.getRoot()
										.getProject(sourceProjectName);

									if (sourceProject.exists()) {
										IJavaProject javaSourceProject = JavaCore.create(sourceProject);

										sourceContainer = new JavaProjectSourceContainer(javaSourceProject);
									}
								} catch (Exception e) {
									logger.logError("Error getting source java project", e);
								}
							}

							if (sourceContainer == null) {
								// default to archive source container
								sourceContainer = new ArchiveSourceContainer(bundleFile, false);
							}

							result.add(sourceContainer);
						} else {
							ExternalArchiveSourceContainer container = new ExternalArchiveSourceContainer(
								runbundle.getFile()
									.toString(),
								false);
							result.add(container);
						}
					}
				}

				lastRun = run;
			}
		} catch (Exception e) {
			logger.logError("Error querying Bnd dependency source containers.", e);
		}

		return result.toArray(new ISourceContainer[0]);
	}

	@Override
	public void dispose() {
		super.dispose();

		if (lastRun != null) {
			LaunchUtils.endRun(lastRun);
			lastRun = null;
		}
	}
}
