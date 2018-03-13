package aQute.lib.hex;

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
}
