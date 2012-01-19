package aQute.lib.base64;

import java.io.*;

/*
 * Base 64 converter.
 * 
 * TODO Implement string to byte[]
 */
public class Base64 {
	byte[]				data;

	static final String	alphabet	= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	static byte[]		values		= new byte[128];

	static {
		for (int i = 0; i < values.length; i++) {
			values[i] = -1;
		}
		// Create reverse index
		for (int i = 0; i < alphabet.length(); i++) {
			char c = alphabet.charAt(i);
			values[c] = (byte) i;
		}
	}

	public Base64(byte data[]) {
		this.data = data;
	}

	public final static byte[] decodeBase64(String string) {
		string = string.trim();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int register = 0;
		int i = 0;
		int pads = 0;

		byte test[] = new byte[3];

		while (i < string.length()) {
			char c = string.charAt(i);
			if (c > 0x7F)
				throw new IllegalArgumentException(
						"Invalid base64 character in " + string
								+ ", character value > 128 ");
			
			int v = 0;
			if ( c == '=' ) {
				pads++;
			} else {
				v = values[c];
				if ( v < 0 )
					throw new IllegalArgumentException(
							"Invalid base64 character in " + string + ", " + c );
			}					
			register <<= 6;
			register |= v;
			test[2] = (byte) (register & 0xFF);
			test[1] = (byte) ((register >> 8) & 0xFF);
			test[0] = (byte) ((register >> 16) & 0xFF);

			i++;

			if ((i % 4) == 0) {
				flush(out, register, pads);
				register = 0;
				pads = 0;
			}
		}
		return out.toByteArray();
	}

	static private void flush(ByteArrayOutputStream out, int register, int pads) {
		switch (pads) {
		case 0:
			out.write(0xFF & (register >> 16));
			out.write(0xFF & (register >> 8));
			out.write(0xFF & (register >> 0));
			break;
			
		case 1:
			out.write(0xFF & (register >> 16));
			out.write(0xFF & (register >> 8));
			break;
			
		case 2:
			out.write(0xFF & (register >> 16));
		}
	}

	public Base64(String s) {
		data = decodeBase64(s);
	}

	public String toString() {
		return encodeBase64(data);
	}

	public static String encodeBase64(byte data[]) {
		StringBuilder sb = new StringBuilder();
		int buf = 0;
		int bits = 0;
		int n = 0;

		while (true) {
			if (bits >= 6) {
				bits -= 6;
				int v = 0x3F & (buf >> bits);
				sb.append(alphabet.charAt(v));
			} else {
				if (n >= data.length)
					break;

				buf <<= 8;
				buf |= 0xFF & data[n++];
				bits += 8;
			}
		}
		if (bits != 0) // must be less than 7
			sb.append(alphabet.charAt(0x3F & (buf << (6 - bits))));

		int mod = 4 - (sb.length() % 4);
		if (mod != 4) {
			for (int i = 0; i < mod; i++)
				sb.append('=');
		}
		return sb.toString();
	}

	public Object toData() {
		return data;
	}

}
