package bndtools.views.bundlegraph;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;
import bndtools.views.bundlegraph.render.EdgeFilter;
import bndtools.views.bundlegraph.render.MermaidRenderer;

public class MermaidRendererTest {

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

	private static BundleGraphModel graphWithEdges(Set<BundleNode> nodes, Set<BundleEdge> edges) {
		return new SimpleBundleGraphModel(nodes, edges, Collections.emptyMap());
	}

	@Test
	public void emptySubsetProducesMinimalGraph() {
		BundleGraphModel model = graph(Collections.emptySet(), Collections.emptyMap());
		String result = MermaidRenderer.toMermaid(model, Collections.emptySet());
		assertTrue(result.startsWith("graph LR\n"), "Should start with graph LR header");
	}

	@Test
	public void singleNodeAppearsInOutput() {
		BundleNode a = node("com.example.bundle");
		Set<BundleNode> nodes = Collections.singleton(a);
		BundleGraphModel model = graph(nodes, Collections.emptyMap());

		String result = MermaidRenderer.toMermaid(model, nodes);

		assertTrue(result.contains("com_example_bundle"), "Node id should replace dots with underscores");
		assertTrue(result.contains("com.example.bundle"), "Node label should contain original BSN");
	}

	@Test
	public void edgeAppearsWhenBothEndpointsInSubset() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(a, Collections.singleton(b)); // a depends on b

		BundleGraphModel model = graph(nodes, deps);
		String result = MermaidRenderer.toMermaid(model, nodes);

