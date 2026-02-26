package bndtools.views.bundlegraph.model;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link BundleGraphModel} implementation backed by explicit edge maps.
 */
public class SimpleBundleGraphModel implements BundleGraphModel {

	private final Set<BundleNode>					nodes;
	private final Map<BundleNode, Set<BundleNode>>	dependencies;
	private final Map<BundleNode, Set<BundleNode>>	dependants;
	private final Set<BundleEdge>					edgeSet;
	private final Map<BundleNode, File>				jarMap;

	/**
	 * Constructs a model from a node set, an explicit edge set, and a map from each node to its JAR file on disk. The
	 * optional flag on each {@link BundleEdge} is preserved and returned by {@link #edges()}. The {@code nodeToJar}
	 * map is stored so that universes can be merged and dependency edges recomputed over the full combined JAR set.
	 *
	 * @param nodes all nodes in the universe
	 * @param edges directed dependency edges (may carry optionality information)
	 * @param nodeToJar map from node to its JAR file (may be empty for test / synthetic models)
	 */
	public SimpleBundleGraphModel(Set<BundleNode> nodes, Set<BundleEdge> edges, Map<BundleNode, File> nodeToJar) {
		this.nodes = Collections.unmodifiableSet(new LinkedHashSet<>(nodes));
		this.edgeSet = Collections.unmodifiableSet(new LinkedHashSet<>(edges));
		this.jarMap = Collections.unmodifiableMap(new LinkedHashMap<>(nodeToJar));
		Map<BundleNode, Set<BundleNode>> deps = new LinkedHashMap<>();
		Map<BundleNode, Set<BundleNode>> revDeps = new LinkedHashMap<>();
		for (BundleNode node : nodes) {
			deps.put(node, new LinkedHashSet<>());
			revDeps.put(node, new LinkedHashSet<>());
		}
		for (BundleEdge edge : edges) {
			deps.computeIfAbsent(edge.from(), k -> new LinkedHashSet<>())
				.add(edge.to());
			revDeps.computeIfAbsent(edge.to(), k -> new LinkedHashSet<>())
				.add(edge.from());
		}
		this.dependencies = Collections.unmodifiableMap(deps);
		this.dependants = Collections.unmodifiableMap(revDeps);
	}

	@Override
	public Set<BundleNode> nodes() {
		return nodes;
	}

	@Override
	public Set<BundleNode> dependenciesOf(BundleNode node) {
		return dependencies.getOrDefault(node, Collections.emptySet());
	}

	@Override
	public Set<BundleNode> dependantsOf(BundleNode node) {
		return dependants.getOrDefault(node, Collections.emptySet());
	}

	@Override
	public Set<BundleEdge> edges() {
		return edgeSet;
	}

	@Override
	public Map<BundleNode, File> nodeToJar() {
		return jarMap;
	}
}
