package test;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;

import org.osgi.framework.*;

import aQute.launcher.minifw.*;

public class MiniFrameworkTest extends TestCase {
	
	public void testSimple() throws Exception {
		Properties properties = new Properties();
		MiniFramework	framework = new MiniFramework(properties);
		URL url = new File("test/test/demo.jar").toURI().toURL();
		
		url.openStream().close();
		framework.init();
		
		Bundle b = framework.installBundle("reference:" + url.toExternalForm());
		assertNotNull(b);
		
		Bundle [] bundles = framework.getBundles();
		assertNotNull(bundles);
		assertEquals( 2, bundles.length);
		
		Class<?> c = b.loadClass("test.TestActivator");
		assertNotNull(c);
		
	}
}
