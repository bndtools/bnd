package aQute.bnd.build.api;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;

/**
 * A snapshot of the build information
 */
@ProviderType
public interface BuildInfo {
	/**
	 * The associated project. Not this is a snapshot so the project might have
	 * moved on.
	 */
	Project getProject();

	/**
	 * Get the build artifacts
	 *
	 */
	List<ArtifactInfo> getArtifactInfos();
}
