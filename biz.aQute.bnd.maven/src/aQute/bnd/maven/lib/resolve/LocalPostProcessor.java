package aQute.bnd.maven.lib.resolve;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class LocalPostProcessor implements PostProcessor {

	@Override
	public ArtifactResult postProcessResult(ArtifactResult resolvedArtifact) throws MojoExecutionException {
		return resolvedArtifact;
	}

}
