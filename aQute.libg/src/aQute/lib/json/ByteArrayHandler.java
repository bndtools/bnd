package aQute.lib.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import aQute.lib.base64.Base64;
import aQute.lib.hex.Hex;

/**
 * Will now use hex for encoding byte arrays
 */
public class ByteArrayHandler extends Handler {
	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException, Exception {
		StringHandler.string(app, Hex.toHexString((byte[]) object));
	}

	@Override
	public Object decodeArray(Decoder r) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ArrayList<Object> list = new ArrayList<>();
		r.codec.parseArray(list, Byte.class, r);
		for (Object b : list) {
			out.write(((Byte) b).byteValue());
		}
		return out.toByteArray();
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
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
		if (hex)
			return Hex.toByteArray(sb.toString());
		else
			return Base64.decodeBase64(sb.toString());
	}
}
