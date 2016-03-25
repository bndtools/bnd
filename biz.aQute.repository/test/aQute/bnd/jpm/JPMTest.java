package aQute.bnd.jpm;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.osgi.service.indexer.impl.util.Hex;

import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.service.library.Library.RevisionRef;
import junit.framework.TestCase;

public class JPMTest extends TestCase {
	File				tmp;
	private Repository	repo;

	@Override
	public void setUp() {
		System.setProperty("jpm4j.in.test", "true");

		tmp = IO.getFile("generated/tmp");
		IO.delete(tmp);
		tmp.mkdirs();

		repo = new Repository();
		Map<String,String> props = new HashMap<String,String>();
		props.put("location", new File(tmp, "loc").getAbsolutePath());
		props.put("index", new File(tmp, "index").getAbsolutePath());
		repo.setProperties(props);
	}

	@Override
	protected void tearDown() throws Exception {
		repo.close();
		IO.delete(tmp);

		super.tearDown();
	}

	public void testJpm() throws Exception {
		File file = IO.getFile("testdata/ws/cnf/jar/biz.aQute.bnd.annotation-2.3.0.jar");
		boolean dropTarget = repo.dropTarget(file.toURI());
		assertTrue(dropTarget);
		File f = repo.get("biz.aQute.bnd.annotation", new Version("2.3.0.201404170725"), null);
		assertNotNull(f);

		ResourceDescriptor descriptor = repo.getDescriptor("biz.aQute.bnd.annotation",
				new Version("2.3.0.201404170725"));

		assertNotNull(descriptor);
		assertEquals("230ae22893a124cdda8910e240d9c12edbacbbe13d8d080610adfc12b06623ff",
				Hex.toHexString(descriptor.sha256));
		assertEquals(file.toURI().toString(), descriptor.url.toString());

		SortedSet<Version> versions = repo.versions("biz.aQute.bnd.annotation");
		assertEquals(1, versions.size());

		assertEquals(new Version("2.3.0.201404170725"), versions.iterator().next());
	}

	public void testDropUrl() throws Exception {
		repo.dropTarget(IO.getFile("testdata/ws/cnf/jar/biz.aQute.bnd.annotation-2.3.0.jar").toURI());
		assertFalse("index should not be dirty after drop", repo.index.isDirty());

		// Read the index, check only one resource with one mirror URL is listed
		List<RevisionRef> refs = new Index(IO.getFile(tmp, "index")).getRevisionRefs();
		assertEquals(1, refs.size());
		assertEquals(1, refs.get(0).urls.size());
	}

	public void testAddMirrorURL() throws Exception {
		// Drop two URIs for an identical resource
		URI uri1 = IO.getFile("testdata/ws/cnf/jar/biz.aQute.bnd.annotation-2.3.0.jar").toURI();
		repo.dropTarget(uri1);
		URI uri2 = IO.getFile("testdata/ws/cnf/jar/biz.aQute.bnd.annotation-2.3.0-duplicate.jar").toURI();
		repo.dropTarget(uri2);

		// Read the index, there should be one resource but two mirror URLs
		List<RevisionRef> refs = new Index(IO.getFile(tmp, "index")).getRevisionRefs();
		assertEquals(1, refs.size());
		assertEquals(2, refs.get(0).urls.size());
		assertTrue(refs.get(0).urls.contains(uri1));
		assertTrue(refs.get(0).urls.contains(uri2));
	}

}
