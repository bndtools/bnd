package test;

import aQute.bnd.osgi.Builder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("resource")
public class TestBndBook extends TestCase {

	public static void testFilterout() throws Exception {
		Builder b = new Builder();
		b.addClasspath(IO.getFile("jar/osgi.jar"));
		b.addClasspath(IO.getFile("jar/ds.jar"));
		b.setProperty("Export-Package", "org.eclipse.*, org.osgi.*");
		b.setProperty("fwusers", "${classes;importing;org.osgi.framework}");
		b.setProperty("foo", "${filterout;${fwusers};org\\.osgi\\..*}");
		b.build();
		String fwusers = b.getProperty("fwusers");
		String foo = b.getProperty("foo");
		assertTrue(fwusers.length() > foo.length());
		assertTrue(fwusers.contains("org.osgi.framework.ServicePermission"));
		assertTrue(fwusers.contains("org.eclipse.equinox.ds.instance.BuildDispose"));
		assertFalse(foo.contains("org.osgi.framework.ServicePermission"));
		assertTrue(foo.contains("org.eclipse.equinox.ds.instance.BuildDispose"));
		System.err.println(b.getProperty("fwusers"));
		System.err.println(b.getProperty("foo"));

	}
}
