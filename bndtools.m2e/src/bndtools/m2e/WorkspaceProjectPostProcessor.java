package bndtools.m2e;

import java.io.File;
import java.nio.file.Paths;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.resolve.PostProcessor;

class WorkspaceProjectPostProcessor implements MavenRunListenerHelper, PostProcessor {

	private static final Logger		logger	= LoggerFactory.getLogger(WorkspaceProjectPostProcessor.class);

	private final IProgressMonitor	monitor;

	WorkspaceProjectPostProcessor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public ArtifactResult postProcessResult(ArtifactResult resolvedArtifact) throws MojoExecutionException {
		Artifact artifact = resolvedArtifact.getArtifact();
		IMavenProjectFacade projectFacade = mavenProjectRegistry.getMavenProject(artifact.getGroupId(),
			artifact.getArtifactId(), artifact.getVersion());
		if (projectFacade != null) {
			try {
				MavenProject mavenProject = projectFacade.getMavenProject(monitor);
				Build build = mavenProject.getBuild();
				File file = Paths.get(build.getDirectory(), build.getFinalName()
					.concat(".")
					.concat(artifact.getExtension()))
					.toFile();
				resolvedArtifact = new ArtifactResult(resolvedArtifact.getRequest());
				resolvedArtifact.setArtifact(
					new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
						artifact.getExtension(), artifact.getVersion(), artifact.getProperties(), file));
			} catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not obtain project artifact for {} due to: {}", resolvedArtifact,
						e.getMessage());
				}
			}
		}
		return resolvedArtifact;
	}

}
