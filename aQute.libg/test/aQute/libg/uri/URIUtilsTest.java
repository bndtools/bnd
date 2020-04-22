package aQute.libg.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.net.URI;
import java.net.URISyntaxException;
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
	public void fromURIString_returnsPath_platformIndependent(SoftAssertions softly) {
		fromURIString_returnsPath("invalid url:", null, softly);
		fromURIString_returnsPath("", null, softly);
		fromURIString_returnsPath("http://some/URL", null, softly);
		fromURIString_returnsPath("unknownscheme:invalid scheme:somepath", null, softly);
		fromURIString_returnsPath("/some/path", "/some/path", softly);
		fromURIString_returnsPath("some/path", "some/path", softly);
		fromURIString_returnsPath("file:/some/path", "/some/path", softly);
		fromURIString_returnsPath("reference:file:/some/path", "/some/path", softly);
		fromURIString_returnsPath("reference:/some/path", "/some/path", softly);
		fromURIString_returnsPath("file:/some/path", "/some/path", softly);
		fromURIString_returnsPath(
			"jar:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURIString_returnsPath("jar:file:/workspace/bnd/cache/mybundle.jar!/",
			"/workspace/bnd/cache/mybundle.jar",
			softly);
		fromURIString_returnsPath("jar:file:/workspace/bnd/cache/mybundle.jar", "/workspace/bnd/cache/mybundle.jar",
			softly);
		fromURIString_returnsPath(
			"bundle:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURIString_returnsPath(
			"bundle:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURIString_returnsPath("zip:file:/workspace/bnd/cache/mybundle.jar", "/workspace/bnd/cache/mybundle.jar",
			softly);
	}

	private static void fromURIString_returnsPath(String uri, String expected, SoftAssertions softly) {
		Path ePath = expected == null ? null : Paths.get(expected);
		softly.assertThat(URIUtil.pathFromURI(uri))
			.as("uri '%s' maps to path '%s'", uri, ePath)
			.isEqualTo(Optional.ofNullable(ePath));
	}

	@Test
	@EnabledOnOs(WINDOWS)
	public void fromURIString_returnsPath_onWindows(SoftAssertions softly) {
		fromURIString_returnsPath("C:\\some\\path", "C:\\some\\path", softly);
		fromURIString_returnsPath("D:\\some\\other\\path", "D:\\some\\other\\path", softly);
		fromURIString_returnsPath("C:/some/path", "C:\\some\\path", softly);
		fromURIString_returnsPath("file:/C:/some/path", "C:\\some\\path", softly);
		fromURIString_returnsPath("reference:file:/X:/some/path", "X:\\some\\path", softly);
		fromURIString_returnsPath("reference:X:/some/path", "X:\\some\\path", softly);
		fromURIString_returnsPath("reference:X:\\some\\path", "X:\\some\\path", softly);
		fromURIString_returnsPath("file://myserver/myshare/some/path", "\\\\myserver\\myshare\\some\\path", softly);
		fromURIString_returnsPath("unknownscheme:file://someserver/somepath", "\\\\someserver\\somepath", softly);
		fromURIString_returnsPath(
			"jar:file:/C:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"C:\\workspace\\bnd\\cache\\mybundle.jar", softly);
		fromURIString_returnsPath(
			"jar:file:/C:/workspace/bnd/cache/mybundle.jar!/",
			"C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
		fromURIString_returnsPath("jar:file:/C:/workspace/bnd/cache/mybundle.jar",
			"C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
		fromURIString_returnsPath(
			"bundle:file:/C:/workspace/bnd/cache/mybundle.jar!/some/contained/element",
			"C:\\workspace\\bnd\\cache\\mybundle.jar", softly);
		fromURIString_returnsPath("zip:file:/C:/workspace/bnd/cache/mybundle.jar",
			"C:\\workspace\\bnd\\cache\\mybundle.jar",
			softly);
	}

	@Test
	public void fromURI_returnsPath_platformIndependent(SoftAssertions softly) throws URISyntaxException {
		fromURI_returnsPath(new URI("http://some/URL"), null, softly);
		fromURI_returnsPath(new URI("unknownscheme", "invalid scheme:somepath", null), null, softly);
		fromURI_returnsPath(new URI("/some/path"), "/some/path", softly);
		fromURI_returnsPath(new URI("some/path"), "some/path", softly);
		fromURI_returnsPath(new URI("file:/some/path"), "/some/path", softly);
		fromURI_returnsPath(new URI("reference:file:/some/path"), "/some/path", softly);
		fromURI_returnsPath(new URI("reference:/some/path"), "/some/path", softly);
		fromURI_returnsPath(new URI("file:/some/path"), "/some/path", softly);
		fromURI_returnsPath(
			new URI(
			"jar:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element"),
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath(new URI(
			"jar:file:/workspace/bnd/cache/mybundle.jar!/"),
			"/workspace/bnd/cache/mybundle.jar",
			softly);
		fromURI_returnsPath(new URI("jar:file:/workspace/bnd/cache/mybundle.jar"),
			"/workspace/bnd/cache/mybundle.jar",
			softly);
		fromURI_returnsPath(
			new URI(
			"bundle:file:/workspace/bnd/cache/mybundle.jar!/some/contained/element"),
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath(
			new URI(
			"bundle:/workspace/bnd/cache/mybundle.jar!/some/contained/element"),
			"/workspace/bnd/cache/mybundle.jar", softly);
		fromURI_returnsPath(new URI("zip:file:/workspace/bnd/cache/mybundle.jar"),
			"/workspace/bnd/cache/mybundle.jar",
			softly);
	}

	private static void fromURI_returnsPath(URI uri, String expected, SoftAssertions softly) {
		Path ePath = expected == null ? null : Paths.get(expected);
		softly.assertThat(URIUtil.pathFromURI(uri))
			.as("uri '%s' maps to path '%s'", uri, ePath)
			.isEqualTo(Optional.ofNullable(ePath));
	}

}
