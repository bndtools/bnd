package aQute.lib.hex;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;

import aQute.lib.base64.Base64;
import junit.framework.TestCase;

public class HexTest extends TestCase {

	static final char	ns[]	= {
		' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'A', 'B', 'C', 'D',
		'E', 'F', 'G', '_'
	};
	static final int	nsr[]	= {
		-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, -1, 10, 11, 12, 13, 14, 15, -1, -1
	};

	public static void testNibble() {
		for (int i = 0; i < ns.length; i++) {
			try {
				assertEquals(nsr[i], Hex.nibble(ns[i]));
			} catch (IllegalArgumentException e) {
				assertEquals("Did not get an exception for nibble \"" + ns[i] + "\"", -1, nsr[i]);
			}
		}
	}

	public static void testHexOddCount() {
		boolean ex = false;
		byte[] bytes = null;
		try {
			Hex.toByteArray("b10a8db164e0754105b7a99be72e3fe");
		} catch (IllegalArgumentException e) {
			ex = true;
		}
		assertTrue(ex);
		assertNull(bytes);
	}

	public static void testHex() {
		byte[] bytes = Hex.toByteArray("b10a8db164e0754105b7a99be72e3fe5");
		String s = Hex.toHexString(bytes);
		assertEquals("b10a8db164e0754105b7a99be72e3fe5", s.toLowerCase());
	}

	public static void testBase64() {
		assertEquals("", Base64.encodeBase64(new byte[] {}));
		assertEquals("MQ==", Base64.encodeBase64(new byte[] {
			'1'
		}));
		assertEquals("MTI=", Base64.encodeBase64(new byte[] {
			'1', '2'
		}));
		assertEquals("MTIz", Base64.encodeBase64(new byte[] {
			'1', '2', '3'
		}));
		assertEquals("MTIzNA==", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4'
		}));
		assertEquals("MTIzNDU=", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5'
		}));
		assertEquals("MTIzNDU2", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6'
		}));
		assertEquals("MTIzNDU2Nw==", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6', '7'
		}));
		assertEquals("MTIzNDU2Nzg=", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6', '7', '8'
		}));
		assertEquals("MTIzNDU2Nzg5", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6', '7', '8', '9'
		}));
		assertEquals("MTIzNDU2Nzg5AA==", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6', '7', '8', '9', 0
		}));
		assertEquals("MTIzNDU2Nzg59g==", Base64.encodeBase64(new byte[] {
			'1', '2', '3', '4', '5', '6', '7', '8', '9', -10
		}));

		assertTrue(Arrays.equals(new byte[0], Base64.decodeBase64("")));
		assertTrue(Arrays.equals(new byte[] {
			'1'
		}, Base64.decodeBase64("MQ==")));
		assertTrue(Arrays.equals(new byte[] {
			'1', '2', '3', '4', '5', '6', '7'
		}, Base64.decodeBase64("MTIzNDU2Nw==")));
		assertTrue(Arrays.equals(new byte[] {
			'1', '2', '3', '4', '5', '6', '7', '8', '9', -10
		}, Base64.decodeBase64("MTIzNDU2Nzg59g==")));
	}

	final static String EOL = String.format("%n");

	public void testFormatEmpty() {
		byte[] input = new byte[] {};
		assertThat(Hex.format(input)).isEqualTo("")
			.isEqualTo(Hex.format(ByteBuffer.allocate(0)));
	}

	public void testNull() {
		assertThat(Hex.format((byte[]) null)).isEmpty();
		assertThat(Hex.format((ByteBuffer) null)).isEmpty();
	}

	public void testAscii() {
		byte[] input = new byte[] {
			-1, 'A', 'a', '~', ' ', '\u007F'
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  FF 41 61 7E 20 7F                                 .Aa~ ." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));
	}

	public void testFormatOne() {
		byte[] input = new byte[] {
			-1
		};
		assertThat(Hex.format(input)).isEqualTo("0x0000  FF                                                ." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));
	}

	public void testFormatSeven() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06                              ......." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));
	}

	public void testFormatEight() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06 07                           ........" + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));
	}

	public void testFormatNine() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06 07  08                       ........  ." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));
	}

	public void testFormatFifteen() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E     ........  ......." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));

	}

	public void testFormatSixteen() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F  ........  ........" + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));

	}

	public void testFormatSeventeen() {
		byte[] input = new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
		};
		assertThat(Hex.format(input))
			.isEqualTo("0x0000  00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F  ........  ........" + EOL + //
			/*         */ "0x0010  10                                                ." + EOL)
			.isEqualTo(Hex.format(ByteBuffer.wrap(input)));

	}

	public void testHexAppend() {
		StringBuilder sb = new StringBuilder();

		Hex.append(sb, (byte) 0xA5);
		sb.append("-");

		Hex.append(sb, (short) 0xA5A4);
		sb.append("-");

		Hex.append(sb, (short) -10);
		sb.append("-");

		Hex.append(sb, 'A');
		sb.append("-");

		Hex.append(sb, 0x1234567);
		sb.append("-");

		Hex.append(sb, 0x123456789ABCDEFL);

		assertThat(sb.toString()).isEqualTo("A5-A5A4-FFF6-0041-01234567-0123456789ABCDEF");

	}
}
