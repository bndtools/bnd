package test;

import java.io.*;

import junit.framework.*;
import aQute.lib.deployer.obr.*;
import aQute.lib.deployer.obr.CachingURLResourceHandle.CachingMode;

public class CachingURLResourceHandleTest extends TestCase {
	
	File currentDir = new File(System.getProperty("user.dir"));
	
	public void testResolveAbsolute() throws IOException {
		CachingURLResourceHandle handle;
		
		File testFile = new File(currentDir, "bnd.bnd").getAbsoluteFile();
		String testUrl = testFile.toURI().toURL().toExternalForm();
		
		// Ignore base
		handle = new CachingURLResourceHandle(testUrl, "http://ignored", null, CachingMode.PreferCache);
		assertEquals(testUrl, handle.getResolvedUrl().toExternalForm());
		
		// Base may be null
		handle = new CachingURLResourceHandle(testUrl, null, null, CachingMode.PreferCache);
		assertEquals(testUrl, handle.getResolvedUrl().toExternalForm());
	}
	
}
