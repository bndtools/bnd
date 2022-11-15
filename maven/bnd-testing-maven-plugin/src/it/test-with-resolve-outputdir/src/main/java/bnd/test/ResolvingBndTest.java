package bnd.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.FrameworkUtil.getBundle;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ResolvingBndTest {

	@Test
	public void test() {
		assertNotNull(getBundle(ResolvingBndTest.class).getBundleContext());
	}
	
	@Test
	public void test2() {
		BundleContext ctx = getBundle(ResolvingBndTest.class).getBundleContext();
		assertNotNull(ctx);
		
		boolean found = false;
		for (Bundle bundle : ctx.getBundles()) {
			if("org.apache.felix.scr".equals(bundle.getSymbolicName())) {
				found = true;
				break;
			}
		}
		assertTrue(found);
		
	}
}
