package aQute.bnd.maven.lib.resolve;

import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.maven.lib.artifact.ProjectArtifactCollector;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.stream.MapStream;
import aQute.bnd.unmodifiable.Lists;

@ProviderType
public class DependencyResolver {

	private static final Logger														logger					= LoggerFactory
		.getLogger(DependencyResolver.class);

	private static final BiPredicate<ArtifactResult, ArtifactResult>				sameExactVersion		= (a,
		b) -> Objects.equals(a.getArtifact()
			.getArtifactId(),
			b.getArtifact()
				.getArtifactId())
			&& Objects.equals(a.getArtifact()
				.getGroupId(),
				b.getArtifact()
					.getGroupId())
			&& Objects.equals(a.getArtifact()
				.getClassifier(),
				b.getArtifact()
					.getClassifier())
			&& Objects.equals(a.getArtifact()
				.getVersion(),
				b.getArtifact()
					.getVersion())
			&& Objects.equals(a.getArtifact()
				.getExtension(),
			b.getArtifact()
				.getExtension());
	private static final BiPredicate<ArtifactResult, Collection<ArtifactResult>>	containsExactVersion		= (
		resolvedArtifact, collection) -> collection.stream()
			.anyMatch(ra -> sameExactVersion.test(ra, resolvedArtifact));

	private final boolean															includeTransitive;
	private final MavenProject														project;
	final Collection<String>														scopes;
	private final RepositorySystemSession											session;
	private final RepositorySystem													system;
	private final ProjectDependenciesResolver										resolver;
	private final PostProcessor														postProcessor;
	@SuppressWarnings("deprecation")
	private final org.apache.maven.artifact.factory.ArtifactFactory					artifactFactory;
	private final boolean															useMavenDependencies;
	private final boolean															includeDependencyManagement;
	private final ProjectArtifactCollector											projectArtifactCollector	= new ProjectArtifactCollector();

