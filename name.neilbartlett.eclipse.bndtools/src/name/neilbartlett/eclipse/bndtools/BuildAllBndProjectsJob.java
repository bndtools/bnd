package name.neilbartlett.eclipse.bndtools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import name.neilbartlett.eclipse.bndtools.builder.BndProjectNature;
import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.utils.CircularDependencyException;
import name.neilbartlett.eclipse.bndtools.utils.DependencyUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class BuildAllBndProjectsJob extends Job {

	public BuildAllBndProjectsJob(String name) {
		super(name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();

		// Find the projects to build and their dependencies
		final Map<String, IProject> projectMap = new HashMap<String, IProject>();
		final Map<String, Set<String>> dependencies = new HashMap<String, Set<String>>();
		for (IProject project : projects) {
			if(project.isOpen()) {
				try {
					IProjectNature bndNature = project.getNature(BndProjectNature.NATURE_ID);
					if(bndNature != null) {
						IJavaProject javaProject = JavaCore.create(project);
						String[] projectDeps = getWorkspaceRepositoryProjectDependences(javaProject);
						dependencies.put(project.getName(), new HashSet<String>(Arrays.asList(projectDeps)));
						projectMap.put(project.getName(), project);
					}
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Build them
		IWorkspaceRunnable operation = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					DependencyUtils.processDependencyMap(dependencies.keySet(), dependencies, new DependencyUtils.Processor<String>() {
						public void process(String projectName, IProgressMonitor monitor) throws CoreException {
							IProject project = projectMap.get(projectName);
							if(project != null)
								project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
						}
					}, monitor);
				} catch (CircularDependencyException e) {
					throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Dependency cycle detected", e));
				}
			}
		};
		
		ISchedulingRule rule = workspace.getRuleFactory().buildRule();
		try {
			workspace.run(operation, rule, 0, monitor);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error building Bnd projects.", e);
		}
		
	}
	
	private String[] getWorkspaceRepositoryProjectDependences(IJavaProject javaProject) throws JavaModelException {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (IClasspathEntry entry : entries) {
			if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				IPath containerPath = entry.getPath();
				if(WorkspaceRepositoryClasspathContainerInitializer.CONTAINER_ID.equals(containerPath.segment(0))) {
					StringTokenizer tokenizer = new StringTokenizer(containerPath.segment(1), ",");
					List<String> depends = new LinkedList<String>();
					while(tokenizer.hasMoreTokens()) {
						String token = tokenizer.nextToken().trim();
						depends.add(token);
					}
					return (String[]) depends.toArray(new String[depends.size()]);
				}
			}
		}
		return new String[0];
	}

}
