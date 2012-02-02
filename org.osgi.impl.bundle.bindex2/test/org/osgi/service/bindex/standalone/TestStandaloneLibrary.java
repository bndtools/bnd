package org.osgi.service.bindex.standalone;

import java.util.LinkedList;
import java.util.List;

import org.osgi.service.bindex.impl.Utils;

import junit.framework.TestCase;

public class TestStandaloneLibrary extends TestCase {

	public void testTheFuckingThing() throws Exception {
		List<String> args = new LinkedList<String>();

		args.add("java");
		args.add("-cp");
		args.add("generated/org.osgi.impl.bundle.bindex2.lib.jar" + System.getProperty("path.separator") + "bin_test");
		args.add("test.Main");

		Process process = new ProcessBuilder(args).start();
		String output = Utils.readStream(process.getInputStream());
		String errors = Utils.readStream(process.getErrorStream());
		
		System.out.println(output);
		System.err.println(errors);
		int returnCode = process.waitFor();

		assertEquals(0, returnCode);
	}

}
