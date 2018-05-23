package aQute.libg.uri;

import java.io.File;
import java.net.URI;

import junit.framework.TestCase;

public class URIUtilsTest extends TestCase {

	public void testResolveAbsolute() throws Exception {
		// reference is absolute, so base is irrelevant
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "http://example.org/bar.xml");
		assertEquals("http://example.org/bar.xml", result.toString());
	}

	public void testResolveRelativeHttp() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "bar.xml");
		assertEquals("http://example.com/bar.xml", result.toString());
	}

	public void testResolveBlank() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "");
		assertEquals("http://example.com/foo.xml", result.toString());
	}

	public void testResolveFragmentBlank() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml#bar"), "");
		assertEquals("http://example.com/foo.xml", result.toString());
	}

	public void testResolveAbsoluteWindowsPath() throws Exception {
		if (isWindows()) {
			URI result = URIUtil.resolve(URI.create("file:/C:/Users/jimbob/base.txt"), "C:\\Users\\sub dir\\foo.txt");
			assertEquals("file:/C:/Users/sub%20dir/foo.txt", result.toString());
			result = URIUtil.resolve(URI.create("file:/C:/Users/jimbob/base.txt"), "C:/Users/sub dir/bar.txt");
			assertEquals("file:/C:/Users/sub%20dir/bar.txt", result.toString());
		}
	}

	public void testResolveRelativeWindowsPath() throws Exception {
		if (isWindows()) {
			URI result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "sub dir\\foo.txt");
			assertEquals("file:/C:/Users/jim/sub%20dir/foo.txt", result.toString());
		}
	}

	public void testResolveUNCWindowsPath() throws Exception {
		if (isWindows()) {
			URI result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "\\\\server\\share\\foo.txt");
			assertEquals("file:////server/share/foo.txt", result.toString());
			result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "//server/share/foo.txt");
			assertEquals("file:////server/share/foo.txt", result.toString());
		}
	}

	private static boolean isWindows() {
		return File.separatorChar == '\\';
	}

}
