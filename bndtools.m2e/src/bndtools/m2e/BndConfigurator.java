package bndtools.m2e;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.builder.IProjectDecorator;
import org.bndtools.api.builder.IProjectDecorator.BndProjectInfo;
import org.bndtools.facade.ExtensionFacade;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant2;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.maven.lib.executions.PluginExecutions;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;

public class BndConfigurator extends ServiceAwareM2EConfigurator {

	private static final String					ARTIFACT_PATTERN				= "%s-%s.%s";
	private static final String					CLASSIFIER_ARTIFACT_PATTERN	= "%s-%s-%s.%s";

	@SuppressWarnings("rawtypes")
	protected final static Class<ExtensionFacade> shortcutClazz;
	static {
		/*
		 * The class JUnitShortcut is loaded here because: a) this class is
		 * loaded immediately to active the bndtools.m2e integration b) other
		 * bndtools.m2e extensions instruct Eclipse to add shortcuts using this
		 * type provided by bndtools.core and only referred to in the
		 * plugin.xml. This reference causes the import to be added to
		 * bndtools.m2e so that this process works.
		 */
		shortcutClazz = ExtensionFacade.class;
	}

	ILogger logger = Logger.getLogger(BndConfigurator.class);

	public static class MavenProjectInfo implements BndProjectInfo {

		private final MavenProject	project;

		private final Packages		exports;
		private final Packages		imports;
		private final Packages		contained;

		public MavenProjectInfo(MavenProject project) throws Exception {
			this.project = project;
			File file = new File(project.getBuild()
				.getOutputDirectory());
			if (!file.exists()) {
				throw new IllegalStateException(
					"The output directory for project " + project.getName() + " does not exist");
			}

			try (Jar jar = new Jar(file); Analyzer analyzer = new Analyzer(jar)) {
				analyzer.analyze();
				exports = analyzer.getExports();
				imports = analyzer.getImports();
				contained = analyzer.getContained();
			}
		}

		@Override
		public Collection<File> getSourcePath() throws Exception {
			List<File> sourcePath = new ArrayList<>();
			for (String path : project.getCompileSourceRoots()) {
				sourcePath.add(new File(path));
			}
			return sourcePath;
		}

		@Override
		public Packages getExports() {
			return exports;
		}

		@Override
		public Packages getImports() {
			return imports;
		}

		@Override
		public Packages getContained() {
			return contained;
		}

	}

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {}

	@Override
	public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade,
		final MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
		return new MojoExecutionBuildParticipant(execution, true, true) {
			@Override
			public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
				String goal = execution.getGoal();
				monitor.beginTask("Executing Goal " + goal, 2);

				final IProject project = projectFacade.getProject();

				// Check if we need to run.
				if (!needsBuilding(getDelta(project), projectFacade)) {
					monitor.done();
					return null;
				}

				// Build mojo like normal
				final Set<IProject> build = super.build(kind, monitor);
				monitor.worked(1);

				// Nothing to do if configuration build
				if (kind == AbstractBuildParticipant2.PRECONFIGURE_BUILD) {
					return build;
				}

				// If a BND packaging goal was used we don't need to call the
				// maven-{packaging}-plugin
				if (hasBndPackaging(execution, projectFacade)) {
					scheduleDecorate(projectFacade);
					monitor.done();
					return build;
				}

				boolean isTest = goal.endsWith("-tests");

				IMarker[] imarkers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);

