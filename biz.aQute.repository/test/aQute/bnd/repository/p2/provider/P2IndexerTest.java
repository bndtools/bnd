package aQute.bnd.repository.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import junit.framework.TestCase;

public class P2IndexerTest extends TestCase {
	File tmp;

	@Override
	protected void setUp() {
		tmp = IO.getFile("generated/tmp/test/" + getClass().getName() + "/" + getName());
		IO.delete(tmp);
		tmp.mkdirs();
	}

	public void testEclipseP2Repo() throws Exception {
		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));

			try (P2Indexer p2 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client,
				new URI("https://download.eclipse.org/egit/updates-4.7.1/"), getName())) {
				List<String> bsns = p2.list(null);
				System.out.println(bsns);
				assertThat(bsns).contains("org.kohsuke.args4j", "org.slf4j.api", "org.apache.httpcomponents.httpclient",
					"org.eclipse.egit.ui.source", "org.eclipse.jgit.archive", "org.eclipse.jgit.pgm.source",
					"org.eclipse.egit", "org.eclipse.egit.ui", "org.eclipse.egit.mylyn.ui.source",
					"org.eclipse.jgit.pgm", "org.apache.commons.compress", "org.eclipse.egit.gitflow.ui",
					"org.slf4j.impl.log4j12", "org.eclipse.jgit.ui", "javaewah", "org.apache.httpcomponents.httpcore",
					"org.eclipse.jgit.source", "org.eclipse.jgit.http.apache", "org.eclipse.egit.gitflow.source",
					"org.eclipse.jgit", "org.eclipse.egit.core.source", "org.eclipse.egit.gitflow.ui.source",
					"org.eclipse.egit.mylyn.ui", "org.eclipse.jgit.lfs", "org.eclipse.egit.gitflow",
					"org.eclipse.egit.core", "org.eclipse.egit.ui.smartimport", "org.eclipse.jgit.lfs.server",
					"org.eclipse.egit.doc", "org.apache.log4j", "com.google.gson", "com.jcraft.jsch");
			}
		}
	}

	public void testURI() throws Exception {
		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));

			try (P2Indexer p2 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client,
				new URI("https://dl.bintray.com/bndtools/bndtools/3.5.0/"), getName())) {
				List<String> bsns = p2.list(null);
				System.out.println(bsns);
				assertThat(bsns).contains("javax.xml", "org.bndtools.templating.gitrepo",
					"org.bndtools.headless.build.manager", "javax.xml.stream", "org.bndtools.templating",
					"org.slf4j.api", "bndtools.api", "bndtools.jareditor",
					"org.bndtools.versioncontrol.ignores.plugin.git", "bndtools.m2e", "org.bndtools.embeddedrepo",
					"biz.aQute.resolve", "org.bndtools.versioncontrol.ignores.manager", "bndtools.builder",
					"bndtools.core", "biz.aQute.repository", "org.bndtools.headless.build.plugin.gradle",
					"bndtools.release", "org.bndtools.headless.build.plugin.ant", "org.osgi.impl.bundle.repoindex.lib",
					"biz.aQute.bndlib");
			}
		}
	}

	public void testFile() throws Throwable {
		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));

			File input = IO.getFile("testdata/p2/macbadge");
			assertThat(input).as("%s must be dir", input)
				.isDirectory();

			try (P2Indexer p2 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client, input.toURI(),
				getName())) {
				List<String> bsns = p2.list(null);
				assertThat(bsns).containsExactly("name.njbartlett.eclipse.macbadge");

				System.out.println(bsns);

				assertThat(p2.versions("name.njbartlett.eclipse.macbadge"))
					.containsExactly(new Version("1.0.0.201110100042"));

				File f = p2.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null);
				assertThat(f).isNotNull()
					.hasName("name.njbartlett.eclipse.macbadge-1.0.0.201110100042.jar");
				assertThat(f.length()).isEqualTo(4672);

				String sha256 = SHA256.digest(f)
					.asHex();

				Repository repository = p2.getBridge()
					.getRepository();
				RequirementBuilder rb = new RequirementBuilder("osgi.content");

				rb.addDirective("filter", "(osgi.content~=" + sha256.toLowerCase() + ")");

				Requirement req = rb.synthetic();
				Collection<Capability> collection = repository.findProviders(Collections.singleton(req))
					.get(req);
				Set<Resource> resources = ResourceUtils.getResources(collection);

				assertThat(resources).hasSize(1);

				final AtomicReference<Throwable> result = new AtomicReference<>();
				final Semaphore sem = new Semaphore(0);

				p2.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null,
					new RepositoryPlugin.DownloadListener() {

						@Override
						public void success(File file) throws Exception {
							try {} catch (Throwable e) {
								result.set(e);
							} finally {
								sem.release();
							}
						}

						@Override
						public void failure(File file, String reason) throws Exception {
							try {
								fail(reason);
							} catch (Throwable e) {
								result.set(e);
							} finally {
								sem.release();
							}
						}

						@Override
						public boolean progress(File file, int percentage) throws Exception {
							return true;
						}
					});

				sem.acquire();
				if (result.get() != null)
					throw result.get();

			} catch (InvocationTargetException ite) {
				Throwable t = Exceptions.unrollCause(ite, InvocationTargetException.class);
				t.printStackTrace();
				throw t;
			}

			try (P2Indexer p3 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client, input.toURI(),
				"test")) {
				File f = p3.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null);
				assertThat(f).isNotNull()
					.hasName("name.njbartlett.eclipse.macbadge-1.0.0.201110100042.jar");
				assertThat(f.length()).isEqualTo(4672);
			}
		}
	}

	public void testRefresh() throws Exception {
		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));

			try (P2Indexer p2 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client,
				new URI("https://dl.bintray.com/bndtools/bndtools/3.5.0/"), getName())) {

				assertThat(p2.versions("bndtools.core")).hasSize(1);

				p2.refresh();

				assertThat(p2.versions("bndtools.core")).hasSize(1);
			}
		}
	}

	public void testTargetPlatform() throws Throwable {
		try (HttpClient client = new HttpClient()) {
			client.setCache(IO.getFile(tmp, "cache"));

			File input = IO.getFile("testdata/p2/macbadge/");
			File targetFile = new File("testdata/targetplatform/macbadge.target");
			String content = IO.collect(targetFile);
			content = content.replaceAll("\\$\\{repo\\}", input.toURI()
				.toString());
			targetFile = new File(tmp, "macbadge.target");
			IO.store(content, targetFile);

			try (P2Indexer p2 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client,
				targetFile.getAbsoluteFile()
					.toURI(),
				getName())) {
				List<String> bsns = p2.list(null);
				assertThat(bsns).containsExactly("name.njbartlett.eclipse.macbadge");

				System.out.println(bsns);

				assertThat(p2.versions("name.njbartlett.eclipse.macbadge"))
					.containsExactly(new Version("1.0.0.201110100042"));

				File f = p2.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null);
				assertThat(f).isNotNull()
					.hasName("name.njbartlett.eclipse.macbadge-1.0.0.201110100042.jar");
				assertThat(f.length()).isEqualTo(4672);

				String sha256 = SHA256.digest(f)
					.asHex();

				Repository repository = p2.getBridge()
					.getRepository();
				RequirementBuilder rb = new RequirementBuilder("osgi.content");

				rb.addDirective("filter", "(osgi.content~=" + sha256.toLowerCase() + ")");

				Requirement req = rb.synthetic();
				Collection<Capability> collection = repository.findProviders(Collections.singleton(req))
					.get(req);
				Set<Resource> resources = ResourceUtils.getResources(collection);

				assertThat(resources).hasSize(1);

				final AtomicReference<Throwable> result = new AtomicReference<>();
				final Semaphore sem = new Semaphore(0);

				p2.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null,
					new RepositoryPlugin.DownloadListener() {

						@Override
						public void success(File file) throws Exception {
							try {} catch (Throwable e) {
								result.set(e);
							} finally {
								sem.release();
							}
						}

						@Override
						public void failure(File file, String reason) throws Exception {
							try {
								fail(reason);
							} catch (Throwable e) {
								result.set(e);
							} finally {
								sem.release();
							}
						}

						@Override
						public boolean progress(File file, int percentage) throws Exception {
							return true;
						}
					});

				sem.acquire();
				if (result.get() != null)
					throw result.get();

			} catch (InvocationTargetException ite) {
				Throwable t = Exceptions.unrollCause(ite, InvocationTargetException.class);
				t.printStackTrace();
				throw t;
			}

			try (P2Indexer p3 = new P2Indexer(new Slf4jReporter(P2IndexerTest.class), tmp, client, input.toURI(),
				getName())) {
				File f = p3.get("name.njbartlett.eclipse.macbadge", new Version("1.0.0.201110100042"), null);
				assertThat(f).isNotNull()
					.hasName("name.njbartlett.eclipse.macbadge-1.0.0.201110100042.jar");
				assertThat(f.length()).isEqualTo(4672);
			}
		}
	}
}
