package aQute.bnd.deployer.repository.wrapper;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.resource.*;

import aQute.bnd.build.*;
import aQute.bnd.jpm.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.lib.utf8properties.*;
import aQute.libg.map.*;

public class TestWrapper extends TestCase {
	File	tmp	= new File("tmp");

	public void setUp() throws Exception {
		System.setProperty("jpm4j.in.test", "true");
		tmp.mkdirs();
		IO.delete(tmp);
		tmp.mkdirs();

	}

	public void tearDown() throws Exception {
		IO.delete(tmp);
	}

	/**
	 * Could not find an Import-Service capability // This turned out to be
	 * caused by a toLowerCase in the filter :-(
	 */

	public void testImportService() throws Exception {
		InfoRepositoryWrapper iw = getRepo();

		Requirement cr = new CapReqBuilder("osgi.service").filter("(objectClass=org.slf4j.Logger)")
				.addDirective("effective", "active").buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> provider = iw.findProviders(Collections.singleton(cr));
		assertNotNull(provider);
		assertEquals(1, provider.size());
		Capability cap = provider.values().iterator().next().iterator().next();
		Resource resource = cap.getResource();
		assertNotNull(resource);
		List<Capability> capabilities = resource.getCapabilities("osgi.identity");
		assertEquals(1, capabilities.size());
		Capability identity = capabilities.iterator().next();
		assertEquals("osgi.logger.provider", identity.getAttributes().get("osgi.identity"));
	}

	private InfoRepositoryWrapper getRepo() throws Exception {
		Repository repo = new Repository();
		repo.setProperties(MAP.$("location", tmp.getAbsolutePath()).$("index", "testdata/ws/cnf/jpm4j.json"));
		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));
		return iw;
	}

	/**
	 * Attributes do not properly encode lists (they are encoded as "[a,b]",
	 * e.g. there is a toString somewhere // This turned out to be caused by the
	 * PersistentResource.getAttr conversion/type guessing. We always turned an
	 * object into a string; so lists became strings.
	 */
	public void testListsBecomeStrings() throws Exception {
		InfoRepositoryWrapper iw = getRepo();

		Requirement cr = new CapReqBuilder("osgi.service").filter("(objectClass=osgi.enroute.logger.api.LoggerAdmin)")
				.addDirective("effective", "active").buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> provider = iw.findProviders(Collections.singleton(cr));
		assertEquals(1, provider.size());
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

	/**
	 * Test the augments facility. This allows us to add caps/reqs to bundles
	 * with a file from the workspace.
	 */

	public void testAugment2() throws Exception {
		
		File cache = new File("cache");
		IO.deleteWithException(cache);
		
		Workspace ws = Workspace.getWorkspace( IO.getFile("testdata/ws"));
		
		assertNotNull( ws );
		
		Repository repo = ws.getPlugin(Repository.class);
		assertNotNull( repo );
		
		assertNotNull(repo.get("biz.aQute.jpm.daemon", new Version("1.1.0"), null));

		org.osgi.service.repository.Repository osgi = ws.getPlugin(org.osgi.service.repository.Repository.class);
		
		//
		// Get the test and identity capability
		//
		

		Requirement testreq = new CapReqBuilder("test").filter("(test=1)").buildSyntheticRequirement();

		Requirement identity = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)")
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> result = osgi.findProviders(Arrays.asList(testreq, identity));

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

	}

}
