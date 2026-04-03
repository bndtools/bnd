package biz.aQute.resolve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.model.EE;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.MultiReleaseNamespace;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.repository.osgi.OSGiRepository;
import aQute.lib.io.IO;

public class StandaloneTest {

	@Test
	public void testMultiRelease(@TempDir
	File dir) throws Exception {
		File loc = new File(dir, "repo.xml");
		File bndrun = new File(dir, "test.bndrun");

		for (EE ee : EnumSet.of(EE.JavaSE_1_8, EE.JavaSE_10, EE.JavaSE_12, EE.JavaSE_19)) {
			System.out.println(ee);
			ResourcesRepository repo = new ResourcesRepository();
			repo.add(ResourceBuilder.parse(IO.getFile("testdata/jar/multi-release-ok.jar"), null));
			repo.add(ResourceBuilder.parse(IO.getFile("testdata/org.apache.felix.framework-4.4.0.jar"), null));
			XMLResourceGenerator xml = new XMLResourceGenerator();
			xml.repository(repo);
			xml.save(loc);

			try (Formatter f = new Formatter()) {
				f.format("-standalone repo.xml\n");
				f.format("-runrequires bnd.identity;id='multirelease.main\n");
				f.format("-runfw org.apache.felix.framework\n");
				f.format("-runee %s\n", ee.getEEName());
				f.format("-resolve cache\n");
				f.format("-runsystemcapabilities.extra fake;fake=fake;version=1.2.3");
				f.format("");
				IO.store(f, bndrun);
				System.out.println(f);
			}
			Bndrun run = (Bndrun) Bndrun.createRun(null, bndrun);
			RunResolution resolve = run.resolve();
			assertThat(resolve.isOK());
			System.out.println(resolve.log);
			assertThat(resolve.getOrderedResources()).hasSize(2);
			Collection<Container> runbundles = run.getRunbundles();
			assertThat(runbundles).hasSize(1);
			assertThat(runbundles.iterator()
				.next()
				.getFile()
				.getName()).isEqualTo("multirelease.main-0.0.0.jar");

			Resource synthetic = resolve.getOrderedResources()
				.stream()
				.flatMap(r -> ResourceUtils.capabilityStream(r, MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE))
				.findFirst()
				.get()
				.getResource();

			Requirement requirement = synthetic
				.getRequirements(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)
				.get(0);
			SortedSet<EE> ees = EE.getEEsFromRequirement(requirement.toString());
			System.out.println(ees);
			assertThat(ees).contains(ee);
		}
	}

	@Test
	public void testStandalone() throws Exception {
		File f = IO.getFile("testdata/standalone/simple.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("https://example.org/index.xml", f0.getLocation());
	}

	@Test
	public void testMultipleUrls() throws Exception {
		File f = IO.getFile("testdata/standalone/multi.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);
		assertTrue(repositories.get(1) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("repo01", f0.getName());
		assertEquals("https://example.org/index1.xml", f0.getLocation());

		OSGiRepository f1 = (OSGiRepository) repositories.get(1);
		assertEquals("second", f1.getName());
		assertEquals("https://example.org/index2.xml", f1.getLocation());
	}

	@Test
	public void testMergedUrls() throws Exception {
		File f = IO.getFile("testdata/standalone/merged.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);
		assertTrue(repositories.get(1) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("repo01", f0.getName());
		assertEquals("https://example.org/index1.xml", f0.getLocation());

		OSGiRepository f1 = (OSGiRepository) repositories.get(1);
		assertEquals("second", f1.getName());
		assertEquals("https://example.org/index2.xml", f1.getLocation());
	}

	@Test
	public void testRelativeUrl() throws Exception {
		File f = IO.getFile("testdata/standalone/relative_url.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(2, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);
		assertTrue(repositories.get(1) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("repo01", f0.getName());
		String resolvedUrl = IO.getFile("testdata/larger-repo.xml")
			.toURI()
			.toString();
		assertEquals(resolvedUrl, f0.getLocation());

		OSGiRepository f1 = (OSGiRepository) repositories.get(1);
		assertEquals("repo02", f1.getName());

		assertEquals("https://example.org/index2.xml", f1.getLocation());
	}

	@Test
	public void testExtraAttribs() throws Exception {
		File f = IO.getFile("testdata/standalone/attribs.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals("foo", f0.getName());
		assertEquals("https://example.org/index.xml", f0.getLocation());

		File cacheDir = IO.getFile(System.getProperty("user.home") + "/.custom_cache_dir");
		assertEquals(cacheDir, f0.getRoot());
	}

	@Test
	public void testMacroExpansion() throws Exception {
		File f = IO.getFile("testdata/standalone/macro.bndrun");
		Run run = Run.createRun(null, f);

		List<Repository> repositories = run.getWorkspace()
			.getPlugins(Repository.class);
		assertEquals(1, repositories.size());
		assertTrue(repositories.get(0) instanceof OSGiRepository);

		OSGiRepository f0 = (OSGiRepository) repositories.get(0);
		assertEquals(System.getProperty("user.name") + " M2", f0.getName());
		File indexFile = IO.getFile(System.getProperty("user.home") + "/.m2/repository/repository.xml");
		assertEquals(indexFile.toURI()
			.toString(), f0.getLocation());
	}
}
