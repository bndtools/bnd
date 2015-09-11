package aQute.bnd.maven.indexer.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.KnownBundleAnalyzer;
import org.osgi.service.indexer.impl.RepoIndex;
import org.osgi.service.indexer.impl.URLResolver;

/**
 * Exports project dependencies to OSGi R5 index format.
 */
@Mojo(name = "index")
public class IndexerMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
	private File targetDir;

    @Parameter( property = "bnd.indexer.allowLocal", defaultValue = "false", readonly = true )
    private boolean allowLocal;

    @Parameter( property = "bnd.indexer.scopes", readonly = true, required=false )
    private List<String> scopes;
    
    @Component
    private RepositorySystem system;

    @Component
    private ProjectDependenciesResolver resolver;

    public void execute() throws MojoExecutionException, MojoFailureException {

    	if(scopes == null || scopes.isEmpty()) {
    		scopes = Arrays.asList("compile", "runtime");
    	}
    	
        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

        request.setResolutionFilter(new ScopeDependencyFilter(scopes, null));
        
        DependencyResolutionResult result;
        try {
            result = resolver.resolve(request);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Map<File, ArtifactResult> dependencies = new HashMap<>();
        
        if (result.getDependencyGraph() != null && !result.getDependencyGraph().getChildren().isEmpty()) {
            discoverArtifacts(dependencies, result.getDependencyGraph().getChildren(), project.getArtifact().getId());
        }

        Map<String, ArtifactRepository> repositories = new HashMap<>();
       
        for (ArtifactRepository artifactRepository : project.getRemoteArtifactRepositories()) {
        	repositories.put(artifactRepository.getId(), artifactRepository);
        }

        RepoIndex indexer = new RepoIndex();
        Filter filter;
        try {
            filter = FrameworkUtil.createFilter("(name=*.jar)");
        } catch (InvalidSyntaxException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        indexer.addAnalyzer(new KnownBundleAnalyzer(), filter);
        
        URLResolver resolver = new MavenURLResolver(dependencies, repositories);
        indexer.setURLResolver(resolver);
        
        Map<String, String> config = new HashMap<String, String>();
        config.put(ResourceIndexer.PRETTY, "true");

        OutputStream output;
        try {
        	targetDir.mkdirs();
            output = new FileOutputStream(new File(targetDir, "index.xml"));
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        try {
            indexer.index(dependencies.keySet(), output, config);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    class MavenURLResolver implements URLResolver {

        private final Map<File, ArtifactResult> dependencies;
		private final Map<String, ArtifactRepository> repositories;

		public MavenURLResolver(Map<File, ArtifactResult> dependencies, Map<String, ArtifactRepository> repositories) {
			this.dependencies = dependencies;
			this.repositories = repositories;
        }

		public URI resolver(File file) throws Exception {
			ArtifactResult artifact = dependencies.get(file);
			if(artifact == null) {
				throw new FileNotFoundException("The file " + file.getCanonicalPath() + " is not known to this resolver");
			}
			
			ArtifactRepository repo = repositories.get(artifact.getRepository().getId());
			if(repo == null) {
				if(allowLocal) {
					getLog().info("The Artifact " + artifact.getArtifact().toString() + 
							" could not be found in any repository, returning the local location");
					return file.toURI();
				}
				throw new FileNotFoundException("The repository " + artifact.getRepository().getId() + " is not known to this resolver");
			}
			
			return URI.create(repo.getUrl() + "/" + 
					repo.getLayout().pathOf(RepositoryUtils.toArtifact(artifact.getArtifact())));
        }
    }

    private void discoverArtifacts(Map<File, ArtifactResult> files, List<DependencyNode> nodes, String parent) throws MojoExecutionException {
        for ( DependencyNode node : nodes) {
            //Ensure that the file is downloaded so we can index it
            try {
				ArtifactResult resolvedArtifact = system.resolveArtifact(session, new ArtifactRequest(node.getArtifact(), 
						project.getRemoteProjectRepositories(), parent));
				files.put(resolvedArtifact.getArtifact().getFile(), resolvedArtifact);
			} catch (ArtifactResolutionException e) {
				throw new MojoExecutionException("Failed to resolve the dependency " + node.getArtifact().toString(), e);
			}
            
            discoverArtifacts(files, node.getChildren(), node.getRequestContext());
        }
    }

}