	/**
	 * Shortcut with {@code scopes = ['compile', 'runtime']},
	 * {@code includeTransitive = true}, and always local resources.
	 *
	 * @param project
	 * @param session
	 * @param resolver
	 * @param system
	 */
	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system) {

		this(project, session, resolver, system, null, Lists.of("compile", "runtime"), true, new LocalPostProcessor());
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, Set<Scope> scopes) {

		this(project, session, resolver, system, null, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()), true, new LocalPostProcessor());
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, List<String> scopes) {

		this(project, session, resolver, system, null, scopes, true, new LocalPostProcessor());
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, Set<Scope> scopes, boolean includeTransitive,
		PostProcessor postProcessor) {

		this(project, session, resolver, system, null, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()), includeTransitive, postProcessor);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, List<String> scopes, boolean includeTransitive,
		PostProcessor postProcessor) {

		this(project, session, resolver, system, null, scopes, includeTransitive, postProcessor);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system,
		@SuppressWarnings("deprecation") org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
		Set<Scope> scopes, boolean includeTransitive, PostProcessor postProcessor) {

		this(project, session, resolver, system, artifactFactory, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()), includeTransitive, postProcessor, true, false);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system,
		@SuppressWarnings("deprecation") org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
		Set<Scope> scopes, boolean includeTransitive, PostProcessor postProcessor, boolean useMavenDependencies,
		boolean includeDependencyManagement) {

		this(project, session, resolver, system, artifactFactory, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()), includeTransitive, postProcessor, useMavenDependencies,
			includeDependencyManagement);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system,
		@SuppressWarnings("deprecation") org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
		List<String> scopes, boolean includeTransitive, PostProcessor postProcessor) {

		this(project, session, resolver, system, artifactFactory, scopes, includeTransitive, postProcessor, true,
			false);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system,
		@SuppressWarnings("deprecation") org.apache.maven.artifact.factory.ArtifactFactory artifactFactory,
		List<String> scopes, boolean includeTransitive, PostProcessor postProcessor, boolean useMavenDependencies,
		boolean includeDependencyManagement) {

		this.project = project;
		this.session = session;
		this.resolver = resolver;
		this.system = system;
		this.artifactFactory = artifactFactory;
		this.scopes = scopes;
		this.includeTransitive = includeTransitive;
		this.postProcessor = postProcessor;
		this.useMavenDependencies = useMavenDependencies;
		this.includeDependencyManagement = includeDependencyManagement;
	}

	public Map<File, ArtifactResult> resolve() throws MojoExecutionException {
		return resolve(getProjectRemoteRepositories(), useMavenDependencies, includeDependencyManagement);
	}

	public Map<File, ArtifactResult> resolveAgainstRepos(Collection<ArtifactRepository> repositories)
		throws MojoExecutionException {
		List<RemoteRepository> remoteRepositories = new ArrayList<>(repositories.size());
		for (ArtifactRepository ar : repositories) {
			remoteRepositories.add(RepositoryUtils.toRepo(ar));
		}

		return resolve(remoteRepositories, useMavenDependencies, includeDependencyManagement);
	}

	private Map<File, ArtifactResult> resolve(List<RemoteRepository> remoteRepositories, boolean useMavenDependencies,
		boolean includeDependencyManagement) throws MojoExecutionException {

		Map<File, ArtifactResult> dependencies = new LinkedHashMap<>();

		if (useMavenDependencies) {
			DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

			DependencyResolutionResult result;
			try {
				result = resolver.resolve(request);
			} catch (DependencyResolutionException e) {
				result = e.getResult();
			}

			DependencyNode dependencyGraph = result.getDependencyGraph();

			if (dependencyGraph != null && !dependencyGraph.getChildren()
				.isEmpty()) {
				discoverArtifacts(dependencies, dependencyGraph.getChildren(), project.getArtifact()
					.getId(), remoteRepositories);
			}
		}

		if (includeDependencyManagement && (project.getDependencyManagement() != null) && (artifactFactory != null)) {
			List<Dependency> originalDependencies = Lists.copyOf(project.getDependencies());

			try {
				setDependencyManagementDependencies();

				DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

				DependencyResolutionResult result;
				try {
					result = resolver.resolve(request);
				} catch (DependencyResolutionException e) {
					result = e.getResult();
				}

				Map<File, ArtifactResult> resolved = new LinkedHashMap<>();
				DependencyNode dependencyGraph = result.getDependencyGraph();

				if (dependencyGraph != null && !dependencyGraph.getChildren()
					.isEmpty()) {
					discoverArtifacts(resolved, dependencyGraph.getChildren(), project.getArtifact()
						.getId(), remoteRepositories);
				}

				MapStream.of(resolved)
					.filterValue(v -> containsExactVersion.negate()
						.test(v, dependencies.values()))
					.forEachOrdered(dependencies::put);
			} finally {
				resetOriginalDependencies(originalDependencies);
			}
		}

		return dependencies;
	}

	public FileSetRepository getFileSetRepository(String name, Collection<File> bundlesInputParameter)
		throws Exception {

		return getFileSetRepository(name, bundlesInputParameter, useMavenDependencies, includeDependencyManagement);
	}

	public FileSetRepository getFileSetRepository(String name, Collection<File> bundlesInputParameter,
		boolean useMavenDependencies) throws Exception {

		return getFileSetRepository(name, bundlesInputParameter, useMavenDependencies, includeDependencyManagement);
	}

	public FileSetRepository getFileSetRepository(String name, Collection<File> bundlesInputParameter,
		boolean useMavenDependencies, boolean includeDependencyManagement) throws Exception {

		Collection<File> bundles = MapStream
			.of(resolve(getProjectRemoteRepositories(), useMavenDependencies, includeDependencyManagement))
			.keys()
			.collect(toSet());

		projectArtifactCollector.collect(project)
			.stream()
			.map(Artifact::getFile)
			.forEach(bundles::add);

		if (bundlesInputParameter != null) {
			bundles.addAll(bundlesInputParameter);
		}

		return new ImplicitFileSetRepository(name, bundles);
	}

	private List<RemoteRepository> getProjectRemoteRepositories() {
		List<RemoteRepository> remoteRepositories = new ArrayList<>(project.getRemoteProjectRepositories());
		ArtifactRepository deployRepo = project.getDistributionManagementArtifactRepository();
		if (deployRepo != null) {
			remoteRepositories.add(RepositoryUtils.toRepo(deployRepo));
		}
		return remoteRepositories;
	}

	private void discoverArtifacts(Map<File, ArtifactResult> files, List<DependencyNode> nodes, String parent,
		List<RemoteRepository> remoteRepositories) throws MojoExecutionException {

		List<RemoteRepository> combinedRepositories = new ArrayList<>(remoteRepositories);
		Queue<DependencyNode> processQueue = new LinkedList<>(nodes);
		Set<String> visited = new HashSet<>();
		DependencyNode node = processQueue.poll();
		long nodesChecked = 0;
		long nodesSkipped = 0;
		long nodesFailed = 0;
		while (node != null) {
			String nodeString = node.toString();
			if (!visited.contains(nodeString)) {
				nodesChecked++;
				combinedRepositories.addAll(node.getRepositories());

				// Ensure that the file is downloaded so we can index it
				try {
					ArtifactResult resolvedArtifact = postProcessor.postProcessResult(system.resolveArtifact(session,
						new ArtifactRequest(node.getArtifact(), combinedRepositories, node.getRequestContext())));
					logger.debug("Located file: {} for artifact {}", resolvedArtifact.getArtifact()
						.getFile(), resolvedArtifact);

					// Add artifact only if the scope of this artifact matches
					// and
					// if
					// we don't already have another version (earlier means
					// higher
					// precedence regardless of version)
					if (scopes.contains(node.getDependency()
						.getScope()) && containsExactVersion.negate()
							.test(resolvedArtifact, files.values())) {
						files.put(resolvedArtifact.getArtifact()
							.getFile(), resolvedArtifact);
					}
				} catch (ArtifactResolutionException e) {
					nodesFailed++;
					logger.warn("Failed to resolve dependency {}", node.getArtifact());
				}

				if (includeTransitive) {
					processQueue.addAll(node.getChildren());
					// discoverArtifacts(files, , node.getRequestContext(),
					// combinedRepositories);
				} else {
					logger.debug("Ignoring transitive dependencies of {}", node.getDependency());
				}
				visited.add(nodeString);
			} else {
				nodesSkipped++;
			}
			node = processQueue.poll();
		}

		logger.info(
			"Resolved all Dependencies for {}. Checked: {}, Skipped(no need to revisit): {}, Failed: {}, Resulting Dependencies: {}",
			parent, nodesChecked, nodesSkipped, nodesFailed, files.size());
	}

	@SuppressWarnings("deprecation")
	private void setDependencyManagementDependencies() {
		List<Dependency> dependencies = project.getDependencies();

		// We already have a copy
		dependencies.clear();

		project.getDependencyManagement()
			.getDependencies()
			.stream()
			.forEach(dependencies::add);

		try {
			project.setDependencyArtifacts(project.createArtifacts(artifactFactory, null, null));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void resetOriginalDependencies(List<Dependency> originalDependencies) {
		project.getDependencies()
			.clear();
		project.getDependencies()
			.addAll(originalDependencies);

		try {
			project.setDependencyArtifacts(project.createArtifacts(artifactFactory, null, null));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}
