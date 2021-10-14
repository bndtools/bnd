package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

/**
 * Test the project resolver
 */
public class ProjectResolverTest {

	private Workspace			ws;
	@InjectTemporaryDirectory
	File						tmp;
	private File				local;
	private ResourcesRepository	fr;
	private File				home	= IO.getFile("testdata/projectresolver");

	@BeforeEach
	protected void setUp() throws Exception {
		ws = Workspace.findWorkspace(IO.getFile("testdata/projectresolver/ws"));
		ws.setTrace(true);
		local = IO.getFile(tmp, "local");
		local.mkdirs();

		OSGiRepository or = new OSGiRepository();
		or.setRegistry(ws);
		or.setReporter(ws);
		Map<String, String> map = new HashMap<>();
		map.put("locations", "https://raw.githubusercontent.com/osgi/osgi.enroute/v1.0.0/cnf/distro/index.xml");
		map.put("name", "enroute");
		or.setProperties(map);

		fr = new ResourcesRepository();
		add(fr, IO.getFile(home, "jar/com.example.jerome.application.jar"));
		add(fr, IO.getFile(home, "jar/osgi.enroute.base.api.jar"));

		ws.addBasicPlugin(fr);
		ws.addBasicPlugin(or);
		ws.propertiesChanged();
	}

	private void add(ResourcesRepository fr, File file) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();
		rb.addFile(file, file.toURI());
		Resource resource = rb.build();
		fr.add(resource);
	}

	@AfterEach
	protected void tearDown() throws Exception {
		ws.close();
	}

	@Test
	public void testAugment() throws Exception {
		try (Builder b = new Builder();) {

			//
			// Create an augment jar. We add a foo capability with bar=1 to
			// promises
			//

			b.setBundleSymbolicName("foo.bar");
			b.setProperty("Provide-Capability", "bnd.augment;path=augments.bnd");
			Jar build = b.build();
			String augm = "-augment.a: osgi.promise;capability:='foo;bar=1'";
			build.putResource("augments.bnd", new EmbeddedResource(augm, 10000L));

			//
			// Store it in the repo
			//

			File out = new File(tmp, "for.bar.jar");
			build.write(out);
			add(fr, out);

			//
			// Try to resolve an initial requirement foo;filter:=(bar=1)
			//

			try (Run run = new Run(ws, IO.work, IO.getFile("testdata/projectresolver/augment.bndrun"));) {
				run.setTrace(true);
				assertNotNull(ws.getRepositories());
				System.out.println(ws.getRepositories());
				assertNotNull(run.getWorkspace()
					.getPlugins(Repository.class));
				System.out.println(run.getWorkspace()
					.getPlugins(Repository.class));

				RunResolution r = RunResolution.resolve(run, null)
					.reportException();
				assertTrue(run.check());
				List<VersionedClause> runbundles = r.getRunBundles();
				assertThat(runbundles).hasSize(1);
				assertTrue(run.check());
			}
		}
	}
}
