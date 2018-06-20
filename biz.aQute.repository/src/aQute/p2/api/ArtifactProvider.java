package aQute.p2.api;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provide P2 artifacts
 */
public interface ArtifactProvider {
	/**
	 * Answer bundles and features
	 */
	List<Artifact> getAllArtifacts() throws Exception;

	default List<Artifact> getBundles() throws Exception {
		return getAllArtifacts().stream()
			.filter(a -> a.classifier == Classifier.BUNDLE)
			.collect(Collectors.toList());
	}

	default List<Artifact> getFeatures() throws Exception {
		return getAllArtifacts().stream()
			.filter(a -> a.classifier == Classifier.FEATURE)
			.collect(Collectors.toList());
	}

}
