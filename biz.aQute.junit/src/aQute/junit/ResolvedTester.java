package aQute.junit;

import java.util.*;

import org.osgi.framework.*;

import junit.framework.*;
/**
 * Verify that all bundles are resolved
 */
public class ResolvedTester extends TestCase {
	BundleContext context = FrameworkUtil.getBundle(ResolvedTester.class).getBundleContext();
	
	public void testAllResolved() {
		assertNotNull("Expected a Bundle Context", context);
		
		List<Bundle> unresolved = new ArrayList<Bundle>();
		for ( Bundle b : context.getBundles()) {
			if ( b.getState() == Bundle.INSTALLED)
				unresolved.add(b);
		}
		
		assertTrue( "Unresolved bundles: " + unresolved.toString(), unresolved.isEmpty());
	}
}
