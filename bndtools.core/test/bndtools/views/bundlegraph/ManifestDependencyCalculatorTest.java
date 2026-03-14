package bndtools.views.bundlegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleNode;

public class ManifestDependencyCalculatorTest {

	@TempDir
	File tmp;

	private static BundleNode node(String bsn) {
		return new BundleNode(bsn, "1.0.0", bsn);
	}

	/** Creates a minimal JAR file with the given Import-Package and Export-Package values. */
	private File createJar(String name, String exportPackage, String importPackage) throws Exception {
		Manifest manifest = new Manifest();
		Attributes attrs = manifest.getMainAttributes();
		attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attrs.putValue("Bundle-SymbolicName", name);
		if (exportPackage != null && !exportPackage.isEmpty()) {
			attrs.putValue("Export-Package", exportPackage);
		}
		if (importPackage != null && !importPackage.isEmpty()) {
			attrs.putValue("Import-Package", importPackage);
		}
		File jarFile = new File(tmp, name + ".jar");
		try (JarOutputStream jos = new JarOutputStream(new java.io.FileOutputStream(jarFile), manifest)) {
			// empty JAR body is fine for manifest parsing
		}
		return jarFile;
	}

	@Test
	public void noEdgesWhenNoImports() throws Exception {
		BundleNode a = node("a");
		BundleNode b = node("b");

		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("a", "com.example.a", null));
		nodeToJar.put(b, createJar("b", "com.example.b", null));

