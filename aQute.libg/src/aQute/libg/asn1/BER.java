package aQute.libg.asn1;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BER implements Types {
	final static DateFormat	df	= new SimpleDateFormat("yyyyMMddHHmmss\\Z");

	final DataInputStream	xin;
	long					position;

	public BER(InputStream in) {
		this.xin = new DataInputStream(in);
	}

	public void dump(PrintStream out) throws Exception {
		int type = readByte();
		long length = readLength();
		if (type == -1 || length == -1)
			throw new EOFException("Empty file");
		dump(out, type, length, "");
	}

	void dump(PrintStream out, int type, long length, String indent) throws Exception {
		int clss = type >> 6;
		int nmbr = type & 0x1F;
		boolean cnst = (type & 0x20) != 0;

		String tag = "[" + nmbr + "]";
		if (clss == 0)
			tag = TAGS[nmbr];

		if (cnst) {
			System.err.printf("%5d %s %s %s%n", length, indent, CLASSES[clss], tag);
			while (length > 1) {
				long atStart = getPosition();
				int t2 = read();
				long l2 = readLength();
				dump(out, t2, l2, indent + "  ");
				length -= getPosition() - atStart;
			}
		} else {
			assert length < Integer.MAX_VALUE;
			assert length >= 0;
			byte[] data = new byte[(int) length];
			readFully(data);
			String summary;

			switch (nmbr) {
				case BOOLEAN :
					assert length == 1;
					summary = data[0] != 0 ? "true" : "false";
					break;

				case INTEGER :
					long n = toLong(data);
					summary = n + "";
					break;

				case UTF8_STRING :
				case IA5STRING :
				case VISIBLE_STRING :
				case UNIVERSAL_STRING :
				case PRINTABLE_STRING :
				case UTCTIME :
					summary = new String(data, UTF_8);
					break;

				case OBJECT_IDENTIFIER :
					summary = readOID(data);
					break;

				case GENERALIZED_TIME :
				case GRAPHIC_STRING :
				case GENERAL_STRING :
				case CHARACTER_STRING :

				case REAL :
				case EOC :
				case BIT_STRING :
				case OCTET_STRING :
				case NULL :
				case OBJECT_DESCRIPTOR :
				case EXTERNAL :
				case ENUMERATED :
				case EMBEDDED_PDV :
				case RELATIVE_OID :
				case NUMERIC_STRING :
				case T61_STRING :
				case VIDEOTEX_STRING :
				case BMP_STRING :
				default :
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < 10 && i < data.length; i++) {
						sb.append(Integer.toHexString(data[i]));
					}
					if (data.length > 10) {
						sb.append("...");
					}
					summary = sb.toString();
					break;
			}
			out.printf("%5d %s %s %s %s\n", length, indent, CLASSES[clss], tag, summary);
		}
	}

	long toLong(byte[] data) {
		if (data[0] < 0) {
			for (int i = 0; i < data.length; i++)
				data[i] = (byte) (0xFF ^ data[i]);

			return -(toLong(data) + 1);
		}
		long n = 0;
		for (int i = 0; i < data.length; i++) {
			n = n * 256 + data[i];
		}
		return n;
	}

	/**
	 * 8.1.3.3 For the definite form, the length octets shall consist of one or
	 * more octets, and shall represent the number of octets in the contents
	 * octets using either the short form (see 8.1.3.4) or the long form (see
	 * 8.1.3.5) as a sender's option. NOTE – The short form can only be used if
	 * the number of octets in the contents octets is less than or equal to 127.
	 * 8.1.3.4 In the short form, the length octets shall consist of a single
	 * octet in which bit 8 is zero and bits 7 to 1 encode the number of octets
	 * in the contents octets (which may be zero), as an unsigned binary integer
	 * with bit 7 as the most significant bit. EXAMPLE L = 38 can be encoded as
	 * 001001102 8.1.3.5 In the long form, the length octets shall consist of an
	 * initial octet and one or more subsequent octets. The initial octet shall
	 * be encoded as follows: a) bit 8 shall be one; b) bits 7 to 1 shall encode
	 * the number of subsequent octets in the length octets, as an unsigned
	 * binary integer with bit 7 as the most significant bit; c) the value
	 * 111111112 shall not be used. ISO/IEC 8825-1:2003 (E) NOTE 1 – This
	 * restriction is introduced for possible future extension. Bits 8 to 1 of
	 * the first subsequent octet, followed by bits 8 to 1 of the second
	 * subsequent octet, followed in turn by bits 8 to 1 of each further octet
	 * up to and including the last subsequent octet, shall be the encoding of
	 * an unsigned binary integer equal to the number of octets in the contents
	 * octets, with bit 8 of the first subsequent octet as the most significant
	 * bit. EXAMPLE L = 201 can be encoded as: 100000012 110010012 NOTE 2 – In
	 * the long form, it is a sender's option whether to use more length octets
	 * than the minimum necessary. 8.1.3.6 For the indefinite form, the length
	 * octets indicate that the contents octets are terminated by
	 * end-of-contents octets (see 8.1.5), and shall consist of a single octet.
	 * 8.1.3.6.1 The single octet shall have bit 8 set to one, and bits 7 to 1
	 * set to zero. 8.1.3.6.2 If this form of length is used, then
	 * end-of-contents octets (see 8.1.5) shall be present in the encoding
	 * following the contents octets. 8.1.4 Contents octets The contents octets
	 * shall consist of zero, one or more octets, and shall encode the data
	 * value as specified in subsequent clauses. NOTE – The contents octets
	 * depend on the type of the data value; subsequent clauses follow the same
	 * sequence as the definition of types in ASN.1. 8.1.5 End-of-contents
	 * octets The end-of-contents octets shall be present if the length is
	 * encoded as specified in 8.1.3.6, otherwise they shall not be present. The
	 * end-of-contents octets shall consist of two zero octets. NOTE – The
	 * end-of-contents octets can be considered as the encoding of a value whose
	 * tag is universal class, whose form is primitive, whose number of the tag
	 * is zero, and whose contents are absent, thus: End-of-contents Length
	 * Contents 0016 0016 Absent
	 */
	private long readLength() throws IOException {
		long n = readByte();
		if (n > 0) {
			// short form
			return n;
		}
		// long form
		int count = (int) (n & 0x7F);
		if (count == 0) {
			// indefinite form
			return 0;
		}
		n = 0;
		while (count-- > 0) {
			n = n * 256 + read();
		}
		return n;
	}

	private int readByte() throws IOException {
		position++;
		return xin.readByte();
	}

	private void readFully(byte[] data) throws IOException {
		position += data.length;
		xin.readFully(data);
	}

	private long getPosition() {
		return position;
	}

	private int read() throws IOException {
		position++;
		return xin.read();
	}

	String readOID(byte[] data) {
		StringBuilder sb = new StringBuilder();
		sb.append((0xFF & data[0]) / 40);
		sb.append(".");
		sb.append((0xFF & data[0]) % 40);

		int i = 0;
		while (++i < data.length) {
			int n = 0;
			while (data[i] < 0) {
				n = n * 128 + (0x7F & data[i]);
				i++;
			}
			n = n * 128 + data[i];
			sb.append(".");
			sb.append(n);
		}

		return sb.toString();
	}

	int getPayloadLength(PDU pdu) throws Exception {
		switch (pdu.getTag() & 0x1F) {
			case EOC :
				return 1;

			case BOOLEAN :
				return 1;

			case INTEGER :
				return size(pdu.getInt());

			case UTF8_STRING :
				String s = pdu.getString();
				byte[] encoded = s.getBytes(UTF_8);
				return encoded.length;

			case IA5STRING :
			case VISIBLE_STRING :
			case UNIVERSAL_STRING :
			case PRINTABLE_STRING :
			case GENERALIZED_TIME :
			case GRAPHIC_STRING :
			case GENERAL_STRING :
			case CHARACTER_STRING :
			case UTCTIME :
			case NUMERIC_STRING : {
				String str = pdu.getString();
				encoded = str.getBytes("ASCII");
				return encoded.length;
			}

			case OBJECT_IDENTIFIER :
			case REAL :
			case BIT_STRING :
				return pdu.getBytes().length;

			case OCTET_STRING :
			case NULL :
			case OBJECT_DESCRIPTOR :
			case EXTERNAL :
			case ENUMERATED :
			case EMBEDDED_PDV :
			case RELATIVE_OID :
			case T61_STRING :
			case VIDEOTEX_STRING :
			case BMP_STRING :
				return pdu.getBytes().length;

			default :
				throw new IllegalArgumentException("Invalid type: " + pdu);
		}
	}

	int size(long value) {
		if (value < 128)
			return 1;

		if (value <= 0xFF)
			return 2;

		if (value <= 0xFFFF)
			return 3;

		if (value <= 0xFFFFFF)
			return 4;

		if (value <= 0xFFFFFFFF)
			return 5;

		if (value <= 0xFFFFFFFFFFL)
			return 6;

		if (value <= 0xFFFFFFFFFFFFL)
			return 7;

		if (value <= 0xFFFFFFFFFFFFFFL)
			return 8;

		if (value <= 0xFFFFFFFFFFFFFFFFL)
			return 9;

		throw new IllegalArgumentException("length too long");
	}

	public void write(OutputStream out, PDU pdu) throws Exception {
		byte id = 0;

		switch (pdu.getClss()) {
			case UNIVERSAL :
				id |= 0;
				break;
			case APPLICATION :
				id |= 0x40;
				break;
			case CONTEXT :
				id |= 0x80;
				break;
			case PRIVATE :
				id |= 0xC0;
				break;
		}

		if (pdu.isConstructed())
			id |= 0x20;

		int tag = pdu.getTag();
		if (tag >= 0 && tag < 31) {
			id |= tag;
		} else {
			throw new UnsupportedOperationException("Cant do tags > 30");
		}

		out.write(id);

		int length = getPayloadLength(pdu);
		int size = size(length);
		if (size == 1) {
			out.write(length);
		} else {
			out.write(size);
			while (--size >= 0) {
				byte data = (byte) ((length >> (size * 8)) & 0xFF);
				out.write(data);
			}
		}
		writePayload(out, pdu);
	}

	void writePayload(OutputStream out, PDU pdu) throws Exception {
		switch (pdu.getTag()) {
			case EOC :
				out.write(0);
				break;

			case BOOLEAN :
				if (pdu.getBoolean())
					out.write(-1);
				else
					out.write(0);
				break;

			case ENUMERATED :
			case INTEGER : {
				long value = pdu.getInt();
				int size = size(value);
				for (int i = size; i >= 0; i--) {
					byte b = (byte) ((value >> (i * 8)) & 0xFF);
					out.write(b);
				}
			}

			case BIT_STRING : {
				byte bytes[] = pdu.getBytes();
				int unused = bytes[0];
				assert unused <= 7;
				int[] mask = {
					0xFF, 0x7F, 0x3F, 0x1F, 0xF, 0x7, 0x3, 0x1
				};
				bytes[bytes.length - 1] &= (byte) mask[unused];
				out.write(bytes);
				break;
			}

			case RELATIVE_OID :
			case OBJECT_IDENTIFIER : {
				int[] oid = pdu.getOID();
				assert oid.length > 2;
				assert oid[0] < 4;
				assert oid[1] < 40;
				byte top = (byte) (oid[0] * 40 + oid[1]);
				out.write(top);
				for (int i = 2; i < oid.length; i++) {
					putOid(out, oid[i]);
				}
				break;
			}

			case OCTET_STRING : {
				byte bytes[] = pdu.getBytes();
				out.write(bytes);
				break;
			}

			case NULL :
				break;

			case BMP_STRING :
			case GRAPHIC_STRING :
			case VISIBLE_STRING :
			case GENERAL_STRING :
			case UNIVERSAL_STRING :
			case CHARACTER_STRING :
			case NUMERIC_STRING :
			case PRINTABLE_STRING :
			case VIDEOTEX_STRING :
			case T61_STRING :
			case REAL :
			case EMBEDDED_PDV :
			case EXTERNAL :
				throw new UnsupportedEncodingException("dont know real, embedded PDV or external");

			case UTF8_STRING : {
				String s = pdu.getString();
				byte[] data = s.getBytes(UTF_8);
				out.write(data);
				break;
			}

			case OBJECT_DESCRIPTOR :
			case IA5STRING :
				String s = pdu.getString();
				byte[] data = s.getBytes("ASCII");
				out.write(data);
				break;

			case SEQUENCE :
			case SET : {
				PDU pdus[] = pdu.getChildren();
				for (PDU p : pdus) {
					write(out, p);
				}
			}

			case UTCTIME :
			case GENERALIZED_TIME :
				Date date = pdu.getDate();
				synchronized (df) {
					String ss = df.format(date);
					byte d[] = ss.getBytes("ASCII");
					out.write(d);
				}
				break;

		}
	}

	private void putOid(OutputStream out, int i) throws IOException {
		if (i > 127) {
			putOid(out, i >> 7);
			out.write(0x80 + (i & 0x7F));
		} else
			out.write(i & 0x7F);
	}
}
