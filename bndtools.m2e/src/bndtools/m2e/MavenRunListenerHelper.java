package bndtools.m2e;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import aQute.bnd.build.Run;
import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.lib.exceptions.Exceptions;

public interface MavenRunListenerHelper {

	IMaven					maven					= MavenPlugin.getMaven();
	IMavenProjectRegistry	mavenProjectRegistry	= MavenPlugin.getMavenProjectRegistry();
	IWorkspace				iWorkspace				= ResourcesPlugin.getWorkspace();

	default IResource getResource(Run run) {
		File propertiesFile = run.getPropertiesFile();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IFile[] locations = root.findFilesForLocationURI(propertiesFile.toURI());
		IFile shortest = null;
		for (IFile f : locations) {
			if (shortest == null || (f.getProjectRelativePath()
				.segmentCount() < shortest.getProjectRelativePath()
					.segmentCount())) {
				shortest = f;
			}
		}
		return shortest;
	}

	default MavenProject getMavenProject(IMavenProjectFacade mavenProjectFacade) {
		try {
			return mavenProjectFacade.getMavenProject(new NullProgressMonitor());
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	default IMavenProjectFacade getMavenProjectFacade(IResource resource) {
		return mavenProjectRegistry.getProject(resource.getProject());
	}

	default boolean isMavenProject(IResource resource) {
		if ((resource != null) && (resource.getProject() != null) && (getMavenProjectFacade(resource) != null)) {
			return true;
		}

		return false;
	}

	default boolean hasBndMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
		return getMavenProject(projectFacade).getPlugin("biz.aQute.bnd:bnd-maven-plugin")
			.getExecutionsAsMap()
			.values()
			.stream()
			.flatMap(pe -> pe.getGoals()
				.stream())
			.anyMatch("bnd-process"::equals);
	}

	default boolean hasBndResolverMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
		return getBndResolverMavenPlugin(projectFacade).isPresent();
	}

	default Optional<PluginExecution> getBndResolverMavenPlugin(IMavenProjectFacade projectFacade)
		throws CoreException {
		return getMavenProject(projectFacade).getPlugin("biz.aQute.bnd:bnd-resolver-maven-plugin")
			.getExecutionsAsMap()
			.values()
			.stream()
			.filter(pe -> pe.getGoals()
				.stream()
				.anyMatch("resolve"::equals))
			.findFirst();
	}

	default MojoExecution getBndResolverMojoExecution(IMavenProjectFacade projectFacade,
		IProgressMonitor monitor) throws CoreException {
		return getMojoExecution(projectFacade, "biz.aQute.bnd:bnd-resolver-maven-plugin", "bnd-resolver:resolve@",
			exe -> true, monitor);
	}

	default MojoExecution getBndResolverMojoExecution(IMavenProjectFacade projectFacade,
		Predicate<MojoExecution> predicate,
		IProgressMonitor monitor) throws CoreException {
		return getMojoExecution(projectFacade, "biz.aQute.bnd:bnd-resolver-maven-plugin", "bnd-resolver:resolve@",
			predicate, monitor);
	}

	default boolean hasBndTestingMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
		return getMavenProject(projectFacade).getPlugin("biz.aQute.bnd:bnd-testing-maven-plugin")
			.getExecutionsAsMap()
			.values()
			.stream()
			.flatMap(pe -> pe.getGoals()
				.stream())
			.anyMatch("testing"::equals);
	}

	default Optional<PluginExecution> getBndTestingMavenPlugin(IMavenProjectFacade projectFacade) throws CoreException {
		return getMavenProject(projectFacade).getPlugin("biz.aQute.bnd:bnd-testing-maven-plugin")
			.getExecutionsAsMap()
			.values()
			.stream()
			.filter(pe -> pe.getGoals()
				.stream()
				.anyMatch("resolve"::equals))
			.findFirst();
	}

	default MojoExecution getBndTestingMojoExecution(IMavenProjectFacade projectFacade,
		IProgressMonitor monitor) throws CoreException {
		return getMojoExecution(projectFacade, "biz.aQute.bnd:bnd-testing-maven-plugin", "bnd-testing:testing@",
			exe -> true, monitor);
	}

	default MojoExecution getBndTestingMojoExecution(IMavenProjectFacade projectFacade,
		Predicate<MojoExecution> predicate,
		IProgressMonitor monitor) throws CoreException {
		return getMojoExecution(projectFacade, "biz.aQute.bnd:bnd-testing-maven-plugin", "bnd-testing:testing@",
			predicate, monitor);
	}

	default MojoExecution getMojoExecution(IMavenProjectFacade projectFacade, String pluginId, String executionPrefix,
		Predicate<MojoExecution> predicate, IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = getMavenProject(projectFacade);
		List<String> tasks = getMavenProject(projectFacade).getPlugin(pluginId)
			.getExecutionsAsMap()
			.keySet()
			.stream()
			.map(executionPrefix::concat)
			.collect(toList());

		MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, tasks, true, monitor);
		return plan.getMojoExecutions()
			.stream()
			.filter(predicate)
			.findFirst()
			.orElse(null);
	}

	default boolean isOffline() {
		try {
			return maven.getSettings()
				.isOffline();
		} catch (CoreException e) {
			throw Exceptions.duck(e);
		}
	}

	default <T> T lookupComponent(Class<T> clazz) {
		try {
			Method lookupComponentMethod = maven.getClass()
				.getMethod("lookupComponent", Class.class);

			return clazz.cast(lookupComponentMethod.invoke(maven, clazz));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
			| SecurityException e) {
			return null;
		}
	}

	default boolean containsBndrun(MojoExecution mojoExecution, MavenProject mavenProject, File bndrunFile,
		IProgressMonitor monitor) {
		try {
			Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class,
				monitor);

			return bndruns.getFiles(mavenProject.getBasedir())
				.contains(bndrunFile);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
