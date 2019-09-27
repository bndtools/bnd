package aQute.bnd.maven.lib.resolve;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.EE;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.RepositoryPlugin;
import biz.aQute.resolve.Bndrun;

public class BndrunContainer {

	private static final Logger										logger	= LoggerFactory
		.getLogger(BndrunContainer.class);

	private final List<File>										bundles;

	private final boolean											includeDependencyManagement;

	private final MavenProject										project;

	private final RepositorySystemSession							repositorySession;

	private final Set<Scope>										scopes;

	private final MavenSession										session;

	private final boolean											useMavenDependencies;

	@SuppressWarnings("deprecation")
	private final org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;

	private final ProjectDependenciesResolver						resolver;

	private final RepositorySystem									system;

	private final boolean											transitive;

	private final PostProcessor										postProcessor;

	private FileSetRepository										fileSetRepository;

	private Processor												processor;

	public static class Builder {

		private final MavenProject										project;
		private final MavenSession										session;
		private final RepositorySystemSession							repositorySession;
		private final ProjectDependenciesResolver						resolver;
		@SuppressWarnings("deprecation")
		private final org.apache.maven.artifact.factory.ArtifactFactory	artifactFactory;
		private final RepositorySystem									system;
		private List<File>												bundles						= Collections
			.emptyList();
		private boolean													includeDependencyManagement	= false;
		private Set<Scope>												scopes						= new HashSet<>(
			Arrays.asList(Scope.compile, Scope.runtime));
		private boolean													useMavenDependencies		= true;
		private boolean													transitive					= true;
		private PostProcessor											postProcessor				= new LocalPostProcessor();

		@SuppressWarnings("deprecation")
		public Builder(MavenProject project, MavenSession session, RepositorySystemSession repositorySession,
			ProjectDependenciesResolver resolver, org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
			RepositorySystem system) {

			this.project = Objects.requireNonNull(project);
			this.session = Objects.requireNonNull(session);
			this.repositorySession = Objects.requireNonNull(repositorySession);
			this.resolver = Objects.requireNonNull(resolver);
			this.artifactFactory = Objects.requireNonNull(artifactFactory);
			this.system = Objects.requireNonNull(system);
		}

		public Builder setBundles(List<File> bundles) {
			this.bundles = bundles;
			return this;
		}

		public Builder setIncludeDependencyManagement(boolean includeDependencyManagement) {
			this.includeDependencyManagement = includeDependencyManagement;
			return this;
		}

		public Builder setPostProcessor(PostProcessor postProcessor) {
			this.postProcessor = postProcessor;
			return this;
		}

		public Builder setScopes(Set<Scope> scopes) {
			this.scopes = scopes;
			return this;
		}

		public Builder setTransitive(boolean transitive) {
			this.transitive = transitive;
			return this;
		}

		public Builder setUseMavenDependencies(boolean useMavenDependencies) {
			this.useMavenDependencies = useMavenDependencies;
			return this;
		}

		public BndrunContainer build() {
			return new BndrunContainer(project, session, resolver, repositorySession, artifactFactory, system, scopes,
				bundles, useMavenDependencies, includeDependencyManagement, transitive, postProcessor);
		}

	}

	public static int report(Bndrun run) {
		int errors = 0;
		for (String warning : run.getWarnings()) {
			logger.warn("Warning : {}", warning);
		}
		for (String error : run.getErrors()) {
			logger.error("Error   : {}", error);
			errors++;
		}
		return errors;
	}

	@SuppressWarnings("deprecation")
	BndrunContainer(MavenProject project, MavenSession session, ProjectDependenciesResolver resolver,
		RepositorySystemSession repositorySession, org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
		RepositorySystem system, Set<Scope> scopes, List<File> bundles, boolean useMavenDependencies,
		boolean includeDependencyManagement, boolean transitive, PostProcessor postProcessor) {
		this.project = project;
		this.session = session;
		this.resolver = resolver;
		this.repositorySession = repositorySession;
		this.artifactFactory = artifactFactory;
		this.system = system;
		this.scopes = scopes;
		this.bundles = bundles;
		this.useMavenDependencies = useMavenDependencies;
		this.includeDependencyManagement = includeDependencyManagement;
		this.transitive = transitive;
		this.postProcessor = postProcessor;
	}

