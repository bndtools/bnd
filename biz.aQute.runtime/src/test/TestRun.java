package test;

import java.io.*;

import junit.framework.*;
import aQute.junit.runtime.*;

public class TestRun extends TestCase {

	public void testSimple() throws Exception {
		Target.main(new String[] { "-set", "noframework", "true", "-test",
				"xtest.ArchetypicalTest", "-target",
				new File("src/test/testbundle.jar").getAbsolutePath() });
	}
}
