package name.neilbartlett.eclipse.bndtools.classpath;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import aQute.lib.osgi.Constants;
import aQute.libg.header.OSGiHeader;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class WorkspaceRepositoryClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	// STATIC SECTION
	
	public static final String CONTAINER_ID = Plugin.PLUGIN_ID + ".WORKSPACE_REPOSITORY";
	
	private static final AtomicReference<WorkspaceRepositoryClasspathContainerInitializer> instanceRef
		= new AtomicReference<WorkspaceRepositoryClasspathContainerInitializer>(null);
	
	public static final WorkspaceRepositoryClasspathContainerInitializer getInstance() {
		WorkspaceRepositoryClasspathContainerInitializer instance;

		instance = instanceRef.get();
		if(instance == null) {
			instanceRef.compareAndSet(null, new WorkspaceRepositoryClasspathContainerInitializer());
			instance = instanceRef.get();
		}
		return instanceRef.get();
	}
	
	// INSTANCE SECTION
	
	/** The containers that have been previously configured against the projects. <b>Map{project name -> container}</b> **/
	private final Map<String, WorkspaceRepositoryClasspathContainer> projectContainerMap = new HashMap<String, WorkspaceRepositoryClasspathContainer>();
	
	/** The bundles available in the workspace. <b>Map{bsn -> Map {version->sorted path list}}</b> */
	private final Map<String,Map<Version,SortedSet<IPath>>> workspaceBundleMap = new HashMap<String, Map<Version,SortedSet<IPath>>>();
	
	/** The bundles exported by each project. <b>Map{project name -> Map{bundle path -> bundle}}</b> */
	private final Map<String, Map<IPath,ExportedBundle>> exportsMap = new HashMap<String, Map<IPath,ExportedBundle>>();
	
	// Prevent instantiation.
	private WorkspaceRepositoryClasspathContainerInitializer() {
	}
	
	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if(containerPath.segmentCount() >= 1) {
			// Construct the new container
			List<BundleDependency> dependencies = parseBundleDependencies(containerPath);
			Map<BundleDependency, ExportedBundle> bindings = calculateBindings(project, dependencies);
			WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(containerPath, project, dependencies, bindings);
			projectContainerMap.put(project.getProject().getName(), newContainer);
			
			// Rebind the container path for the project
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { newContainer }, null);
		}
	}
	/**
	 * Parse the bundle dependencies of the specified container path.
	 * @param containerPath
	 * @return A list of bundle dependencies
	 */
	public static List<BundleDependency> parseBundleDependencies(IPath containerPath) {
		List<BundleDependency> dependencies = new LinkedList<BundleDependency>();
		String bundleListStr = containerPath.segment(1);
		Map<String, Map<String, String>> parsedDepList = OSGiHeader.parseHeader(bundleListStr);
		for (Entry<String, Map<String,String>> entry : parsedDepList.entrySet()) {
			dependencies.add(parsedEntryToDependency(entry));
		}
		return dependencies;
	}
	private Map<BundleDependency, ExportedBundle> calculateBindings(IJavaProject project, Collection<? extends BundleDependency> dependencies) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	
		ClasspathProblemReporterJob markerUpdateJob = new ClasspathProblemReporterJob(project);
		
		Map<BundleDependency,ExportedBundle> bindings = new HashMap<BundleDependency,ExportedBundle>();
		for (BundleDependency dependency : dependencies) {
			String symbolicName = dependency.getSymbolicName();
			Map<Version, SortedSet<IPath>> versionToPathsMap = workspaceBundleMap.get(symbolicName);
			if(versionToPathsMap != null) {
				// Record all the possible matches and rank them by version
				List<RejectedExportCandidate> rejections = new LinkedList<RejectedExportCandidate>();
				SortedMap<Version, ExportedBundle> matches = new TreeMap<Version, ExportedBundle>();
				for (Entry<Version,SortedSet<IPath>> entry : versionToPathsMap.entrySet()) {
					Version entryVersion = entry.getKey();
					SortedSet<IPath> paths = entry.getValue();
					if(paths != null && !paths.isEmpty() && dependency.getVersionRange().includes(entryVersion)) {
						// Iterate over the paths and pick the first one that does not give us a cycle
						ExportedBundle selectedExport = null;
						for (IPath path : paths) {
							ExportedBundle export = findExport(root, path);
							if(!checkForCycles(project.getProject().getName(), export)) {
								selectedExport = export;
								break;
							} else {
								rejections.add(new RejectedExportCandidate(export, "Possible dependency cycle", true));
							}
						}
						if(selectedExport != null) {
							matches.put(entryVersion, selectedExport);
						}
					}
				}
				// Take the highest-version available export
				if(!matches.isEmpty()) {
					Version highestVersion = matches.lastKey();
					ExportedBundle bestExport = matches.get(highestVersion);
					
					bindings.put(dependency, bestExport);
				} else {
					String message = MessageFormat.format("No available exports matching the version range \"{0}\". {1,choice,0#One candidate|1<{1} candidates} rejected.", dependency.getVersionRange(), rejections.size());
					ResolutionProblem problem = new ResolutionProblem(message, dependency);
					problem.addRejectedCandidates(rejections);
					
					markerUpdateJob.addResolutionProblem(problem);
				}
			} else {
				String message = MessageFormat.format("No exported bundles match symbolic name \"{0}\".", dependency.getSymbolicName());
				ResolutionProblem problem = new ResolutionProblem(message, dependency);
				markerUpdateJob.addResolutionProblem(problem);
			}
		}
		markerUpdateJob.schedule();
		return bindings;
	}

	// Find the export so we can get its source bnd file (if any)
	private ExportedBundle findExport(IWorkspaceRoot root, IPath path) {
		Map<IPath, ExportedBundle> exports = exportsMap.get(path.segment(0));
		return exports != null ? exports.get(path) : null;
	}
	
	private boolean checkForCycles(String startingProject, ExportedBundle export) {
		IPath bndPath = export.getSourceBndFilePath();
		
		// If no source bnd file then this cannot be part of a cycle, since it is not built by us.
		if(bndPath == null) {
			return false;
		}
		
		// If the bndFile is the starting project, then this export would directly result in a cycle
		if(startingProject.equals(bndPath.segment(0))) {
			return true;
		}

		// Find the dependencies of the project exporting the bundle, and recurse
		WorkspaceRepositoryClasspathContainer container = projectContainerMap.get(bndPath.segment(0));
		if(container != null) {
			for (BundleDependency transitiveDep : container.getDependencies()) {
				ExportedBundle transitiveBinding = container.getBinding(transitiveDep);
				if(transitiveBinding != null) {
					if(checkForCycles(startingProject, transitiveBinding)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Get all the dependency bindings for the specified project
	 * 
	 * @param project
	 *            The project to examine
	 * @return A map of dependencies to bindings for those dependencies. The map
	 *         will not contain any entries for unbound dependencies, so they
	 *         must be discovered by another means.
	 */
	public Map<BundleDependency, ExportedBundle> getBindingsForProject(IProject project) {
		WorkspaceRepositoryClasspathContainer container = projectContainerMap.get(project.getName());
		if(container == null) {
			return Collections.emptyMap();
		}
		return container.getAllBindings();
	}
	public List<ExportedBundle> getAllWorkspaceExports() {
		List<ExportedBundle> result = new ArrayList<ExportedBundle>();
		for(Entry<String, Map<IPath, ExportedBundle>> entry : exportsMap.entrySet()) {
			Map<IPath, ExportedBundle> pathMap = entry.getValue();
			for (Entry<IPath, ExportedBundle> pathEntry : pathMap.entrySet()) {
				result.add(pathEntry.getValue());
			}
		}
		return result;
	}
	private static BundleDependency parsedEntryToDependency(Entry<String, Map<String, String>> entry) {
		String symbolicName = entry.getKey();
		String versionRangeStr = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
		VersionRange versionRange;
		if(versionRangeStr != null) {
			versionRange = new VersionRange(versionRangeStr);
		} else {
			versionRange = new VersionRange("0.0.0");
		}
		BundleDependency dependency = new BundleDependency(symbolicName, versionRange);
		return dependency;
	}

	/**
	 * Process a set of changes to the exported bundles for a project.
	 * 
	 * @param project
	 *            The project whose exported bundles may be changing.
	 * @param deletedJarFiles
	 *            A list of JAR files in the project, which may or may not be
	 *            bundles that are currently exported by the project.
	 * @param changedBundles
	 *            A list of changed or added bundles in the project
	 * @param monitor
	 *            the progress monitor to use for reporting progress to the
	 *            user. It is the caller's responsibility to call done() on the
	 *            given monitor. Accepts null, indicating that no progress
	 *            should be reported and that the operation cannot be cancelled.
	 * @return
	 */
	public IStatus bundlesChanged(IProject project, List<IFile> deletedJarFiles, List<ExportedBundle> changedBundles, IProgressMonitor monitor) {
		Set<String> affectedProjects = new HashSet<String>();

		processDeletedJarFiles(project, deletedJarFiles, affectedProjects);
		processChangedBundles(project, changedBundles, affectedProjects);
		
		return updateClasspathContainers(affectedProjects, monitor);
	}
	
	/**
	 * Set or reset the exports for the specified project.
	 * 
	 * @param project
	 *            The project whose exported bundles are being reset.
	 * @param bundles
	 *            A list of changed or added bundles in the project
	 * @param monitor
	 *            the progress monitor to use for reporting progress to the
	 *            user. It is the caller's responsibility to call done() on the
	 *            given monitor. Accepts null, indicating that no progress
	 *            should be reported and that the operation cannot be cancelled.
	 * @return The status of the operation.
	 */
	public IStatus resetProjectExports(IProject project, List<ExportedBundle> bundles, IProgressMonitor monitor) {
		Set<String> affectedProjects = new HashSet<String>();

		removeAllExports(project, affectedProjects);
		processChangedBundles(project, bundles, affectedProjects);
		
		return updateClasspathContainers(affectedProjects, monitor);
		
	}

	// Fix the classpath containers for the affected projects
	private IStatus updateClasspathContainers(Collection<String> affectedProjects, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Error(s) occurred processing change to exported bundles.", null);

		SubMonitor progress = SubMonitor.convert(monitor, affectedProjects.size());
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String projectName : affectedProjects) {
			IProject dependerProject = root.getProject(projectName);
			IJavaProject javaProject = JavaCore.create(dependerProject);
			
			WorkspaceRepositoryClasspathContainer oldContainer = projectContainerMap.get(projectName);
			Collection<BundleDependency> dependencies = oldContainer.getDependencies();
			Map<BundleDependency, ExportedBundle> newBindings = calculateBindings(javaProject, dependencies);
			
			WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(oldContainer.getPath(), oldContainer.getJavaProject(), dependencies, newBindings);
			projectContainerMap.put(projectName, newContainer);
			
			try {
				JavaCore.setClasspathContainer(newContainer.getPath(), new IJavaProject[] { javaProject }, new IClasspathContainer[] { newContainer }, progress.newChild(1));
			} catch (JavaModelException e) {
				status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to set classpath for project '{0}'", projectName), e));
			}
		}
		
		return status;
	}
	private void removeAllExports(IProject project, Collection<? super String> affectedProjects) {
		Map<IPath, ExportedBundle> exportedBundles = exportsMap.remove(project.getName());
		if(exportedBundles != null) {
			for (Entry<IPath, ExportedBundle> entry : exportedBundles.entrySet()) {
				Set<String> projects = removeBundle(entry.getValue());
				if(projects != null) {
					affectedProjects.addAll(projects);
				}
			}
		}
	}
	private void processDeletedJarFiles(IProject project, List<IFile> deletedJarFiles, Collection<? super String> affectedProjects) {
		Map<IPath, ExportedBundle> exportedBundles = exportsMap.get(project.getName());
		if(deletedJarFiles != null) {
			for (IFile deletedJarFile : deletedJarFiles) {
				if(exportedBundles != null) {
					ExportedBundle removedBundle = exportedBundles.remove(deletedJarFile.getFullPath());
					if(removedBundle != null) {
						Set<String> projects = removeBundle(removedBundle);
						if(projects != null) {
							affectedProjects.addAll(projects);
						}
					}
					if(exportedBundles.isEmpty()) {
						exportsMap.remove(project.getName());
					}
				}
			}
		}
	}
	private void processChangedBundles(IProject project, List<ExportedBundle> changedBundles, Collection<? super String> affectedProjects) {
		Map<IPath, ExportedBundle> exportedBundles = exportsMap.get(project.getName());
		if(changedBundles != null && !changedBundles.isEmpty()) {
			if(exportedBundles == null) {
				exportedBundles = new HashMap<IPath, ExportedBundle>();
				exportsMap.put(project.getName(), exportedBundles);
			}
			for(ExportedBundle changedBundle : changedBundles) {
				ExportedBundle priorEntry = exportedBundles.put(changedBundle.getPath(), changedBundle);
				if(priorEntry == null) {
					affectedProjects.addAll(addBundle(changedBundle));
				} else {
					if(!priorEntry.getSymbolicName().equals(changedBundle.getSymbolicName()) || !priorEntry.getVersion().equals(changedBundle.getVersion())) {
						affectedProjects.addAll(removeBundle(priorEntry));
						affectedProjects.addAll(addBundle(changedBundle));
					}
				}
			}
		}
	}
	
	/**
	 * Add the bundle to the workspace
	 * 
	 * @param bundle
	 *            The bundle that is being added
	 * @return The set of projects that may be affected by this addition, e.g.
	 *         they should import the new bundle.
	 */
	private Set<String> addBundle(ExportedBundle bundle) {
		// Add to the workspace bundles
		Map<Version, SortedSet<IPath>> versionMap = workspaceBundleMap.get(bundle.getSymbolicName());
		if(versionMap == null) {
			versionMap = new HashMap<Version, SortedSet<IPath>>();
			workspaceBundleMap.put(bundle.getSymbolicName(), versionMap);
		}
		SortedSet<IPath> paths = versionMap.get(bundle.getVersion());
		if(paths == null) {
			paths = new TreeSet<IPath>(new Comparator<IPath>() {
				public int compare(IPath o1, IPath o2) {
					return o1.toString().compareTo(o2.toString());
				}
			});
			versionMap.put(bundle.getVersion(), paths);
		}
		paths.add(bundle.getPath());
		
		// Work out which projects are affected (i.e. should import the new bundle)
		Set<String> affectedProjects = new HashSet<String>();
		for (Entry<String, WorkspaceRepositoryClasspathContainer> projectEntry: projectContainerMap.entrySet()) {
			String projectName = projectEntry.getKey();
			WorkspaceRepositoryClasspathContainer container = projectEntry.getValue();
			for (BundleDependency dependency : container.getDependencies()) {
				if(dependency.getSymbolicName().equals(bundle.getSymbolicName()) && dependency.getVersionRange().includes(bundle.getVersion())) {
					// Interesting if not already bound to an equal or higher version
					ExportedBundle boundExport = container.getBinding(dependency);
					if(boundExport == null || boundExport.getVersion().compareTo(bundle.getVersion()) <= 0) {
						affectedProjects.add(projectName);
						break;
					}
				}
			}
		}
		return affectedProjects;
	}
	/**
	 * Remove the exported bundle from the workspace.
	 * 
	 * @param bundle
	 *            The bundle that is being removed.
	 * @return The set of projects that will be affected by this removal, i.e.
	 *         because they import the bundle.
	 */
	private Set<String> removeBundle(ExportedBundle bundle) {
		// Update the workspace bundles
		final Map<Version, SortedSet<IPath>> versionMap = workspaceBundleMap.get(bundle.getSymbolicName());
		if(versionMap != null) {
			SortedSet<IPath> paths = versionMap.get(bundle.getVersion());
			if(paths != null) {
				paths.remove(bundle.getPath());
				if(paths.isEmpty()) {
					versionMap.remove(bundle.getVersion());
					if(versionMap.isEmpty()) {
						workspaceBundleMap.remove(bundle.getSymbolicName());
					}
				}
			}
		}
		
		// Work out which projects are affected
		Set<String> affectedProjects = new HashSet<String>();
		for (Entry<String, WorkspaceRepositoryClasspathContainer> projectEntry: projectContainerMap.entrySet()) {
			String projectName = projectEntry.getKey();
			WorkspaceRepositoryClasspathContainer container = projectEntry.getValue();
			if(container.isBoundToPath(bundle.getPath())) {
				affectedProjects.add(projectName);
			}
		}
		
		return affectedProjects;
	}
}