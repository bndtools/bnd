package aQute.libg.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class URIUtilsTest {

	@Test
	public void testResolveAbsolute() throws Exception {
		// reference is absolute, so base is irrelevant
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "http://example.org/bar.xml");
		assertThat(result).hasToString("http://example.org/bar.xml");
	}

	@Test
	public void testResolveRelativeHttp() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "bar.xml");
		assertThat(result).hasToString("http://example.com/bar.xml");
	}

	@Test
	public void testResolveBlank() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml"), "");
		assertThat(result).hasToString("http://example.com/foo.xml");
	}

	@Test
	public void testResolveFragmentBlank() throws Exception {
		URI result = URIUtil.resolve(URI.create("http://example.com/foo.xml#bar"), "");
		assertThat(result).hasToString("http://example.com/foo.xml");
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testResolveAbsoluteWindowsPath() throws Exception {
		URI result = URIUtil.resolve(URI.create("file:/C:/Users/jimbob/base.txt"), "C:\\Users\\sub dir\\foo.txt");
		assertThat(result).hasToString("file:/C:/Users/sub%20dir/foo.txt");
		result = URIUtil.resolve(URI.create("file:/C:/Users/jimbob/base.txt"), "C:/Users/sub dir/bar.txt");
		assertThat(result).hasToString("file:/C:/Users/sub%20dir/bar.txt");
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testResolveRelativeWindowsPath() throws Exception {
		URI result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "sub dir\\foo.txt");
		assertThat(result).hasToString("file:/C:/Users/jim/sub%20dir/foo.txt");
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void testResolveUNCWindowsPath() throws Exception {
		URI result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "\\\\server\\share\\foo.txt");
		assertThat(result).hasToString("file:////server/share/foo.txt");
		result = URIUtil.resolve(URI.create("file:/C:/Users/jim/base.txt"), "//server/share/foo.txt");
		assertThat(result).hasToString("file:////server/share/foo.txt");
	}

	@Test
	public void fromURI_returnsPath_platformIndependent(SoftAssertions softly) {
		fromURI_returnsPath("invalid url:", null, softly);
		fromURI_returnsPath("", null, softly);
		fromURI_returnsPath("http://some/URL", null, softly);
		fromURI_returnsPath("unknownsheme:invalid scheme:somepath", null, softly);
		fromURI_returnsPath("/some/path", "/some/path", softly);
		fromURI_returnsPath("some/path", "some/path", softly);
		fromURI_returnsPath("file:/some/path", "/some/path", softly);
		fromURI_returnsPath("reference:file:/some/path", "/some/path", softly);
		fromURI_returnsPath("reference:/some/path", "/some/path", softly);
		fromURI_returnsPath("file:/some/path", "/some/path", softly);
		fromURI_returnsPath("jar:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath("jar:file:/workspace/bnd/cache/mybundle.jar!/", "/workspace/bnd/cache/mybundle.jar",
			softly);
		fromURI_returnsPath("jar:file:/workspace/bnd/cache/mybundle.jar", "/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath("bundle:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath("bundle:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath("zip:file:/workspace/bnd/cache/mybundle.jar", "/workspace/bnd/cache/mybundle.jar", softly);
	}

	private static void fromURI_returnsPath(String uri, String expected, SoftAssertions softly) {
		Path ePath = expected == null ? null : Paths.get(expected);
		softly.assertThat(URIUtil.pathFromURI(uri))
			.as("uri '%s' maps to path '%s'", uri, ePath)
			.isEqualTo(Optional.ofNullable(ePath));
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void fromURI_returnsPath_onWindows(SoftAssertions softly) {
		fromURI_returnsPath("C:\\some\\path", "C:\\some\\path", softly);
		fromURI_returnsPath("D:\\some\\other\\path", "D:\\some\\other\\path", softly);
		fromURI_returnsPath("C:/some/path", "C:\\some\\path", softly);
		fromURI_returnsPath("file:/C:/some/path", "C:\\some\\path", softly);
		fromURI_returnsPath("reference:file:/X:/some/path", "X:\\some\\path", softly);
		fromURI_returnsPath("reference:X:/some/path", "X:\\some\\path", softly);
		fromURI_returnsPath("reference:X:\\some\\path", "X:\\some\\path", softly);
		fromURI_returnsPath("file://myserver/myshare/some/path", "\\\\myserver\\myshare\\some\\path", softly);
		fromURI_returnsPath("unknownsheme:file://someserver/somepath", "\\\\someserver\\somepath", softly);
		fromURI_returnsPath("jar:file:/C:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"C:\\workspace\\bnd\\cache\\mybundle.jar", softly);
		fromURI_returnsPath("jar:file:/C:/workspace/bnd/cache/mybundle.jar!/",
			"C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
		fromURI_returnsPath("jar:file:/C:/workspace/bnd/cache/mybundle.jar", "C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
		fromURI_returnsPath("bundle:file:/C:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"C:\\workspace\\bnd\\cache\\mybundle.jar", softly);
		fromURI_returnsPath("zip:file:/C:/workspace/bnd/cache/mybundle.jar", "C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
	}
}
