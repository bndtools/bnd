package aQute.bnd.repository.osgi;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpClient;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import junit.framework.TestCase;

public class OSGiIndexTest extends TestCase {
	File	tmp;
	File	cache;

	private String getTestName() {
		return getClass().getName() + "/" + getName();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tmp = IO.getFile("generated/tmp/test/" + getTestName());
		cache = IO.getFile(tmp, "name");
		IO.delete(tmp);
	}

	public void testIndex() throws Exception {
		HttpClient client = new HttpClient();
		client.setCache(tmp);
		OSGiIndex oi = getIndex(client);

		List<String> list = oi.getBridge()
			.list("osgi.enroute.*");
		assertNotNull(list);
		assertEquals(32, list.size());

		oi = getIndex(client);
		list = oi.getBridge()
			.list("osgi.enroute.*");
		assertNotNull(list);
		assertEquals(32, list.size());

		System.out.println(list);

		SortedSet<Version> versions = oi.getBridge()
			.versions("osgi.enroute.rest.simple.provider");
		assertEquals(1, versions.size());
		System.out.println(versions);

		File f = new File(tmp, "f");
		Promise<File> promise = oi.get("osgi.enroute.rest.simple.provider", new Version("2.0.2.201509211431"), f);
		assertNotNull(promise);
		File value = promise.getValue();
		assertEquals(value, f);
		promise = oi.get("osgi.enroute.rest.simple.provider", new Version("2.0.2.201509211431"), f);
		assertNotNull(promise);
	}

	public void testAggregateIndex() throws Exception {
		HttpClient client = new HttpClient();
		client.setCache(tmp);
		OSGiIndex oi = new OSGiIndex("name", client, cache,
			Collections.singletonList(IO.getFile("testdata/repo7/index-aggregate.xml")
				.toURI()),
			0, false);

		List<String> list = oi.getBridge()
			.list("org.eclipse.*");
		assertNotNull(list);
		assertEquals(9, list.size());

		System.out.println(list);

		SortedSet<Version> versions = oi.getBridge()
			.versions("org.apache.geronimo.components.geronimo-transaction");
		assertEquals(1, versions.size());
	}

	public OSGiIndex getIndex(HttpClient client) throws Exception, URISyntaxException {
		return new OSGiIndex("name", client, cache, Collections.singletonList(
			new URI("https://raw.githubusercontent.com/osgi/osgi.enroute/v1.0.0/cnf/distro/index.xml")), 0, false);
	}
}
