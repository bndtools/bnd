package biz.aQute.resolve.repository;

import java.io.*;
import java.util.*;

import org.osgi.resource.*;

import aQute.bnd.osgi.resource.*;
import aQute.lib.io.*;
import junit.framework.*;

public class TestWrapper extends TestCase {
	File tmp;
	
	public void setUp() {
		tmp = new File("tmp");
		IO.delete(tmp);
	}
	
	public void tearDown() {
		IO.delete(tmp);
	}
	
	
	public void testRepo() throws Exception {
		TestRepository testRepo = new TestRepository( new File("testdata/repo3"));
		
		assertEquals(4, testRepo.list(null).size());
		
		InfoRepositoryWrapper wrapper = new InfoRepositoryWrapper(testRepo, tmp);
		
		Map<Requirement,Collection<Capability>> providers = wrapper.findProviders(Arrays.asList(CapReqBuilder.createPackageRequirement("org.osgi.framework", null).buildRequirement()));
		assertNotNull(providers);
		assertEquals( 1, providers.size());
		
		
	}
}
