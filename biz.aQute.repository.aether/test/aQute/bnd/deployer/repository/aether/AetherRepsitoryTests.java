package aQute.bnd.deployer.repository.aether;

import java.io.*;
import java.util.*;

import junit.framework.*;
import test.lib.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.version.*;
import aQute.lib.io.*;

@SuppressWarnings("restriction")
public class AetherRepsitoryTests extends TestCase {

	private static NanoHTTPD	httpd;
	private static int			httpdPort;

	private static DownloadListener listener = new DownloadListener() {

		@Override
		public void failure(File file, String reason) throws Exception {}

		@Override
		public boolean progress(File file, int percentage) throws Exception {
			return true;
		}

		@Override
		public void success(File file) throws Exception {}
	};

	private AetherRepository createRepo() throws Exception {
		AetherRepository repo = new AetherRepository();

		Map<String,String> props = new HashMap<String,String>();
		props.put(AetherRepository.PROP_MAIN_URL, "http://127.0.0.1:" + httpdPort);
		repo.setProperties(props);

		return repo;
	}

	@Override
	protected void setUp() throws Exception {
		httpd = new NanoHTTPD(0, IO.getFile("testdata/repo"));
		httpdPort = httpd.getPort();
	}

	@Override
	protected void tearDown() throws Exception {
		httpd.stop();
	}

	public void testVersionsBadBsn() throws Exception {
		RepositoryPlugin repo = createRepo();
		SortedSet<Version> versions = repo.versions("foo.bar.foobar");

		assertNull(versions);
	}

	public void testVersionsGAVStyle() throws Exception {
		RepositoryPlugin repo = createRepo();
		SortedSet<Version> versions = repo.versions("javax.servlet:servlet-api");

		assertNotNull(versions);
		assertTrue(versions.size() == 2);
		assertEquals("2.4.0", versions.first().toString());
		assertEquals("2.5.0", versions.last().toString());
	}

	public void testStrategyExactVersion() throws Exception {
		RepositoryPlugin repo = createRepo();

		Map<String,String> attrs = new HashMap<String,String>();
		attrs.put("version", "2.5");
		attrs.put("strategy", "exact");

		File file = repo.get("javax.servlet:servlet-api", new Version(2, 5, 0), attrs, listener);

		assertNotNull(file);
		assertEquals("servlet-api-2.5.jar", file.getName());
	}

	public void testStrategyExactVersionBadGAV() throws Exception {
		RepositoryPlugin repo = createRepo();

		Map<String,String> attrs = new HashMap<String,String>();
		attrs.put("version", "1.0");
		attrs.put("strategy", "exact");

		File file = repo.get("foo.bar:foobar-api", new Version(1, 0, 0), attrs, listener);

		assertNull(file);
	}

	public void testSourceLookup() throws Exception {
		RepositoryPlugin repo = createRepo();

		Map<String,String> attrs = new HashMap<String,String>();
		attrs.put("version", "2.5");
		attrs.put("bsn", "javax.servlet:servlet-api");

		File file = repo.get("servlet-api.source", new Version(2, 5, 0), attrs, listener);

		assertTrue(file.exists());
		assertEquals("servlet-api-2.5-sources.jar", file.getName());
	}
}
