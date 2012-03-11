package aQute.lib.repository;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;
import aQute.libg.version.Version;

import junit.framework.TestCase;

public class TestFixedIndexedRepo extends TestCase {
	
	private int countBundles(RepositoryPlugin repo) throws Exception {
		int count = 0;
		
		List<String> list = repo.list(null);
		if (list != null) for (String bsn : list) {
			List<Version> versions = repo.versions(bsn);
			if (versions != null) count += versions.size();
		}
		
		return count;
	}
	
	public void testIndex1() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "index1");
		props.put("locations", new File("testdata/index1.xml").toURI().toString());
		repo.setProperties(props);

		List<String> bsns = repo.list(null);
		assertEquals(2, bsns.size());
		assertEquals("org.example.c", bsns.get(0));
		assertEquals("org.example.f", bsns.get(1));
	}
	
	public void testIndex2() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "index2");
		props.put("locations", new File("testdata/index2.xml").toURI().toString());
		repo.setProperties(props);
		
		assertEquals(56, countBundles(repo));
	}

	public void testIndex2Compressed() throws Exception {
		FixedIndexedRepo repo = new FixedIndexedRepo();
		Map<String, String> props = new HashMap<String, String>();
		props.put("name", "index2");
		props.put("locations", new File("testdata/index2.xml.gz").toURI().toString());
		repo.setProperties(props);
		
		assertEquals(56, countBundles(repo));
	}


}
