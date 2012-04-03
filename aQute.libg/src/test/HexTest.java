package test;

import aQute.lib.hex.*;
import junit.framework.*;

public class HexTest extends TestCase {

	public void testHex() {
		byte[] bytes = Hex.toByteArray("b10a8db164e0754105b7a99be72e3fe5");
		String s = Hex.toHexString(bytes);
		assertEquals( "b10a8db164e0754105b7a99be72e3fe5", s.toLowerCase());
	}
}
