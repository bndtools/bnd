package org.example.tests;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class ExampleTest extends TestCase {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    public void testExample() throws Exception {
    	Bundle[] bundles = context.getBundles();
    	System.out.println("Available bundles: " + bundles);
    	assertTrue("There should be more than 1 bundle", bundles.length > 1);
    }
}
