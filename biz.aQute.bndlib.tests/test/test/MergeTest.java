package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

@SuppressWarnings("resource")
public class MergeTest {

	@Test
	public void testFirst() throws Exception {
		testMerge("first", new String[] {
			"A", "C"
		}, new String[] {
			"B"
		}, "first", 0, 0);
	}

	@Test
	public void testMergeFirst() throws Exception {
		testMerge("merge-first", new String[] {
			"A", "B", "C"
		}, new String[] {
			""
		}, "first", 0, 0);
	}

	@Test
	public void testDefault() throws Exception {
		testMerge(null, new String[] {
			"A", "B", "C"
		}, new String[] {}, "first", 0, 1);
	}

	@Test
	public void testMergeLast() throws Exception {
		testMerge("merge-last", new String[] {
			"A", "B", "C"
		}, new String[] {
			""
		}, "last", 0, 0);
	}

	@Test
	public void testError() throws Exception {
		testMerge("error", null, null, null, 1, 1);
	}

	void testMerge(String type, String[] in, String[] out, String c, int errors, int warnings) throws Exception {
		Builder b = new Builder();
		try {
			b.setClasspath(new File[] {
				IO.getFile("test/test/split/split-a.jar"), IO.getFile("test/test/split/split-b.jar")
			});
			Properties p = new Properties();
			if (type != null)
				p.put("Export-Package", "test.split;-split-package:=" + type);
			else
				p.put("Export-Package", "test.split");
			p.put("Import-Package", "");
			b.setProperties(p);
			Jar jar = b.build();

			System.err.println("Errors     :" + b.getErrors());
			System.err.println("Warnings   :" + b.getWarnings());
			assertEquals(errors, b.getErrors()
				.size());
			assertEquals(warnings, b.getWarnings()
				.size());
			if (errors != 0)
				return;

			for (int i = 0; in != null && i < in.length; i++)
				assertNotNull(jar.getResource("test/split/" + in[i]), "Contains " + in[i]);
			for (int i = 0; out != null && i < out.length; i++)
				assertNull(jar.getResource("test/split/" + out[i]), "Does not contain " + out[i]);

			Resource r = jar.getResource("test/split/C");
			InputStream is = r.openInputStream();
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));
			String s = dis.readLine();
			assertEquals(s, c);
		} finally {
			b.close();
		}
	}

}
