package test;

import aQute.bnd.main.*;
import junit.framework.*;

public class TestLibSync extends TestCase {

	public void testlibsync() throws Exception {
		bnd.main(new String[] {"-trace", "libsync.repo=http://localhost:8080/repo", "libsync", "tmp/biz.aQute.bnd.jar"});
	}
}
