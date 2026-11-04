package bndtools.views.bundlegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.GraphClosures;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;

public class GraphClosuresTest {

	private static BundleNode node(String bsn) {
		return new BundleNode(bsn, "1.0.0", bsn);
	}

	private static BundleGraphModel graph(Set<BundleNode> nodes, Map<BundleNode, Set<BundleNode>> deps) {
		Set<BundleEdge> edges = new LinkedHashSet<>();
		for (Map.Entry<BundleNode, Set<BundleNode>> e : deps.entrySet()) {
			for (BundleNode to : e.getValue()) {
				edges.add(new BundleEdge(e.getKey(), to, false));
			}
		}
		return new SimpleBundleGraphModel(nodes, edges, Collections.emptyMap());
	}

	@Test
	public void emptySeedsProduceEmptyClosure() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		BundleGraphModel model = graph(nodes, Collections.emptyMap());

		Set<BundleNode> result = GraphClosures.dependencyClosure(model, Collections.emptySet());
		assertTrue(result.isEmpty());
	}

	@Test
	public void noEdgesReturnsSeedsOnly() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		BundleGraphModel model = graph(nodes, Collections.emptyMap());

		Set<BundleNode> seed = Collections.singleton(a);
		Set<BundleNode> result = GraphClosures.dependencyClosure(model, seed);
		assertEquals(Collections.singleton(a), result);
	}

	@Test
	public void directDependencyIsIncluded() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(a, Collections.singleton(b)); // a depends on b
		BundleGraphModel model = graph(nodes, deps);

		Set<BundleNode> seed = Collections.singleton(a);
		Set<BundleNode> result = GraphClosures.dependencyClosure(model, seed);

		assertTrue(result.contains(a));
		assertTrue(result.contains(b));
		assertEquals(2, result.size());
	}

	@Test
	public void transitiveDependenciesAreIncluded() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		BundleNode c = node("c");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(a, Collections.singleton(b));
		deps.put(b, Collections.singleton(c));
		BundleGraphModel model = graph(nodes, deps);

		Set<BundleNode> seed = Collections.singleton(a);
		Set<BundleNode> result = GraphClosures.dependencyClosure(model, seed);

		assertTrue(result.contains(a));
		assertTrue(result.contains(b));
		assertTrue(result.contains(c));
		assertEquals(3, result.size());
	}

	@Test
	public void cyclicDependenciesAreHandled() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		Set<BundleNode> bSet = new LinkedHashSet<>();
		bSet.add(b);
		Set<BundleNode> aSet = new LinkedHashSet<>();
		aSet.add(a);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(a, bSet);
		deps.put(b, aSet); // cyclic
		BundleGraphModel model = graph(nodes, deps);

		Set<BundleNode> seed = Collections.singleton(a);
		Set<BundleNode> result = GraphClosures.dependencyClosure(model, seed);

		assertEquals(2, result.size());
	}

	/**
	 * BundleNode identity is bsn+version only; projectName is metadata. A node loaded from a repository (projectName
	 * "") and one loaded from a workspace project (projectName "myProject") must be equal so that edges are shared
	 * across providers.
	 */
	@Test
	public void bundleNodesWithSameBsnVersionButDifferentProjectNameAreEqual() {
		BundleNode fromRepo = new BundleNode("com.example.bundle", "1.0.0", "");
		BundleNode fromProject = new BundleNode("com.example.bundle", "1.0.0", "myProject");
		assertEquals(fromRepo, fromProject, "nodes with same bsn+version must be equal regardless of projectName");
		assertEquals(fromRepo.hashCode(), fromProject.hashCode(),
			"equal nodes must have same hashCode");

		BundleNode differentVersion = new BundleNode("com.example.bundle", "2.0.0", "");
		assertNotEquals(fromRepo, differentVersion, "nodes with different version must not be equal");
	}

	/**
	 * Simulates the drop-onto-selected bug: edges are built with repo-created nodes; a project-created node with the
	 * same bsn+version (but different projectName) is used as the seed for the closure. Because identity is now
	 * bsn+version only, the closure must find the transitive dependencies.
	 */
	@Test
	public void closureFindsEdgesWhenSeedNodeHasDifferentProjectName() {
		BundleNode repoA = new BundleNode("a", "1.0.0", "");   // repo-created
		BundleNode repoB = new BundleNode("b", "1.0.0", "");   // repo-created
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(repoA);
		nodes.add(repoB);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(repoA, Collections.singleton(repoB)); // a depends on b (edge uses repo nodes)
		BundleGraphModel model = graph(nodes, deps);

		// Seed with project-created node (different projectName but same bsn+version as repoA)
		BundleNode projectA = new BundleNode("a", "1.0.0", "myProject");
		Set<BundleNode> result = GraphClosures.dependencyClosure(model, Collections.singleton(projectA));

		assertEquals(2, result.size(), "transitive dep b must be found even though seed has different projectName");
		assertTrue(result.stream().anyMatch(n -> n.bsn().equals("b")), "b must be in the closure");
	}

	@Test
	public void dependantClosureFindsWhatDependsOnSeed() {
		BundleNode a = node("a");
		BundleNode b = node("b");
		BundleNode c = node("c");
		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(b, Collections.singleton(a)); // b depends on a
		deps.put(c, Collections.singleton(a)); // c depends on a
		BundleGraphModel model = graph(nodes, deps);

		Set<BundleNode> seed = Collections.singleton(a);
		Set<BundleNode> result = GraphClosures.dependantClosure(model, seed);

		assertTrue(result.contains(a));
		assertTrue(result.contains(b));
		assertTrue(result.contains(c));
		assertEquals(3, result.size());
	}

	@Test
	public void nullContributingPackageIsNormalizedToEmptyString() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edge = new BundleEdge(a, b, false, null);
		assertNotNull(edge.contributingPackage(), "contributingPackage must never be null");
		assertEquals("", edge.contributingPackage(), "null contributingPackage should be normalized to empty string");
	}

	@Test
	public void equalsIgnoresContributingPackage() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edgeWithPkg = new BundleEdge(a, b, false, "com.example.api");
		BundleEdge edgeNoPkg = new BundleEdge(a, b, false);

		assertEquals(edgeWithPkg, edgeNoPkg,
			"Edges with same from/to/optional but different contributingPackage should be equal");
	}

	@Test
	public void hashCodeIgnoresContributingPackage() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edgeWithPkg = new BundleEdge(a, b, false, "com.example.api");
		BundleEdge edgeNoPkg = new BundleEdge(a, b, false);

		assertEquals(edgeWithPkg.hashCode(), edgeNoPkg.hashCode(),
			"Hash codes must be equal when from/to/optional are the same");
	}

	@Test
	public void edgesWithDifferentOptionalAreNotEqual() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge mandatory = new BundleEdge(a, b, false, "com.example.api");
		BundleEdge optional = new BundleEdge(a, b, true, "com.example.api");

		assertNotEquals(mandatory, optional, "Edges with different optional flag must not be equal");
	}

	@Test
	public void edgesWithDifferentFromAreNotEqual() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		BundleEdge edgeAB = new BundleEdge(a, b, false);
		BundleEdge edgeCB = new BundleEdge(c, b, false);

		assertNotEquals(edgeAB, edgeCB, "Edges with different 'from' nodes must not be equal");
	}

	@Test
	public void edgesWithDifferentToAreNotEqual() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		BundleEdge edgeAB = new BundleEdge(a, b, false);
		BundleEdge edgeAC = new BundleEdge(a, c, false);

		assertNotEquals(edgeAB, edgeAC, "Edges with different 'to' nodes must not be equal");
	}
}
