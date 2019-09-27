package aQute.lib.base64;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.regex.Pattern;

/*
 * Base 64 converter.
 *
 */
public class Base64 {
	private static final int		DEFAULT_MAX_INPUT_LENGTH	= 65000;

	byte[]							data;

	private static final String		alphabet					= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final byte[]		values						= new byte[128];
	private static final String		BASE64_C					= "[\\p{Alnum}+/]";
	private static final Pattern	BASE64_P					= Pattern
		.compile("(?:" + BASE64_C + "{4})*(?:" + BASE64_C + "{2}==|" + BASE64_C + "{3}=)?");

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
		try {
			return decodeBase64(new StringReader(string));
		} catch (IOException e) {
			// cannot get IO exceptions on String
			return null;
		}
	}

	public static byte[] decodeBase64(Reader rdr) throws IOException {
		return decodeBase64(rdr, DEFAULT_MAX_INPUT_LENGTH);
	}

	public static byte[] decodeBase64(Reader rdr, int maxLength) throws IOException {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream(maxLength);
			decode(rdr, bout, maxLength);
			return bout.toByteArray();
		} finally {
			rdr.close();
		}
	}

	public static byte[] decodeBase64(InputStream in) throws IOException {
		return decodeBase64(in, DEFAULT_MAX_INPUT_LENGTH);
	}

	public static byte[] decodeBase64(InputStream in, int maxLength) throws IOException {
		return decodeBase64(new BufferedReader(new InputStreamReader(in, US_ASCII)), maxLength);
	}

	public final static byte[] decodeBase64(File file) throws IOException {
		if (file.length() > Integer.MAX_VALUE)
			throw new IllegalArgumentException("File " + file + " is >4Gb for base 64 decoding");
		try {
			return decodeBase64(Files.newBufferedReader(file.toPath(), US_ASCII), (int) file.length() * 2 / 3);
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException(iae.getMessage() + ": " + file, iae);
		}
	}

	public final static void decode(Reader rdr, OutputStream out) throws IOException {
		decode(rdr, out, DEFAULT_MAX_INPUT_LENGTH);
	}

	public final static void decode(Reader rdr, OutputStream out, int maxLength) throws IOException {
		int register = 0;
		int i = 0;
		int pads = 0;

		byte test[] = new byte[3];
		for (int c; (c = rdr.read()) >= 0;) {

			maxLength--;
			if (maxLength < 0)
				throw new IllegalArgumentException("Input stream for base64 decoding is too large");

			if (Character.isWhitespace(c) || c == '\r' || c == '\n')
				continue;

			if (c > 0x7F)
				throw new IllegalArgumentException("Invalid base64 character in " + rdr + ", character value > 128 ");

			int v = 0;
			if (c == '=') {
				pads++;
			} else {
				v = values[c];
				if (v < 0)
					throw new IllegalArgumentException("Invalid base64 character in " + rdr + ", " + c);
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
	}

	static private void flush(OutputStream out, int register, int pads) throws IOException {
		switch (pads) {
			case 0 :
				out.write(0xFF & (register >> 16));
				out.write(0xFF & (register >> 8));
				out.write(0xFF & (register >> 0));
				break;

			case 1 :
				out.write(0xFF & (register >> 16));
				out.write(0xFF & (register >> 8));
				break;

			case 2 :
				out.write(0xFF & (register >> 16));
		}
	}

	public Base64(String s) {
		data = decodeBase64(s);
	}

	@Override
	public String toString() {
		return encodeBase64(data);
	}

	public static String encodeBase64(InputStream in) throws IOException {
		StringWriter sw = new StringWriter();
		encode(in, sw);
		return sw.toString();
	}

	public static String encodeBase64(File in) throws IOException {
		StringWriter sw = new StringWriter();
		encode(in, sw);
		return sw.toString();
	}

	public static String encodeBase64(byte data[]) {
		StringWriter sw = new StringWriter();
		ByteArrayInputStream bin = new ByteArrayInputStream(data);
		try {
			encode(bin, sw);
		} catch (IOException e) {
			// can't happen
		}
		return sw.toString();
	}

	public Object toData() {
		return data;
	}

	public static void encode(File in, Appendable sb) throws IOException {
		if (in.length() > Integer.MAX_VALUE)
			throw new IllegalArgumentException("File > 4Gb " + in);

		encode(new BufferedInputStream(Files.newInputStream(in.toPath())), sb, (int) in.length());
	}

	public static void encode(InputStream in, Appendable sb) throws IOException {
		encode(in, sb, DEFAULT_MAX_INPUT_LENGTH);
	}

	public static void encode(InputStream in, Appendable sb, int maxLength) throws IOException {
		try {
			// StringBuilder sb = new StringBuilder();
			int buf = 0;
			int bits = 0;
			int out = 0;

			while (true) {
				if (bits >= 6) {
					bits -= 6;
					int v = 0x3F & (buf >> bits);
					sb.append(alphabet.charAt(v));
					out++;
				} else {
					int c = in.read();
					if (c < 0)
						break;

					maxLength--;
					if (maxLength < 0)
						throw new IllegalArgumentException("Length (" + maxLength + ") for base 64 encode exceeded");

					buf <<= 8;
					buf |= 0xFF & c;
					bits += 8;
				}
			}
			if (bits != 0) {// must be less than 7
				sb.append(alphabet.charAt(0x3F & (buf << (6 - bits))));
				out++;
			}
			int mod = 4 - (out % 4);
			if (mod != 4) {
				for (int i = 0; i < mod; i++)
					sb.append('=');
			}
		} finally {
			in.close();
		}
	}

	public static boolean isBase64(String value) {
		return BASE64_P.matcher(value)
			.matches();
	}

}
