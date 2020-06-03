package aQute.bnd.maven.lib.resolve;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.lib.unmodifiable.Lists;

@ProviderType
public class DependencyResolver {

	private static final Logger					logger	= LoggerFactory.getLogger(DependencyResolver.class);

	private final boolean						includeTransitive;
	private final MavenProject					project;
	final Collection<String>					scopes;
	private final RepositorySystemSession		session;
	private final RepositorySystem				system;
	private final ProjectDependenciesResolver	resolver;
	private final PostProcessor					postProcessor;

	private Map<File, ArtifactResult>			resolvedDependencies;

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

		this(project, session, resolver, system, Lists.of("compile", "runtime"), true, new LocalPostProcessor());
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, Set<Scope> scopes) {

		this(project, session, resolver, system, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()));
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, List<String> scopes) {

		this(project, session, resolver, system, scopes, true, new LocalPostProcessor());
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, Set<Scope> scopes, boolean includeTransitive,
		PostProcessor postProcessor) {

		this(project, session, resolver, system, scopes.stream()
			.map(Scope::name)
			.collect(Collectors.toList()), includeTransitive, postProcessor);
	}

	public DependencyResolver(MavenProject project, RepositorySystemSession session,
		ProjectDependenciesResolver resolver, RepositorySystem system, List<String> scopes, boolean includeTransitive,
		PostProcessor postProcessor) {

		this.project = project;
		this.session = session;
		this.resolver = resolver;
		this.system = system;
		this.scopes = scopes;
		this.includeTransitive = includeTransitive;
		this.postProcessor = postProcessor;
	}

	public Map<File, ArtifactResult> resolve() throws MojoExecutionException {
		List<RemoteRepository> remoteRepositories = new ArrayList<>(project.getRemoteProjectRepositories());
		ArtifactRepository deployRepo = project.getDistributionManagementArtifactRepository();
		if (deployRepo != null) {
			remoteRepositories.add(RepositoryUtils.toRepo(deployRepo));
		}
		return resolve(remoteRepositories);
	}

	public Map<File, ArtifactResult> resolveAgainstRepos(Collection<ArtifactRepository> repositories)
		throws MojoExecutionException {
		List<RemoteRepository> remoteRepositories = new ArrayList<>(repositories.size());
		for (ArtifactRepository ar : repositories) {
			remoteRepositories.add(RepositoryUtils.toRepo(ar));
		}

		return resolve(remoteRepositories);
	}

	private Map<File, ArtifactResult> resolve(List<RemoteRepository> remoteRepositories) throws MojoExecutionException {
		if (resolvedDependencies != null) {
			return resolvedDependencies;
		}

		DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

		DependencyResolutionResult result;
		try {
			result = resolver.resolve(request);
		} catch (DependencyResolutionException e) {
			result = e.getResult();
			logger.warn(e.getMessage());
		}

		Map<File, ArtifactResult> dependencies = new LinkedHashMap<>();

		DependencyNode dependencyGraph = result.getDependencyGraph();

		if (dependencyGraph != null && !dependencyGraph.getChildren()
			.isEmpty()) {
			discoverArtifacts(dependencies, dependencyGraph.getChildren(), project.getArtifact()
				.getId(), remoteRepositories);
		}

		return resolvedDependencies = dependencies;
	}

	public FileSetRepository getFileSetRepository(String name, Collection<File> bundlesInputParameter,
		boolean useMavenDependencies) throws Exception {

		Collection<File> bundles = new ArrayList<>();
		if (useMavenDependencies) {
			Map<File, ArtifactResult> dependencies = resolve();

			bundles.addAll(dependencies.keySet());

			String finalName = project.getBuild()
				.getFinalName();

			Optional.ofNullable(project.getPlugin("org.apache.maven.plugins:maven-jar-plugin"))
				.map(Plugin::getExecutions)
				.orElseGet(ArrayList<PluginExecution>::new)
				.stream()
				.map(PluginExecution::getConfiguration)
				.map(Xpp3Dom.class::cast)
				.forEach(c -> readConfiguration(c, finalName, bundles));
		}

		if (bundlesInputParameter != null) {
			bundles.addAll(bundlesInputParameter);
		}

		return new ImplicitFileSetRepository(name, bundles);
	}

	private void readConfiguration(Xpp3Dom xpp3Dom, String finalName, Collection<File> bundles) {
		String classifier = Optional.ofNullable(xpp3Dom)
			.map(c -> c.getChild("classifier"))
			.map(Xpp3Dom::getValue)
			.orElse("");
		StringBuilder fileName = new StringBuilder(finalName);
		if (!classifier.isEmpty()) {
			fileName.append("-")
				.append(classifier);
		}
		fileName.append(".jar");
		File current = new File(project.getBuild()
			.getDirectory(), fileName.toString());
		if (current.exists() && !bundles.contains(current)) {
			bundles.add(current);
		}
	}

	private void discoverArtifacts(Map<File, ArtifactResult> files, List<DependencyNode> nodes, String parent,
		List<RemoteRepository> remoteRepositories) throws MojoExecutionException {

		for (DependencyNode node : nodes) {
			List<RemoteRepository> combinedRepositories = new ArrayList<>(remoteRepositories);
			combinedRepositories.addAll(node.getRepositories());

			// Ensure that the file is downloaded so we can index it
			try {
				ArtifactResult resolvedArtifact = postProcessor.postProcessResult(system.resolveArtifact(session,
					new ArtifactRequest(node.getArtifact(), combinedRepositories, parent)));
				logger.debug("Located file: {} for artifact {}", resolvedArtifact.getArtifact()
					.getFile(), resolvedArtifact);

				// Add artifact only if the scope of this artifact matches.
				if (scopes.contains(node.getDependency()
					.getScope())) {
					files.put(resolvedArtifact.getArtifact()
						.getFile(), resolvedArtifact);
				}
			} catch (ArtifactResolutionException e) {
				logger.warn("Failed to resolve dependency {}", node.getArtifact());
			}

			if (includeTransitive) {
				discoverArtifacts(files, node.getChildren(), node.getRequestContext(), combinedRepositories);
			} else {
				logger.debug("Ignoring transitive dependencies of {}", node.getDependency());
			}
		}
	}

}
