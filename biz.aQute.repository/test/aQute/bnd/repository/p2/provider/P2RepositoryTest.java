package aQute.bnd.repository.p2.provider;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class P2RepositoryTest extends TestCase {
	File tmp = IO.getFile("generated/tmp");

	@Override
	public void setUp() {
		IO.delete(tmp);
		tmp.mkdirs();
	}

	public void testSimple() throws Exception {
		try (P2Repository p2r = new P2Repository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "https://dl.bintray.com/bndtools/bndtools/latest/");
			config.put("name", "test");
			p2r.setProperties(config);

			List<String> list = p2r.list(null);
			assertNotNull(list);
			assertTrue(list.size() > 1);
		}
	}

}
