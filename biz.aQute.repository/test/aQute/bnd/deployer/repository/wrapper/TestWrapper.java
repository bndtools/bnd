package aQute.bnd.deployer.repository.wrapper;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.osgi.resource.*;

import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.libg.map.*;
import aQute.library.bnd.*;

public class TestWrapper extends TestCase {
	File				tmp	= new File("tmp");

	public void setUp() throws Exception {
		tmp.mkdirs();
		IO.delete(tmp);
		tmp.mkdirs();

	}

	public void tearDown() throws Exception {
		IO.delete(tmp);
	}

	public void testbasic() throws Exception {
		JpmRepository repo = new JpmRepository();
		repo.setProperties(MAP.$("location", tmp.getAbsolutePath()).$("index", "testdata/ws/cnf/jpm4j.json"));
		assertNotNull(repo.get("biz.aQute.jpm.daemon", new Version("1.1.0"), null));

		InfoRepositoryWrapper iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		Requirement cr = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)")
				.buildSyntheticRequirement();

		Map<Requirement,Collection<Capability>> result = iw.findProviders(Arrays.asList(cr));
		assertNotNull(result);
		assertEquals(1, result.size());

		iw.close();

		cr = new CapReqBuilder("osgi.identity").filter("(osgi.identity=biz.aQute.jpm.daemon)").buildSyntheticRequirement();
		iw = new InfoRepositoryWrapper(tmp, Collections.singleton(repo));

		result = iw.findProviders(Arrays.asList(cr));
		assertNotNull(result);
		assertEquals(1, result.size());

		iw.close();
	}

}
