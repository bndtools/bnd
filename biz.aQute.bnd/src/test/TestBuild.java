package test;

import junit.framework.*;
import aQute.bnd.main.*;

public class TestBuild extends TestCase {

	public void testX() {}

	public void testBndBuild() throws Exception {
		bnd.main(new String[] {
			"version"
		});
		// bnd.main(new String[] {"-etb",
		// "/Ws/osgi/master/osgi.ct/generated/osgi.ct.cmpn", "runtests",
		// "org.osgi.test.cases.log.bnd", "org.osgi.test.cases.metatype.bnd"});
	}
}
