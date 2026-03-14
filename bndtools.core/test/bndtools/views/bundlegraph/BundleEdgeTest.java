package bndtools.views.bundlegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleNode;

/**
 * Tests for {@link BundleEdge}, focusing on the {@code contributingPackage} field and equals/hashCode contract.
 */
public class BundleEdgeTest {

	private static BundleNode node(String bsn) {
		return new BundleNode(bsn, "1.0.0", bsn);
	}

	@Test
	public void threeArgConstructorSetsEmptyContributingPackage() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edge = new BundleEdge(a, b, false);
		assertEquals("", edge.contributingPackage(),
			"Three-arg constructor should set contributingPackage to empty string");
	}

	@Test
	public void fourArgConstructorSetsContributingPackage() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edge = new BundleEdge(a, b, false, "com.example.api");
		assertEquals("com.example.api", edge.contributingPackage(),
			"Four-arg constructor should record the given contributing package");
	}

	@Test
	public void nullContributingPackageIsNormalizedToEmptyString() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge edge = new BundleEdge(a, b, false, null);
		assertNotNull(edge.contributingPackage(), "contributingPackage must never be null");
		assertEquals("", edge.contributingPackage(),
			"null contributingPackage should be normalized to empty string");
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

		assertNotEquals(mandatory, optional,
			"Edges with different optional flag must not be equal");
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

	@Test
	public void optionalFlagIsRecorded() {
		BundleNode a = node("bundle.a");
		BundleNode b = node("bundle.b");

		BundleEdge mandatory = new BundleEdge(a, b, false);
		BundleEdge optional = new BundleEdge(a, b, true);

		assertFalse(mandatory.optional(), "Mandatory edge should report optional=false");
		assertTrue(optional.optional(), "Optional edge should report optional=true");
	}
}
