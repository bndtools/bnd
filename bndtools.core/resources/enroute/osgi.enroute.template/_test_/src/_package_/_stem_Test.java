package _package_;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 */

public class _stem_Test {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    /*
     * 
     * 
     * 
     * 
     */
    @Test
    public void test_stem_() throws Exception {
    	Assert.assertNotNull(context);
    }
}
