package bndtools.views.bundlegraph.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper for computing transitive closures over a {@link BundleGraphModel}.
 */
public final class GraphClosures {

	private GraphClosures() {}

	/**
	 * Returns the transitive closure of all dependencies reachable from the given seed nodes (including the seeds
	 * themselves).
	 */
	public static Set<BundleNode> dependencyClosure(BundleGraphModel model, Set<BundleNode> seeds) {
		return breadthFirstSearchClosure(seeds, n -> model.dependenciesOf(n));
	}

	/**
	 * Returns the transitive closure of all dependants that reach any of the seed nodes (including the seeds
	 * themselves).
	 */
	public static Set<BundleNode> dependantClosure(BundleGraphModel model, Set<BundleNode> seeds) {
		return breadthFirstSearchClosure(seeds, n -> model.dependantsOf(n));
	}

	private static Set<BundleNode> breadthFirstSearchClosure(Set<BundleNode> seeds,
		java.util.function.Function<BundleNode, Set<BundleNode>> neighbours) {
		Set<BundleNode> visited = new LinkedHashSet<>(seeds);
		java.util.Queue<BundleNode> queue = new java.util.ArrayDeque<>(seeds);
		while (!queue.isEmpty()) {
			BundleNode current = queue.poll();
			for (BundleNode neighbour : neighbours.apply(current)) {
				if (visited.add(neighbour)) {
					queue.add(neighbour);
				}
			}
		}
		return visited;
	}
}
