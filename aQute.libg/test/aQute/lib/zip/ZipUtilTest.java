package aQute.lib.zip;

import static aQute.lib.zip.ZipUtil.cleanPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.UncheckedIOException;

import org.junit.Test;

public class ZipUtilTest {

	void testCleanpath(String in, String out) {

		if (out != null) {
			assertThat(cleanPath(in)).isEqualTo(out);
			if (!in.isEmpty()) {
				assertThat(cleanPath("/" + in)).isEqualTo(out);
				assertThat(cleanPath("./" + in)).isEqualTo(out);
				assertThat(cleanPath("./././" + in)).isEqualTo(out);
				assertThat(cleanPath("./bar/../" + in)).isEqualTo(out);
			}
		} else {
			assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath(in));

		}
	}

	@Test
	public void testOkPaths() {

		testCleanpath("foo/bar/", "foo/bar/");
		testCleanpath("foo/bar", "foo/bar");

		testCleanpath("def/ghi/../ghi", "def/ghi");

		testCleanpath("", "");
		testCleanpath(".", "");
		testCleanpath("./", "/");
		testCleanpath("/././", "/");
		testCleanpath("///", "/");
		testCleanpath("/", "/");

		testCleanpath("x/..", "");
		testCleanpath("./x/..", "");
		testCleanpath("././././././x/..", "");
		testCleanpath("x/..x", "x/..x");
		testCleanpath("./x/y/z/.", "x/y/z");
		testCleanpath("./x/y./z/.", "x/y./z");
		testCleanpath("./x/.y/z/.", "x/.y/z");
		testCleanpath("./x/y.bar/z/.", "x/y.bar/z");

	}

	@Test
	public void testUnmodifiedPaths() {

		testCleanpath("abc/def/ghi", "abc/def/ghi");
		testCleanpath("abc/def../ghi", "abc/def../ghi");
		testCleanpath("abc/..def../ghi", "abc/..def../ghi");
		testCleanpath("abc/def./ghi", "abc/def./ghi");
		testCleanpath("abc/.def./ghi", "abc/.def./ghi");
		testCleanpath("abc/..def./ghi", "abc/..def./ghi");
		testCleanpath("abc/.def../ghi", "abc/.def../ghi");
	}

	@Test
	public void testPathsThatFailedOnPreviousCode() {

		testCleanpath("x/y/z/..", "x/y");
		testCleanpath("x/y/z/../..", "x");
		testCleanpath("./x/y/z/../..", "x");
		testCleanpath("./x/y/z/../../..", "");
		testCleanpath("./x/y/z/../..bbb/../..", "x");
		testCleanpath("./x/y/z/../..bbb../../..", "x");

		testCleanpath("./x/y/../..bbb/../..", "");
		testCleanpath("./x/y/../..bbb../../..", "");

	}

	@Test
	public void testBadPaths() {

		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath(
			"../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../../tmp/evil.txt"));

		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("./x/../.."));

		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("x/../../z/y"));
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("./x/../../z/y"));

		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("./x/../.."));
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("./x/./././././././../.."));
	}
}
