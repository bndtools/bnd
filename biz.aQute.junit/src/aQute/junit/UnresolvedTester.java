package aQute.junit;

import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

/**
 * Verify that all bundles are resolved
 */
public class UnresolvedTester extends TestCase {
	BundleContext	context;

	public void setBundleContext(BundleContext context) {
		this.context = context;
		System.out.println("got context " + context);
	}

	@SuppressWarnings("deprecation")
	public void testAllResolved() {
		assertNotNull("Expected a Bundle Context", context);
		
		List<Bundle> unresolved = new ArrayList<Bundle>();
		for (Bundle b : context.getBundles()) {
			if (b.getState() == Bundle.INSTALLED) {
				try {
					b.start();
				} catch( BundleException e) {
					System.err.println(e.getMessage());
					unresolved.add(b);
				}
			}
		}
		assertTrue("Unresolved bundles: " + unresolved.toString(), unresolved.isEmpty());
	}
}
