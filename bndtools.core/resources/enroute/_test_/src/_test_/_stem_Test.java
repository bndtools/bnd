package _test_;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class _stem_Test extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    public void testExample() throws Exception {
    	assertNotNull(context);
    }
}