	public int execute(File runFile, String task, File workingDir, Operation operation) throws Exception {
		if (!runFile.exists()) {
			logger.error("Could not find bnd run file {}", runFile);
			return 1;
		}
		String bndrun = getNamePart(runFile);
		File temporaryDir = workingDir.toPath()
			.resolve("tmp")
			.resolve(task)
			.resolve(bndrun)
			.toFile();
		File cnf = new File(temporaryDir, Workspace.CNFDIR);
		aQute.lib.io.IO.mkdirs(cnf);
		try (Bndrun run = Bndrun.createBndrun(null, runFile)) {
			run.setBase(temporaryDir);
			Workspace workspace = run.getWorkspace();
			workspace.setBuildDir(cnf);
			workspace.setOffline(session.getSettings()
				.isOffline());
			workspace.addBasicPlugin(getFileSetRepository());
			run.setParent(getProcessor(workspace));
			for (RepositoryPlugin repo : workspace.getRepositories()) {
				repo.list(null);
			}
			setRunrequiresFromProjectArtifact(run);
			setEEfromBuild(run);
			run.getInfo(workspace);
			int errors = report(run);
			if (!run.isOk()) {
				return errors;
			}
			return operation.apply(runFile, bndrun, run);
		}
	}

	public FileSetRepository getFileSetRepository() throws Exception {
		if (fileSetRepository != null) {
			return fileSetRepository;
		}

		if (includeDependencyManagement) {
			includeDependencyManagement(project, artifactFactory);
		}

		DependencyResolver dependencyResolver = new DependencyResolver(project, repositorySession, resolver, system,
			scopes, transitive, postProcessor);

		String name = project.getName()
			.isEmpty() ? project.getArtifactId() : project.getName();

		return fileSetRepository = dependencyResolver.getFileSetRepository(name, bundles, useMavenDependencies);
	}

	public void setRunrequiresFromProjectArtifact(Run run) {
		String runrequires = run.getProperty(Constants.RUNREQUIRES);

		if (runrequires == null && ("jar".equals(project.getPackaging()) || "war".equals(project.getPackaging()))
			&& (project.getPlugin("biz.aQute.bnd:bnd-maven-plugin") != null)) {

			Artifact artifact = project.getArtifact();

			run.setProperty(Constants.RUNREQUIRES,
				String.format("osgi.identity;filter:='(osgi.identity=%s)'", artifact.getArtifactId()));

			logger.info("Bnd inferred {}: {}", Constants.RUNREQUIRES, run.getProperty(Constants.RUNREQUIRES));
		}
	}

	public void setEEfromBuild(Run run) {
		String runee = run.getProperty(Constants.RUNEE);

		if (runee == null) {
			Optional.ofNullable(project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin"))
				.map(Plugin::getConfiguration)
				.map(Xpp3Dom.class::cast)
				.map(dom -> dom.getChild("target"))
				.map(Xpp3Dom::getValue)
				.flatMap(EE::highestFromTargetVersion)
				.ifPresent(ee -> {
					run.setProperty(Constants.RUNEE, ee.getEEName());

					logger.info("Bnd inferred {}: {}", Constants.RUNEE, run.getProperty(Constants.RUNEE));
				});
		}
	}

	private String getNamePart(File runFile) {
		String nameExt = runFile.getName();
		int pos = nameExt.lastIndexOf('.');
		return (pos > 0) ? nameExt.substring(0, pos) : nameExt;
	}

	private Processor getProcessor(Workspace workspace) {
		if (processor != null) {
			return processor;
		}

		Properties beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		beanProperties.put("settings", session.getSettings());
		Properties mavenProperties = new Properties(beanProperties);
		mavenProperties.putAll(project.getProperties());
		return processor = new Processor(workspace, mavenProperties, false);
	}

	@SuppressWarnings("deprecation")
	private void includeDependencyManagement(MavenProject mavenProject,
		org.apache.maven.artifact.factory.ArtifactFactory artifactFactory) {
		if (mavenProject.getDependencyManagement() != null) {
			List<Dependency> dependencies = mavenProject.getDependencies();

			mavenProject.getDependencyManagement()
				.getDependencies()
				.forEach(dependencies::add);

			try {
				mavenProject.setDependencyArtifacts(mavenProject.createArtifacts(artifactFactory, null, null));
			} catch (Exception e) {
				throw aQute.lib.exceptions.Exceptions.duck(e);
			}
		}
	}

}
