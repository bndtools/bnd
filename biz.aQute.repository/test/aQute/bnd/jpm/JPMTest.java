package aQute.bnd.jpm;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.service.indexer.impl.util.*;

import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.io.*;

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
}
