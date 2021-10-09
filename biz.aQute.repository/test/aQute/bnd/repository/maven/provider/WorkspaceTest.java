package aQute.bnd.repository.maven.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import aQute.bnd.build.Workspace;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.provider.FakeNexus;

public class WorkspaceTest {
	String						tmpName;
	File						tmp;
	File						local;
	File						remote;
	File						index;
	File						build;

	private MavenBndRepository	repo;
	private FakeNexus			fnx;
	private Workspace			workspace;

	@BeforeEach
	protected void setUp(TestInfo testInfo) throws Exception {
		tmpName = "generated/tmp/test/" + testInfo.getTestClass()
			.get()
			.getName() + "/"
			+ testInfo.getTestMethod()
				.get()
				.getName();
		tmp = IO.getFile(tmpName);
		IO.delete(tmp);
		local = IO.getFile(tmp, "local");
		remote = IO.getFile(tmp, "remote");
		index = IO.getFile(tmp, "index");
		build = IO.getFile(tmp, "workspace/cnf/build.bnd");
		remote.mkdirs();
		local.mkdirs();

		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		IO.copy(IO.getFile("testresources/mavenrepo/index.maven"), index);

		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		IO.close(fnx);
	}

	@Test
	public void testEnv() throws Exception {
		config(null);
		assertNotNull(workspace);
		assertNotNull(repo);
		System.out.println(workspace.getBase());
	}

	void config(Map<String, String> override) throws Exception {
		Map<String, String> config = new HashMap<>();
		config.put("local", tmpName + "/local");
		config.put("index", tmpName + "/index");
		config.put("releaseUrl", fnx.getBaseURI() + "/repo/");

		if (override != null)
			config.putAll(override);

		try (Formatter sb = new Formatter();) {
			sb.format("-plugin.maven= \\\n");
			sb.format("  %s; \\\n", MavenBndRepository.class.getName());
			sb.format("  name=test; \\\n", MavenBndRepository.class.getName());
			sb.format("  local=%s; \\\n", config.get("local"));
			sb.format("  releaseUrl=%s; \\\n", config.get("releaseUrl"));
			sb.format("  index=%s\n", config.get("index"));

			build.getParentFile()
				.mkdirs();
			IO.store(sb.toString(), build);

			workspace = Workspace.getWorkspace(build.getParentFile()
				.getParentFile());
			repo = workspace.getPlugin(MavenBndRepository.class);
		}
	}

}
