package aQute.libg.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

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
}
