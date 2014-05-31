package _package_;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
    
    
    @BeforeClass
    public static void beforeClass() {
    	System.out.println("Before class");
    }

    @AfterClass
    public static void afterClass() {
    	System.out.println("After class");
    }

    @Before
    public static void before() {
    	System.out.println("Before class");
    }

    @After
    public static void after() {
    	System.out.println("After class");
    }

    @Test
    public void testExample() throws Exception {
    	Assert.assertNotNull(context);
    }
}