				if (imarkers != null && Arrays.stream(imarkers)
					.map(m -> m.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO))
					.anyMatch(s -> s == IMarker.SEVERITY_ERROR)) {
					// there are compile errors, don't build jar
					return build;
				}

				final String targetDirectory = projectFacade.getMavenProject()
					.getBuild()
					.getDirectory();

				// now we make sure jar is built in separate job, doing this
				// during maven builder will throw lifecycle
				// errors

				Job job = new WorkspaceJob(
					"Executing " + project.getName() + " jar:" + (isTest ? "test-" : "") + "jar goal") {
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						try {
							SubMonitor progress = SubMonitor.convert(monitor, 3);
							execJarMojo(getMaven(), getRegistry(), projectFacade,
								progress.split(1, SubMonitor.SUPPRESS_NONE), isTest);

							// We need to trigger a refresh
							IPath targetDirPath = Path.fromOSString(targetDirectory);
							IPath projectPath = project.getLocation();
							IPath relativeTargetDirPath = targetDirPath.makeRelativeTo(projectPath);
							IFolder targetDir = project.getFolder(relativeTargetDirPath);

							project.getFolder(relativeTargetDirPath)
								.refreshLocal(IResource.DEPTH_INFINITE, progress.split(1));
							return Status.OK_STATUS;
						} catch (Throwable e) {
							return new Status(IStatus.ERROR, this.getClass(),
								"Error executing jar goal: " + e.getMessage(), e);
						}
					}
				};
				job.setRule(project);
				job.schedule();
				monitor.worked(1);
				return build;
			}

		};
	}

	private boolean hasBndPackaging(MojoExecution execution, IMavenProjectFacade projectFacade) {
		boolean isTest = execution.getGoal().endsWith("-tests");

		return execution.getPlugin()
			.getExecutions()
			.stream()
			.filter(PluginExecutions::isPackagingGoal)
			.map(PluginExecutions::extractClassifier)
			.anyMatch(classifier -> isTest ? "tests".equals(classifier) : "".equals(classifier));
	}

	private boolean needsBuilding(IResourceDelta delta, IMavenProjectFacade projectFacade) {
		if (delta == null)
			return true;
		IResourceDelta[] children = delta.getAffectedChildren();
		Stream<IResourceDelta> descendants = Arrays.stream(children)
			.filter(Objects::nonNull)
			.flatMap(this::collectResourceDeltas);
		Optional<IResourceDelta> leftOver = descendants
			.filter(Objects::nonNull)
			.filter(d -> {
				IPath fullPath = d.getResource()
					.getFullPath();
				if(d.getKind() != IResourceDelta.CHANGED) {
					return true;
				}
				if(fullPath.toFile().isDirectory()) {
					return false;
				} else if (isOurArtifact(fullPath.lastSegment(), projectFacade)
					&& (d.getFlags() & IResourceDelta.CONTENT) != 0) {
					return false;
				}
				return true;
			})
			.findFirst();
		return leftOver.isPresent();
	}


	private boolean isOurArtifact(String lastSegment, IMavenProjectFacade projectFacade) {
		ArtifactKey artifactKey = projectFacade.getArtifactKey();
		String artifact = null;
		if (artifactKey.classifier() == null) {
			artifact = String.format(ARTIFACT_PATTERN, artifactKey.artifactId(), artifactKey.version(),
				projectFacade.getMavenProject()
					.getPackaging());
		} else {
			artifact = String.format(CLASSIFIER_ARTIFACT_PATTERN, artifactKey.artifactId(), artifactKey.version(),
				artifactKey.classifier(), projectFacade.getMavenProject()
					.getPackaging());
		}
		if (artifact.equals(lastSegment)) {
			return true;
		}
		artifact = String.format(CLASSIFIER_ARTIFACT_PATTERN, artifactKey.artifactId(), artifactKey.version(), "tests",
			projectFacade.getMavenProject()
				.getPackaging());
		return artifact.equals(lastSegment);
	}

	private Stream<IResourceDelta> collectResourceDeltas(IResourceDelta delta) {
		if (delta == null)
			return Stream.empty();
		IResourceDelta[] children = delta.getAffectedChildren();
		Stream<IResourceDelta> descendants = Arrays.stream(children)
			.filter(Objects::nonNull)
			.flatMap(this::collectResourceDeltas);
		return Stream.concat(descendants, Arrays.stream(children));
	}

	private MavenProject getMavenProject(final IMavenProjectFacade projectFacade, IProgressMonitor monitor)
		throws CoreException {
		MavenProject mavenProject = projectFacade.getMavenProject();

		if (mavenProject == null) {
			mavenProject = projectFacade.getMavenProject(monitor);
		}

		monitor.done();

		return mavenProject;
	}

	private void execJarMojo(IMaven maven, IMavenProjectRegistry projectRegistry,
		final IMavenProjectFacade projectFacade, IProgressMonitor monitor, boolean isTest)
		throws CoreException {
		projectFacade.getResolverConfiguration()
			.setResolveWorkspaceProjects(true);

		projectRegistry.execute(projectFacade, (context1, monitor1) -> {
			SubMonitor progress = SubMonitor.convert(monitor1);
			MavenProject mavenProject = getMavenProject(projectFacade, progress.split(1));
			mavenProject.getArtifact().setFile(null);

			List<MojoExecution> mojoExecutions = null;
			if (!isTest) {
				mojoExecutions = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-jar-plugin",
					monitor1, "jar");

				if (mojoExecutions.isEmpty()) {
					mojoExecutions = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-war-plugin",
						monitor1, "war");
				}
			} else {
				mojoExecutions = projectFacade.getMojoExecutions("org.apache.maven.plugins", "maven-jar-plugin",
					monitor1, "test-jar");
			}

			for (MojoExecution mojoExecution : mojoExecutions) {
				context1.execute(mavenProject, mojoExecution, progress.split(1));
			}

			// We can now decorate based on the build we just did.
			scheduleDecorate(projectFacade);

			return null;
		}, monitor);
	}

	public void scheduleDecorate(final IMavenProjectFacade projectFacade) {
		final IProject project = projectFacade.getProject();

		Job job = new WorkspaceJob("Executing " + project.getName() + " Bndtools decoration") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					MavenProject mavenProject = getMavenProject(projectFacade, monitor);
					IProjectDecorator decorator = Injector.ref.get();
					if (decorator != null) {
						BndProjectInfo info = new MavenProjectInfo(mavenProject);
						decorator.updateDecoration(project, info);
					}

					monitor.done();
					return Status.OK_STATUS;
				} catch (Throwable e) {
					return new Status(IStatus.ERROR, this.getClass(),
						"Error executing " + project.getName() + " Bndtools decoration" + e.getMessage(), e);
				}
			}
		};
		job.setRule(project);
		job.schedule();
	}

	@Component
	public static class Injector {

		private static AtomicReference<IProjectDecorator> ref = new AtomicReference<>();

		@Reference
		void setDecorator(IProjectDecorator decorator) {
			ref.set(decorator);
		}

		void unsetDecorator(IProjectDecorator decorator) {
			ref.compareAndSet(decorator, null);
		}
	}
}
