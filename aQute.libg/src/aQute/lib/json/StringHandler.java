package aQute.lib.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class StringHandler extends Handler {

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws IOException {
		string(app, object.toString());
	}

	static void string(Appendable app, String s) throws IOException {

		app.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' :
					app.append("\\\"");
					break;

				case '\\' :
					app.append("\\\\");
					break;

				case '\b' :
					app.append("\\b");
					break;

				case '\f' :
					app.append("\\f");
					break;

				case '\n' :
					app.append("\\n");
					break;

				case '\r' :
					app.append("\\r");
					break;

				case '\t' :
					app.append("\\t");
					break;

				default :
					if (Character.isISOControl(c)) {
						app.append("\\u");
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 12)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 8)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 4)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 0)));
					} else
						app.append(c);
			}
		}
		app.append('"');
	}

	@Override
	public Object decode(Decoder dec, String s) throws Exception {
		return s;
	}

	@Override
	public Object decode(Decoder dec, Number s) {
		return s.toString();
	}

	@Override
	public Object decode(Decoder dec, boolean s) {
		return Boolean.toString(s);
	}

	@Override
	public Object decode(Decoder dec) {
		return null;
	}

	/**
	 * An object can be assigned to a string. This means that the stream is
	 * interpreted as the object but stored in its complete in the string.
	 */
	@Override
	public Object decodeObject(Decoder r) throws Exception {
		return collect(r, '}');
	}

	/**
	 * An array can be assigned to a string. This means that the stream is
	 * interpreted as the array but stored in its complete in the string.
	 */
	@Override
	public Object decodeArray(Decoder r) throws Exception {
		return collect(r, ']');
	}

	/**
	 * Gather the input until you find the the closing character making sure
	 * that new blocks are are take care of.
	 * <p>
	 * This method parses the input for a complete block so that it can be
	 * stored in a string. This allows envelopes.
	 *
	 * @param isr
	 * @param c
	 * @throws Exception
	 */
	private Object collect(Decoder isr, @SuppressWarnings("unused") char close) throws Exception {
		boolean instring = false;
		int level = 1;
		StringBuilder sb = new StringBuilder();

		int c = isr.current();
		while (c > 0 && level > 0) {
			sb.append((char) c);
			if (instring)
				switch (c) {
					case '"' :
						instring = true;
						break;

					case '[' :
					case '{' :
						level++;
						break;

					case ']' :
					case '}' :
						level--;
						break;
				}
			else
				switch (c) {
					case '"' :
						instring = false;
						break;

					case '\\' :
						sb.append((char) isr.read());
						break;
				}

			c = isr.read();
		}
		return sb.toString();
	}

}
