package aQute.bnd.repository.p2.provider;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.lib.io.IO;

public class P2RepositoryTest {
	File tmp;

	@BeforeEach
	public void setUp(TestInfo info) throws Exception {
		Method testMethod = info.getTestMethod()
			.get();
		tmp = Paths.get("generated/tmp/test", getClass().getName(), testMethod.getName())
			.toAbsolutePath()
			.toFile();
		IO.delete(tmp);
		IO.mkdirs(tmp);
	}

	@Test
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

	@Test
	public void testXtext() throws Exception {

		try (P2Repository p2r = new P2Repository()) {
			Workspace w = Workspace.createStandaloneWorkspace(new Processor(), tmp.toURI());
			w.setTrace(true);
			w.setBase(tmp);
			p2r.setRegistry(w);

			Map<String, String> config = new HashMap<>();
			config.put("url", "http://download.eclipse.org/modeling/tmf/xtext/updates/releases/head/R201304180855/");
			config.put("name", "test");
			p2r.setProperties(config);
			List<String> list = p2r.list(null);
			assertTrue(w.check());
			assertNotNull(list);
			assertTrue(list.size() > 1);
		}
	}
}
