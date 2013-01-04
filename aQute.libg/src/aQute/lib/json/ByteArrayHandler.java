package aQute.lib.json;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.base64.*;
import aQute.lib.hex.*;

/**
 * Will now use hex for encoding byte arrays
 */
public class ByteArrayHandler extends Handler {
	Pattern	ENCODING	= Pattern
								.compile("((:?[\\dA-Za-z][\\dA-Za-z])*)|((:?ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/)+={1,3})");

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws IOException, Exception {
		StringHandler.string(app, Hex.toHexString((byte[]) object));
	}

	@Override
	Object decodeArray(Decoder r) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ArrayList<Object> list = new ArrayList<Object>();
		r.codec.parseArray(list, Byte.class, r);
		for (Object b : list) {
			out.write(((Byte) b).byteValue());
		}
		return out.toByteArray();
	}

	@Override
	Object decode(Decoder dec, String s) throws Exception {
		boolean hex = true;
		StringBuilder sb = new StringBuilder(s);
		for (int i = sb.length() - 1; i >= 0; i--) {
			char c = sb.charAt(i);
			if (Character.isWhitespace(c))
				sb.delete(i, i + 1);
			else {
				switch (c) {
					case '0' :
					case '1' :
					case '2' :
					case '3' :
					case '4' :
					case '5' :
					case '6' :
					case '7' :
					case '8' :
					case '9' :
					case 'A' :
					case 'B' :
					case 'C' :
					case 'D' :
					case 'E' :
					case 'F' :
					case 'a' :
					case 'b' :
					case 'c' :
					case 'd' :
					case 'e' :
					case 'f' :
						break;

					default :
						hex = false;
						break;
				}
			}
		}
		if ( hex)
			return Hex.toByteArray(sb.toString());
		else
			return Base64.decodeBase64(sb.toString());
	}
}
