package aQute.lib.zip;

import static aQute.lib.zip.ZipUtil.cleanPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

public class ZipUtilTest {

	void testCleanpath(String in, String out) {

		if (out != null) {
			assertThat(cleanPath(in)).isEqualTo(out);
			if (!in.isEmpty()) {
				assertThat(cleanPath("/" + in)).isEqualTo(out);
				assertThat(cleanPath("./" + in)).isEqualTo(out);
				assertThat(cleanPath("./././" + in)).isEqualTo(out);
				assertThat(cleanPath("./bar/../" + in)).isEqualTo(out);
				assertThat(cleanPath("./bar/baz/../../" + in)).isEqualTo(out);
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
		testCleanpath("abc/.def..../ghi", "abc/.def..../ghi");
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

		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> cleanPath("./x/./././././././../.."));
	}

	@Test
	public void extraFieldFromString_one_value() {
		byte[] testfield = ZipUtil.extraFieldFromString(null, "test value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's', 't', ' ', 'v', 'a',
			'l', 'u', 'e');
	}

	@Test
	public void extraFieldFromString_replace_value_first() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.put(testvalue)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.array();

		assertThat(testfield).containsExactly(0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's', 't', ' ', 'v', 'a',
			'l', 'u', 'e', 0xFE, 0xCA, 0, 0);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0);
	}

	@Test
	public void extraFieldFromString_replace_value_last() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.put(testvalue)
			.array();

		assertThat(testfield).containsExactly(0xFE, 0xCA, 0, 0, 0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's',
			't', ' ', 'v', 'a', 'l', 'u', 'e');
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0);
	}

	@Test
	public void extraFieldFromString_replace_value_middle() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 5)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xB1FF)
			.putShort((short) 2)
			.put((byte) 1)
			.put((byte) 2)
			.put(testvalue)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.array();

		assertThat(testfield).containsExactly(0xFF, 0xB1, 2, 0, 1, 2, 0xEA, 0xBD, "test value".length(), 0, 't', 'e',
			's', 't', ' ', 'v', 'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFF, 0xB1, 2, 0, 1, 2, 0xFE, 0xCA, 0, 0);
	}

	@Test
	public void extraFieldFromString_replace_value_first_invalid() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 5)
			.order(ByteOrder.LITTLE_ENDIAN)
			.put(testvalue)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.putShort((short) 0xB1FF)
			.putShort((short) 20)
			.put((byte) 1)
			.put((byte) 2)
			.array();

		assertThat(testfield).containsExactly(0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's', 't', ' ', 'v', 'a',
			'l', 'u', 'e', 0xFE, 0xCA, 0, 0, 0xFF, 0xB1, 20, 0, 1, 2);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0, 0xFF, 0xB1, 20, 0, 1, 2);
	}

	@Test
	public void extraFieldFromString_replace_value_middle_invalid() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 5)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.put(testvalue)
			.putShort((short) 0xB1FF)
			.putShort((short) 20)
			.put((byte) 1)
			.put((byte) 2)
			.array();

		assertThat(testfield).containsExactly(0xFE, 0xCA, 0, 0, 0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's',
			't', ' ', 'v', 'a', 'l', 'u', 'e', 0xFF, 0xB1, 20, 0, 1, 2);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0, 0xFF, 0xB1, 20, 0, 1, 2);
	}

	@Test
	public void extraFieldFromString_add_value() {
		byte[] testfield = ByteBuffer.allocate(Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.array();

		assertThat(testfield).containsExactly(0xFE, 0xCA, 0, 0);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0);
	}

	@Test
	public void extraFieldFromString_add_value_invalid() {
		byte[] testfield = ByteBuffer.allocate(Short.BYTES * 5)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.putShort((short) 0xB1FF)
			.putShort((short) 20)
			.put((byte) 1)
			.put((byte) 2)
			.array();

		assertThat(testfield).containsExactly(0xFE, 0xCA, 0, 0, 0xFF, 0xB1, 20, 0, 1, 2);
		testfield = ZipUtil.extraFieldFromString(testfield, "newer value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "newer value".length(), 0, 'n', 'e', 'w', 'e', 'r', ' ', 'v',
			'a', 'l', 'u', 'e', 0xFE, 0xCA, 0, 0, 0xFF, 0xB1, 20, 0, 1, 2);
	}

	@Test
	public void stringFromExtraField_plainUTF8() {
		byte[] testfield = "test value".getBytes(UTF_8);
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_one() {
		byte[] testfield = ZipUtil.extraFieldFromString(new byte[0], "test value");
		assertThat(testfield).containsExactly(0xEA, 0xBD, "test value".length(), 0, 't', 'e', 's', 't', ' ', 'v', 'a',
			'l', 'u', 'e');
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_first() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.put(testvalue)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.array();
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_middle() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 5)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xB1FF)
			.putShort((short) 2)
			.put((byte) 1)
			.put((byte) 2)
			.put(testvalue)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.array();
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_last() {
		byte[] testvalue = ZipUtil.extraFieldFromString(null, "test value");
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.put(testvalue)
			.array();
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_invalid() {
		byte[] testfield = "test value".getBytes(UTF_8);
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}

	@Test
	public void stringFromExtraField_invalid_last() {
		byte[] testvalue = "test value".getBytes(UTF_8);
		byte[] testfield = ByteBuffer.allocate(testvalue.length + Short.BYTES * 2)
			.order(ByteOrder.LITTLE_ENDIAN)
			.putShort((short) 0xCAFE)
			.putShort((short) 0)
			.put(testvalue)
			.array();
		assertThat(ZipUtil.stringFromExtraField(testfield)).isEqualTo("test value");
	}
}
