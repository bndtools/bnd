package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.http.testservers.HttpTestServer.Config;
import aQute.lib.io.IO;
import aQute.maven.provider.FakeNexus;
import junit.framework.TestCase;

public class WorkspaceTest extends TestCase {
	File	tmp		= IO.getFile("generated/tmp");
	File	local	= IO.getFile(tmp, "local");
	File	remote	= IO.getFile(tmp, "remote");
	File	index	= IO.getFile(tmp, "index");
	File	build	= IO.getFile(tmp, "workspace/cnf/build.bnd");

	private MavenBndRepository	repo;
	private FakeNexus			fnx;
	private Processor			reporter;
	private Workspace			workspace;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IO.delete(tmp);
		remote.mkdirs();
		local.mkdirs();

		IO.copy(IO.getFile("testresources/mavenrepo"), remote);
		IO.copy(IO.getFile("testresources/mavenrepo/index.maven"), index);

		Config config = new Config();
		fnx = new FakeNexus(config, remote);
		fnx.start();
	}

	public void testEnv() throws Exception {
		config(null);
		assertNotNull(workspace);
		assertNotNull(repo);
		System.out.println(workspace.getBase());
	}

	public void testDefaultIndexFile() throws Exception {
		config(null);
	}

	public void testDefaultPollWithChange() throws Exception {
		config(null);
	}

	public void testDefaultPollWithoutChange() throws Exception {
		config(null);
	}

	public void testRelease() {

	}

	void config(Map<String,String> override) throws Exception {
		Map<String,String> config = new HashMap<>();
		config.put("local", "generated/tmp/local");
		config.put("index", "generated/tmp/index");
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

			build.getParentFile().mkdirs();
			IO.store(sb.toString(), build);

			workspace = Workspace.getWorkspace(build.getParentFile().getParentFile());
			repo = workspace.getPlugin(MavenBndRepository.class);
		}
	}

}
