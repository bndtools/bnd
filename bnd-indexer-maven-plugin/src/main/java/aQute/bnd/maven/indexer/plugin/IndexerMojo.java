package aQute.bnd.maven.indexer.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
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

    @Component
    private RepositorySystem system;

    @Component
    ProjectDependenciesResolver resolver;

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println(resolver.getClass());
        System.out.println("project.getRemoteArtifactRepositories()");
        System.out.println(project.getRemoteArtifactRepositories());
        System.out.println("project.getRemoteProjectRepositories()");
        System.out.println(project.getRemoteProjectRepositories());
        System.out.println("project.getRepositories()");
        System.out.println(project.getRepositories());

        DependencyResolutionRequest request = new DefaultDependencyResolutionRequest(project, session);

        DependencyResolutionResult result;
        try {
            result = resolver.resolve(request);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if (result.getDependencyGraph() != null && !result.getDependencyGraph().getChildren().isEmpty()) {
            dumpArtifacts(result.getDependencyGraph().getChildren());
            RepositoryUtils.toArtifacts(artifacts, result.getDependencyGraph().getChildren(),
                    Collections.singletonList(project.getArtifact().getId()), null);
        }

        RepoIndex indexer = new RepoIndex();
        Filter filter;
        try {
            filter = FrameworkUtil.createFilter("(name=*.jar)");
        } catch (InvalidSyntaxException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        indexer.addAnalyzer(new KnownBundleAnalyzer(), filter);
        URLResolver resolver = new MavenURLResolver();
        indexer.setURLResolver(resolver);
        Map<String, String> config = new HashMap<String, String>();
        config.put(ResourceIndexer.PRETTY, "true");
        Set<File> inputs = new HashSet<File>();
        File outputPath = project.getBasedir();
        for (Artifact artifact : artifacts) {
            try {
                List<String> downloadedRepositories = parseRemoteRepositores(artifact.getFile());
                System.out.println("found jar file " + artifact.getFile().getName() + " in " + downloadedRepositories);
                constructRemoteUrls(artifact, downloadedRepositories, project.getRemoteArtifactRepositories());
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            inputs.add(artifact.getFile());
        }
//        for (Artifact artifact : artifacts) {
//            for (ArtifactRepository testRepo : project.getRemoteArtifactRepositories()) {
//                System.out.println("testing " + artifact.getId() + " against " + testRepo.getId());
//                System.out.println("-->" + artifact.getRepository());
//            };
//        }

        OutputStream output;
        try {
            output = new FileOutputStream(new File(outputPath, "index.xml"));
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        try {
            indexer.index(inputs, output, config);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    private List<String> parseRemoteRepositores(File file) throws IOException {
        List<String > repositories = new ArrayList<String>();
        File path = file.getParentFile();
        String name = file.getName();
        File metadata = new File(path, "_remote.repositories");
        if (!metadata.exists()) {
            metadata = new File(path, "_maven.repositories");
        }
        if (metadata.exists()) {
            String line;
            try(BufferedReader reader = new BufferedReader(new FileReader(metadata))) {
	            while ((line = reader.readLine()) != null) {
	                if (line.startsWith("#")) continue;
	                if (line.contains(name)) {
	                    String repository = line.substring(line.indexOf(">") + 1, line.indexOf("="));
	                    repositories.add(repository);
	                }
	            }
            }
        }
        return repositories;
    }
    private void constructRemoteUrls(Artifact artifact, List<String> downloadedRepositories, List<ArtifactRepository> remoteArtifactRepositories) {
        for (ArtifactRepository artifactRepository : remoteArtifactRepositories) {
            System.out.println("testing " + artifactRepository.getId());
            if (downloadedRepositories.contains(artifactRepository.getId())) {
                System.out.println(artifactRepository.getUrl() + "/" + artifactRepository.getLayout().pathOf(artifact));
            }
        }
    }

    static class MavenURLResolver implements URLResolver {

        public URI resolver(File file) throws Exception {
            return file.toURI();
        }
    }

    private static void dumpArtifacts(List<DependencyNode> nodes) {
        for ( DependencyNode node : nodes) {
            System.out.println(node.getArtifact().getArtifactId() + " repo " + node.getRepositories());
            dumpArtifacts(node.getChildren());
        }
    }

}
