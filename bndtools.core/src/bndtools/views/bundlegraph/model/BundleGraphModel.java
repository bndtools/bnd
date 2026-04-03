package bndtools.views.bundlegraph.model;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Model interface for querying bundle graph nodes and their dependency relationships.
 */
public interface BundleGraphModel {

	/**
	 * Returns all known bundle nodes in this model's universe.
	 */
	Set<BundleNode> nodes();

	/**
	 * Returns the direct dependencies of the given node (i.e., bundles that {@code node} depends on).
	 */
	Set<BundleNode> dependenciesOf(BundleNode node);

	/**
	 * Returns the direct dependants of the given node (i.e., bundles that depend on {@code node}).
	 */
	Set<BundleNode> dependantsOf(BundleNode node);

	/**
	 * Returns all directed dependency edges in this model.
	 * <p>
	 * The default implementation derives edges from {@link #dependenciesOf(BundleNode)}, treating all edges as
	 * mandatory. Implementations that track {@link BundleEdge#optional() optional} edges should override this method.
	 */
	default Set<BundleEdge> edges() {
		Set<BundleEdge> result = new LinkedHashSet<>();
		for (BundleNode n : nodes()) {
			for (BundleNode dep : dependenciesOf(n)) {
				result.add(new BundleEdge(n, dep, false));
			}
		}
		return result;
	}

	/**
	 * Returns the map from each {@link BundleNode} to its JAR {@link File} on disk. This is used to recompute
	 * cross-provider dependency edges when universes are merged. The default implementation returns an empty map.
	 */
	default Map<BundleNode, File> nodeToJar() {
		return Collections.emptyMap();
	}
}