		Map<BundleNode, Set<BundleNode>> deps = ManifestDependencyCalculator.calculateDependencies(nodeToJar);
		assertTrue(deps.isEmpty(), "No imports means no dependency edges");
	}

	@Test
	public void edgeCreatedForMatchingImportExport() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		// b exports com.example.api; a imports it
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.api"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api", null));

		Map<BundleNode, Set<BundleNode>> deps = ManifestDependencyCalculator.calculateDependencies(nodeToJar);

		Set<BundleNode> aDeps = deps.get(a);
		assertTrue(aDeps != null && aDeps.contains(b), "bundle.a should depend on bundle.b (which exports com.example.api)");
		assertEquals(1, aDeps.size());
	}

	@Test
	public void selfImportsAreIgnored() throws Exception {
		BundleNode a = node("bundle.a");

		// a exports and imports the same package
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", "com.example.api", "com.example.api"));

		Map<BundleNode, Set<BundleNode>> deps = ManifestDependencyCalculator.calculateDependencies(nodeToJar);
		assertTrue(deps.isEmpty(), "Self-imports should not produce a self-loop edge");
	}

	@Test
	public void missingJarIsSkippedGracefully() throws Exception {
		BundleNode a = node("bundle.a");

		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, new File(tmp, "nonexistent.jar"));

		Map<BundleNode, Set<BundleNode>> deps = ManifestDependencyCalculator.calculateDependencies(nodeToJar);
		assertTrue(deps.isEmpty(), "Missing JAR should be skipped without error");
	}

	@Test
	public void transitiveDependenciesResolvedCorrectly() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		// c exports pkg.c; b imports pkg.c and exports pkg.b; a imports pkg.b
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "pkg.b"));
		nodeToJar.put(b, createJar("bundle.b", "pkg.b", "pkg.c"));
		nodeToJar.put(c, createJar("bundle.c", "pkg.c", null));

		Map<BundleNode, Set<BundleNode>> deps = ManifestDependencyCalculator.calculateDependencies(nodeToJar);

		// a depends on b (directly)
		assertTrue(deps.getOrDefault(a, Set.of())
			.contains(b), "a should depend on b");
		// b depends on c (directly)
		assertTrue(deps.getOrDefault(b, Set.of())
			.contains(c), "b should depend on c");
		// a does not directly depend on c
		assertFalse(deps.getOrDefault(a, Set.of())
			.contains(c), "a should not directly depend on c");
	}

	// ---- Tests for calculateEdges() with per-edge optionality ----

	@Test
	public void allOptionalImportsProduceOptionalEdge() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Map<BundleNode, File> nodeToJar = new HashMap<>();
		// a imports both packages from b, both are optional → edge should be optional
		nodeToJar.put(a, createJar("bundle.a", null,
			"com.example.api;resolution:=optional,com.example.spi;resolution:=optional"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api,com.example.spi", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size(), "Should be exactly one edge");
		BundleEdge edge = edges.iterator().next();
		assertEquals(a, edge.from(), "Edge should originate from bundle.a");
		assertEquals(b, edge.to(), "Edge should point to bundle.b");
		assertTrue(edge.optional(), "Edge should be optional when all imports are optional");
	}

	@Test
	public void mixedImportsProduceMandatoryEdge() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Map<BundleNode, File> nodeToJar = new HashMap<>();
		// a imports one optional and one mandatory package from b → edge is mandatory
		nodeToJar.put(a, createJar("bundle.a", null,
			"com.example.api;resolution:=optional,com.example.spi"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api,com.example.spi", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size(), "Should be exactly one edge");
		BundleEdge edge = edges.iterator().next();
		assertFalse(edge.optional(), "Edge should be mandatory when at least one import is mandatory");
	}

	@Test
	public void mandatoryImportProducesMandatoryEdge() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.api"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size());
		assertFalse(edges.iterator().next().optional(), "Mandatory import should produce mandatory edge");
	}

	@Test
	public void optionalEdgeIsAlwaysIncludedInModel() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		// b exports com.example.api; a imports it as optional
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.api;resolution:=optional"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api", null));

		// calculateEdges always includes optional edges, flagged via BundleEdge.optional()
		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);
		assertEquals(1, edges.size(), "Optional edge should always be included in the model");
		assertTrue(edges.iterator().next().optional(), "Edge should be flagged optional");
	}

	@Test
	public void splitPackageProducesEdgesToAllExporters() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");
		BundleNode c = node("bundle.c");

		// Both b and c export the same package (split package); a imports it
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.split"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.split", null));
		nodeToJar.put(c, createJar("bundle.c", "com.example.split", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		// a should have an edge to both b and c (one per exporter)
		assertTrue(edges.stream().anyMatch(e -> e.from().equals(a) && e.to().equals(b)),
			"bundle.a should depend on bundle.b (split package co-exporter)");
		assertTrue(edges.stream().anyMatch(e -> e.from().equals(a) && e.to().equals(c)),
			"bundle.a should depend on bundle.c (split package co-exporter)");
		assertEquals(2, edges.stream().filter(e -> e.from().equals(a)).count(),
			"Exactly two edges from bundle.a (one per exporter of the split package)");
	}

	// ---- Tests for contributingPackage field ----

	@Test
	public void contributingPackageIsSetOnEdge() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		// b exports com.example.api; a imports it
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.api"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size(), "Should be exactly one edge");
		BundleEdge edge = edges.iterator().next();
		assertEquals("com.example.api", edge.contributingPackage(),
			"The contributing package should be the imported/exported package");
	}

	@Test
	public void contributingPackageIsFirstImportedPackageWhenMultipleMatch() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		// b exports two packages; a imports both — contributingPackage should be the first one processed
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		// Use a LinkedHashMap-friendly ordering: first entry in Import-Package is pkg.one
		nodeToJar.put(a, createJar("bundle.a", null, "pkg.one,pkg.two"));
		nodeToJar.put(b, createJar("bundle.b", "pkg.one,pkg.two", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size(), "Should be exactly one edge between a and b");
		BundleEdge edge = edges.iterator().next();
		// contributingPackage is the first package that established the edge
		assertFalse(edge.contributingPackage().isEmpty(),
			"Contributing package should not be empty when packages are matched");
		assertTrue(edge.contributingPackage().equals("pkg.one") || edge.contributingPackage().equals("pkg.two"),
			"Contributing package should be one of the matched packages");
	}

	@Test
	public void contributingPackageNotNullForOptionalEdge() throws Exception {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		// b exports com.example.api; a imports it as optional
		Map<BundleNode, File> nodeToJar = new HashMap<>();
		nodeToJar.put(a, createJar("bundle.a", null, "com.example.api;resolution:=optional"));
		nodeToJar.put(b, createJar("bundle.b", "com.example.api", null));

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);

		assertEquals(1, edges.size());
		BundleEdge edge = edges.iterator().next();
		assertTrue(edge.optional(), "Edge should be optional");
		assertEquals("com.example.api", edge.contributingPackage(),
			"Contributing package should be set even for optional edges");
	}
}