		// Edge should be: b --> a (b exports something a imports)
		assertTrue(result.contains("bundle_b --> bundle_a"), "Should have solid edge from b to a");
	}

	@Test
	public void edgeOmittedWhenEndpointNotInSubset() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> all = new LinkedHashSet<>();
		all.add(a);
		all.add(b);

		Map<BundleNode, Set<BundleNode>> deps = new HashMap<>();
		deps.put(a, Collections.singleton(b));

		BundleGraphModel model = graph(all, deps);
		// Subset contains only 'a', not 'b'
		Set<BundleNode> subset = Collections.singleton(a);
		String result = MermaidRenderer.toMermaid(model, subset);

		assertFalse(result.contains("-->") || result.contains(".->"),
			"No edges when dependency endpoint not in subset");
	}

	// ---- Tests for the styled (primary/secondary) overload ----

	@Test
	public void noClassDefsWhenAllNodesArePrimary() {
		BundleNode a = node("bundle.a");
		Set<BundleNode> nodes = Collections.singleton(a);
		BundleGraphModel model = graph(nodes, Collections.emptyMap());

		// subset == primary: no secondary nodes → no classDef emitted
		String result = MermaidRenderer.toMermaid(model, nodes, nodes);
		assertFalse(result.contains("classDef"), "No classDef when all nodes are primary");
		assertFalse(result.contains(":::"), "No class assignment when all nodes are primary");
	}

	@Test
	public void classDefsEmittedWhenMixedPrimaryAndSecondary() {
		BundleNode primary = node("bundle.primary");
		BundleNode secondary = node("bundle.secondary");

		Set<BundleNode> allNodes = new LinkedHashSet<>();
		allNodes.add(primary);
		allNodes.add(secondary);
		BundleGraphModel model = graph(allNodes, Collections.emptyMap());

		Set<BundleNode> primarySet = Collections.singleton(primary);
		String result = MermaidRenderer.toMermaid(model, allNodes, primarySet);

		assertTrue(result.contains("classDef primary"), "Primary classDef should be declared");
		assertTrue(result.contains("classDef secondary"), "Secondary classDef should be declared");
		// Node declarations look like: bundle_primary["..."]:::primary
		assertTrue(result.contains("]:::primary"), "Primary node should carry :::primary class");
		assertTrue(result.contains("]:::secondary"), "Secondary node should carry :::secondary class");
	}

	@Test
	public void noClassDefsWhenPrimarySetIsEmpty() {
		BundleNode a = node("bundle.a");
		Set<BundleNode> nodes = Collections.singleton(a);
		BundleGraphModel model = graph(nodes, Collections.emptyMap());

		// Empty primary set → nothing to distinguish, no styling
		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet());
		assertFalse(result.contains("classDef"), "No classDef when primary set is empty");
	}

	// ---- Tests for optional vs mandatory edge rendering ----

	@Test
	public void mandatoryEdgeRenderedAsSolidArrow() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		// a depends on b via a mandatory edge
		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, false));
		BundleGraphModel model = graphWithEdges(nodes, edges);

		String result = MermaidRenderer.toMermaid(model, nodes);
		assertTrue(result.contains("bundle_b --> bundle_a"), "Mandatory edge should use solid --> arrow");
		assertFalse(result.contains(".->"), "Mandatory edge should not use dotted arrow");
	}

	@Test
	public void optionalEdgeRenderedAsDottedArrow() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		// a depends on b via an all-optional edge
		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, true));
		BundleGraphModel model = graphWithEdges(nodes, edges);

		String result = MermaidRenderer.toMermaid(model, nodes);
		assertTrue(result.contains("bundle_b .-> bundle_a"), "Optional edge should use dotted .-> arrow");
		assertFalse(result.contains("-->"), "Optional edge should not use solid arrow");
	}

	@Test
	public void optionalEdgeHiddenWhenIncludeOptionalIsFalse() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		// a depends on b via an all-optional edge
		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, true));
		BundleGraphModel model = graphWithEdges(nodes, edges);

		// includeOptional=false → dotted arrow should be suppressed, and both nodes hidden (no connected edges)
		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ONLY_MANDATORY);
		assertFalse(result.contains("bundle_b .-> bundle_a"), "Optional edge should be hidden with EdgeFilter.ONLY_MANDATORY");
		assertFalse(result.contains("-->"), "No solid arrow either");
		assertFalse(result.contains("bundle_a"), "Disconnected node a should be hidden");
		assertFalse(result.contains("bundle_b"), "Disconnected node b should be hidden");
	}

	@Test
	public void mandatoryEdgeShownEvenWhenIncludeOptionalIsFalse() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);

		// a depends on b via a mandatory edge
		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, false));
		BundleGraphModel model = graphWithEdges(nodes, edges);

		// mandatory edge must still appear when ONLY_MANDATORY filter is active
		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ONLY_MANDATORY);
		assertTrue(result.contains("bundle_b --> bundle_a"), "Mandatory edge must still be shown");
	}

	@Test
	public void mixedEdgesFilteredCorrectlyWhenIncludeOptionalIsFalse() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Set<BundleEdge> edges = new LinkedHashSet<>();
		edges.add(new BundleEdge(a, b, false)); // a→b mandatory
		edges.add(new BundleEdge(a, c, true));  // a→c optional-only

		BundleGraphModel model = graphWithEdges(nodes, edges);
		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ONLY_MANDATORY);

		// mandatory edge must be present
		assertTrue(result.contains("bundle_b --> bundle_a"), "Mandatory edge should still be shown");
		// optional-only edge must be hidden
		assertFalse(result.contains("bundle_c .-> bundle_a"), "Optional-only edge should be hidden");
	}

	@Test
	public void mixedEdgesRenderedWithCorrectArrowStyles() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Set<BundleEdge> edges = new LinkedHashSet<>();
		edges.add(new BundleEdge(a, b, false)); // a→b mandatory
		edges.add(new BundleEdge(a, c, true));  // a→c optional

		BundleGraphModel model = graphWithEdges(nodes, edges);
		String result = MermaidRenderer.toMermaid(model, nodes);

		assertTrue(result.contains("bundle_b --> bundle_a"), "Mandatory edge b→a should use solid arrow");
		assertTrue(result.contains("bundle_c .-> bundle_a"), "Optional edge c→a should use dotted arrow");
	}

	@Test
	public void onlyOptionalFilterShowsOnlyOptionalEdges() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Set<BundleEdge> edges = new LinkedHashSet<>();
		edges.add(new BundleEdge(a, b, false)); // a→b mandatory
		edges.add(new BundleEdge(a, c, true));  // a→c optional-only

		BundleGraphModel model = graphWithEdges(nodes, edges);
		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ONLY_OPTIONAL);

		// optional edge must be present, mandatory edge must be hidden
		assertTrue(result.contains("bundle_c .-> bundle_a"), "Optional-only edge should be shown");
		assertFalse(result.contains("bundle_b --> bundle_a"), "Mandatory edge should be hidden");
		// node b has no active edge → must not appear in output
		assertFalse(result.contains("bundle_b["), "Disconnected node b should be hidden when only-optional filter active");
	}

	@Test
	public void nodesWithoutEdgesHiddenUnderOnlyMandatoryFilter() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c"); // isolated – no edges

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, false)); // a→b mandatory
		BundleGraphModel model = graphWithEdges(nodes, edges);

		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ONLY_MANDATORY);
		assertTrue(result.contains("bundle_b --> bundle_a"), "Mandatory edge should appear");
		assertFalse(result.contains("bundle_c"), "Isolated node c must not appear");
	}

	/**
	 * Regression: ONLY_MANDATORY with a non-empty primaryNodes set must suppress secondary-to-secondary edges. Only
	 * edges that touch at least one primary node should appear.
	 */
	@Test
	public void onlyMandatoryFilterExcludesSecondaryToSecondaryEdges() {
		// Scenario mirrors the issue report:
		// primary: assertj_core
		// mandatory edge: net_bytebuddy --> assertj_core (should be shown)
		// mandatory edge: junit_platform --> junit_jupiter (secondary↔secondary, should be hidden)
		BundleNode assertjCore = node("assertj-core");
		BundleNode netBytebuddy = node("net.bytebuddy.byte-buddy");
		BundleNode junitJupiter = node("junit-jupiter-api");
		BundleNode junitPlatform = node("junit-platform-commons");

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(assertjCore);
		nodes.add(netBytebuddy);
		nodes.add(junitJupiter);
		nodes.add(junitPlatform);

		Set<BundleEdge> edges = new LinkedHashSet<>();
		edges.add(new BundleEdge(netBytebuddy, assertjCore, false));   // mandatory: netBytebuddy depends on assertjCore
		edges.add(new BundleEdge(junitPlatform, junitJupiter, false)); // mandatory: secondary→secondary

		BundleGraphModel model = graphWithEdges(nodes, edges);
		Set<BundleNode> primarySet = Collections.singleton(assertjCore);

		String result = MermaidRenderer.toMermaid(model, nodes, primarySet, EdgeFilter.ONLY_MANDATORY);

		// The edge touching the primary node must appear
		assertTrue(result.contains("assertj_core --> net_bytebuddy_byte_buddy")
			|| result.contains("net_bytebuddy_byte_buddy --> assertj_core"),
			"Mandatory edge to primary node must be shown");
		// Secondary-to-secondary edge must NOT appear
		assertFalse(result.contains("junit_platform_commons") || result.contains("junit_jupiter_api"),
			"Secondary-to-secondary mandatory edges must be hidden when primary set is non-empty");
	}

	@Test
	public void allFilterShowsAllNodesIncludingIsolated() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c"); // isolated – no edges

		Set<BundleNode> nodes = new LinkedHashSet<>();
		nodes.add(a);
		nodes.add(b);
		nodes.add(c);

		Set<BundleEdge> edges = Collections.singleton(new BundleEdge(a, b, false));
		BundleGraphModel model = graphWithEdges(nodes, edges);

		String result = MermaidRenderer.toMermaid(model, nodes, Collections.emptySet(), EdgeFilter.ALL);
		assertTrue(result.contains("bundle_c"), "Isolated node c must still appear under ALL filter");
	}
}
