package aQute.bnd.deployer.repository.wrapper;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.version.Version;
import aQute.lib.deployer.InfoFileRepo;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.map.MAP;
import junit.framework.TestCase;

public class TestWrapper extends TestCase {
	private File tmp;

	public void setUp() throws Exception {
		tmp = IO.getFile("generated/tmp/test/" + getName());
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
				.addDirective("effective", "active")
				.buildSyntheticRequirement();

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

	/**
	 * Attributes do not properly encode lists (they are encoded as "[a,b]",
	 * e.g. there is a toString somewhere // This turned out to be caused by the
	 * PersistentResource.getAttr conversion/type guessing. We always turned an
	 * object into a string; so lists became strings.
	 */
	public void testListsBecomeStrings() throws Exception {
		InfoRepositoryWrapper iw = getRepo();

		Requirement cr = new CapReqBuilder("osgi.service").filter("(objectClass=osgi.enroute.logger.api.LoggerAdmin)")
				.addDirective("effective", "active")
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> provider = iw.findProviders(Collections.singleton(cr));
		assertEquals(1, provider.size());
	}

	public void testFileRepoBasic() throws Exception {
		try (InfoFileRepo repo = getFileRepo(true);) {
			testRepo(1, repo);
		}
	}

	public void testFileRepoBasicWithoutIndex() throws Exception {
		try (InfoFileRepo repo = getFileRepo(false);) {
			testRepo(0, repo);
		}
	}

	private void testRepo(int count, InfoRepository... repo) throws Exception, FileNotFoundException, IOException {
		List<InfoRepository> repos = Arrays.asList(repo);

		for (InfoRepository r : repos) {
			assertNotNull(r.get("osgi.logger.provider", new Version("1.1.0"), null));
		}

		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, repos);

		Requirement req = CapReqBuilder.createBundleRequirement("osgi.logger.provider", null)
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> result = iw.findProviders(Arrays.asList(req));
		assertNotNull(result);
		assertEquals(count, result.get(req)
			.size());

		iw.close();

		iw = new InfoRepositoryWrapper(tmp, repos);

		result = iw.findProviders(Arrays.asList(req));
		assertNotNull(result);
		assertEquals(count, result.get(req)
			.size());

		iw.close();
		for (InfoRepository r : repos) {
			if (r instanceof Closeable)
				((Closeable) r).close();
		}
	}

	/**
	 * Test the augments facility. This allows us to add caps/reqs to bundles
	 * with a file from the workspace.
	 */

	public void testFileRepoAugment() throws Exception {
		InfoFileRepo repo = getFileRepo(true);
		augmentTest(repo);
	}

	private void augmentTest(InfoRepository repo) throws Exception, IOException {
		assertNotNull(repo.get("osgi.logger.provider", new Version("1.1.0"), null));

		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		Properties augments = new UTF8Properties();
		augments.load(new StringReader("osgi.logger.provider: cap=test;test=1\n"));
		iw.addAugment(augments);

		//
		// Get the test and identity capability
		//

		Requirement testreq = new CapReqBuilder("test").filter("(test=1)").buildSyntheticRequirement();

		Requirement identity = new CapReqBuilder("osgi.identity").filter("(osgi.identity=osgi.logger.provider)")
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

	private InfoRepositoryWrapper getRepo() throws Exception {
		InfoFileRepo repo = getFileRepo(true);
		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));
		return iw;
	}

	private InfoFileRepo getFileRepo(boolean info) throws Exception, FileNotFoundException {
		InfoFileRepo repo = new InfoFileRepo();
		repo.setProperties(MAP.$("location", tmp.getAbsolutePath()).$("index", "" + info));

		Collection<File> files = IO.tree(IO.getFile("testdata/ws/cnf/repo"), "*.jar");
		for (File f : files) {
			repo.put(new FileInputStream(f), null);
		}
		return repo;
	}

}
