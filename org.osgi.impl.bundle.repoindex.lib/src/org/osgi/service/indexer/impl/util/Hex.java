package org.osgi.service.indexer.impl.util;

public class Hex {

	private final static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public final static String toHexString(byte data[]) {
		StringBuilder sb = new StringBuilder();
		append(sb, data);
		return sb.toString();
	}

	public final static void append(StringBuilder sb, byte[] data) {
		for (int i = 0; i < data.length; i++) {
			sb.append(nibble(data[i] >> 4));
			sb.append(nibble(data[i]));
		}
	}

	private final static char nibble(int i) {
		return HEX[i & 0xF];
	}
}
