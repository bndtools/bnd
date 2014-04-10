package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.osgi.*;

@SuppressWarnings("resource")
public class MergeTest extends TestCase {

	public static void testFirst() throws Exception {
		testMerge("first", new String[] {
				"A", "C"
		}, new String[] {
			"B"
		}, "first", 0, 0);
	}

	public static void testMergeFirst() throws Exception {
		testMerge("merge-first", new String[] {
				"A", "B", "C"
		}, new String[] {
			""
		}, "first", 0, 0);
	}

	public static void testDefault() throws Exception {
		testMerge(null, new String[] {
				"A", "B", "C"
		}, new String[] {}, "first", 0, 1);
	}

	public static void testMergeLast() throws Exception {
		testMerge("merge-last", new String[] {
				"A", "B", "C"
		}, new String[] {
			""
		}, "last", 0, 0);
	}

	public static void testError() throws Exception {
		testMerge("error", null, null, null, 1, 1);
	}

	static void testMerge(String type, String[] in, String[] out, String c, int errors, int warnings) throws Exception {
		Builder b = new Builder();
		b.setClasspath(new File[] {
				new File("src/test/split/split-a.jar"), new File("src/test/split/split-b.jar")
		});
		Properties p = new Properties();
		if (type != null)
			p.put(Constants.EXPORT_PACKAGE, "test.split;-split-package:=" + type);
		else
			p.put(Constants.EXPORT_PACKAGE, "test.split");
		p.put(Constants.IMPORT_PACKAGE, "");
		b.setProperties(p);
		Jar jar = b.build();

		System.err.println("Errors     :" + b.getErrors());
		System.err.println("Warnings   :" + b.getWarnings());
		assertEquals(errors, b.getErrors().size());
		assertEquals(warnings, b.getWarnings().size());
		if (errors != 0)
			return;

		for (int i = 0; in != null && i < in.length; i++)
			assertNotNull("Contains " + in[i], jar.getResource("test/split/" + in[i]));
		for (int i = 0; out != null && i < out.length; i++)
			assertNull("Does not contain " + out[i], jar.getResource("test/split/" + out[i]));

		Resource r = jar.getResource("test/split/C");
		InputStream is = r.openInputStream();
		BufferedReader dis = new BufferedReader(new InputStreamReader(is));
		String s = dis.readLine();
		assertEquals(s, c);
	}

}
