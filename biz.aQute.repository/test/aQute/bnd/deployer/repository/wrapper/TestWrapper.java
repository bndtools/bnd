package aQute.bnd.deployer.repository.wrapper;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.resource.*;

import aQute.bnd.jpm.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.lib.utf8properties.*;
import aQute.libg.map.*;

public class TestWrapper extends TestCase {
	File	tmp	= new File("tmp");

	public void setUp() throws Exception {
		tmp.mkdirs();
		IO.delete(tmp);
		tmp.mkdirs();

	}

	public void tearDown() throws Exception {
		IO.delete(tmp);
	}

	public void testbasic() throws Exception {
		Repository repo = new Repository();
		repo.setProperties(MAP.$("location", tmp.getAbsolutePath()).$("index", "testdata/ws/cnf/jpm4j.json"));
		assertNotNull(repo.get("biz.aQute.jpm.daemon", new Version("1.1.0"), null));

		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		Requirement cr = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)")
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> result = iw.findProviders(Arrays.asList(cr));
		assertNotNull(result);
		assertEquals(1, result.size());

		iw.close();

		cr = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)")
				.buildSyntheticRequirement();
		iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		result = iw.findProviders(Arrays.asList(cr));
		assertNotNull(result);
		assertEquals(1, result.size());

		iw.close();
	}

	/**
	 * Test the augments facility. This allows us to add caps/reqs to bundles
	 * with a file from the workspace.
	 */

	public void testAugment() throws Exception {
		Repository repo = new Repository();
		repo.setProperties(MAP.$("location", tmp.getAbsolutePath()).$("index", "testdata/ws/cnf/jpm4j.json"));

		assertNotNull(repo.get("biz.aQute.jpm.daemon", new Version("1.1.0"), null));

		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		Properties augments = new UTF8Properties();
		augments.load(new StringReader("biz.aQute.jpm.daemon: cap=test;test=1\n"));
		iw.addAugment(augments);

		
		//
		// Get the test and identity capability
		//
		
		Requirement testreq = new CapReqBuilder("test").filter("(test=1)").buildSyntheticRequirement();

		Requirement identity = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)")
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> result = iw.findProviders(Arrays.asList(testreq, identity));

		assertNotNull(result);
		assertEquals(2, result.size());

		//
		// Test if they come from the same resource
		//
		
		Capability testcap = result.get(testreq).iterator().next();
		Capability identitycap = result.get(identity).iterator().next();
		assertNotNull(testcap);
		assertNotNull(identitycap);
		assertEquals(testcap.getResource(), identitycap.getResource());

		iw.close();
	}

}
