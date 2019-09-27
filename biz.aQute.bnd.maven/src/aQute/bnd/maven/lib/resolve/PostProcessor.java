package aQute.bnd.maven.lib.resolve;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public interface PostProcessor {

	ArtifactResult postProcessResult(ArtifactResult resolvedArtifact) throws MojoExecutionException;

}
