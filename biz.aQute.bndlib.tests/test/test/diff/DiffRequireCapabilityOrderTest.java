package test.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;

/**
 * Demonstrates that Require-Capability is compared as an opaque string,
 * so different attribute/directive insertion orders produce a false positive
 * even though Jar.writeManifest() normalises the order.
 */
class DiffRequireCapabilityOrderTest {

	/**
	 * Test for bug reported in #7377
	 */
    @Test
    void differentInsertionOrderCausesFalsePositive() throws Exception {
        // --- 1. Build two logically identical headers with different insertion orders

		// Order A: cardinality -> effective -> filter -> resolution (typical
		// post-#6798)
        Parameters reqA = new Parameters();
        Attrs a = new Attrs();
        a.put("cardinality:", "multiple");
        a.put("effective:", "active");
        a.put("filter:", "(objectClass=com.example.Foo)");
        a.put("resolution:", "optional");
        reqA.put("osgi.service", a);

		// Order B: filter -> effective -> resolution -> cardinality (typical
		// pre-#6798 / live Analyzer)
        Parameters reqB = new Parameters();
        Attrs b = new Attrs();
        b.put("filter:", "(objectClass=com.example.Foo)");
        b.put("effective:", "active");
        b.put("resolution:", "optional");
        b.put("cardinality:", "multiple");
        reqB.put("osgi.service", b);

        String headerA = reqA.toString();
        String headerB = reqB.toString();

        // The raw strings differ
        assertNotEquals(headerA, headerB, "insertion order must produce different strings");

		// --- 2. Put them into two Jars (in-memory manifests - NOT yet cleaned)

        Jar jarA = new Jar("a");
        Manifest manA = new Manifest();
        manA.getMainAttributes().putValue("Manifest-Version", "1.0");
        manA.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "test.bundle");
        manA.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.0");
        manA.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY, headerA);
        jarA.setManifest(manA);

        Jar jarB = new Jar("b");
        Manifest manB = new Manifest();
        manB.getMainAttributes().putValue("Manifest-Version", "1.0");
        manB.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, "test.bundle");
        manB.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1.0.0");
        manB.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY, headerB);
        jarB.setManifest(manB);

        // --- 3. The Differ sees a difference (the bug)

        DiffPluginImpl differ = new DiffPluginImpl();
        Tree treeA = differ.tree(jarA);
        Tree treeB = differ.tree(jarB);

        Diff diff = treeA.diff(treeB);
        Diff manifestDiff = diff.get("<manifest>");

        // Optional: print what the Differ actually compared
        System.out.println("Differ saw:");
        manifestDiff.getChildren().forEach(c -> {
				if (c.getName()
					.startsWith(Constants.REQUIRE_CAPABILITY)) {
					System.out.println("  " + c.getDelta() + "  " + c.getName());
				}
        });

		assertEquals(Delta.UNCHANGED, manifestDiff.getDelta(),
			"Require-Capability with different attribute order must be treated as equal");

        // --- 4. After writing, the manifests become identical (clean() works)

        ByteArrayOutputStream outA = new ByteArrayOutputStream();
        Jar.writeManifest(manA, outA);
        String writtenA = outA.toString("UTF-8");

        ByteArrayOutputStream outB = new ByteArrayOutputStream();
        Jar.writeManifest(manB, outB);
        String writtenB = outB.toString("UTF-8");

        // Extract just the Require-Capability line for a clean assertion
        String reqLineA = extractHeader(writtenA, Constants.REQUIRE_CAPABILITY);
        String reqLineB = extractHeader(writtenB, Constants.REQUIRE_CAPABILITY);

        assertEquals(reqLineA, reqLineB,
                "After clean()/reorderClause() the written headers must be identical");
    }

    private static String extractHeader(String manifest, String name) {
        for (String line : manifest.split("\r?\n")) {
            if (line.startsWith(name + ":")) {
                return line;
            }
        }
        return null;
    }
}