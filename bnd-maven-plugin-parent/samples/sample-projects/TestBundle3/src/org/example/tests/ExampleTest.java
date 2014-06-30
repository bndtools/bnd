package org.example.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class ExampleTest {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @Test
    public void testExample() throws Exception {
    	Bundle[] bundles = context.getBundles();
    	System.out.println("Available bundles: " + bundles);
    	assertTrue("There should be more than 1 bundle", bundles.length > 1);
    }
}
