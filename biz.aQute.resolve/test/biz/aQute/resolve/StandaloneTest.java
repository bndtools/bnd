package biz.aQute.resolve;

import java.io.File;
import java.util.List;

import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class StandaloneTest extends TestCase {

	public void testStandalone() throws Exception {
		File f = IO.getFile("testdata/standalone/simple.bndrun");
		Run run = Run.createStandaloneRun(f);

		List<Repository> repositories = run.getWorkspace().getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof FixedIndexedRepo);

		FixedIndexedRepo f0 = (FixedIndexedRepo) repositories.get(0);
		assertEquals("_1", f0.getName());
	}
}
