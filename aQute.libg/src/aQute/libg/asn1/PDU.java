package aQute.libg.asn1;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;

public class PDU implements Types, Iterable<PDU> {
	final int		identifier;
	final Object	payload;
	byte			data[]	= new byte[100];

	public PDU(int id, Object payload) {
		identifier = id;
		this.payload = payload;
	}

	public PDU(Date payload) {
		identifier = UTCTIME;
		this.payload = payload;
	}

	public PDU(int n) {
		this(UNIVERSAL + INTEGER, n);
	}

	public PDU(boolean value) {
		this(UNIVERSAL + BOOLEAN, value);
	}

	public PDU(String s) throws Exception {
		this(UNIVERSAL + IA5STRING, s);
	}

	public PDU(byte[] data) {
		this(UNIVERSAL + OCTET_STRING, data);
	}

	public PDU(BitSet bits) {
		this(UNIVERSAL + BIT_STRING, bits);
	}

	public PDU(int top, int l1, int... remainder) {
		identifier = UNIVERSAL + OBJECT_IDENTIFIER;
		int[] ids = new int[remainder.length + 2];
		ids[0] = top;
		ids[1] = l1;
		System.arraycopy(remainder, 0, ids, 2, remainder.length);
		payload = ids;
	}

	public PDU(int tag, PDU... set) {
		this(tag, (Object) set);
	}

	public PDU(PDU... set) {
		this(SEQUENCE + CONSTRUCTED, set);
	}

	public int getTag() {
		return identifier & TAGMASK;
	}

	int getClss() {
		return identifier & CLASSMASK;
	}

	public boolean isConstructed() {
		return (identifier & CONSTRUCTED) != 0;
	}

	public String getString() {
		return (String) payload;
	}

	@Override
	public Iterator<PDU> iterator() {
		return Arrays.asList((PDU[]) payload)
			.iterator();
	}

	public int[] getOID() {
		assert getTag() == OBJECT_IDENTIFIER;
		return (int[]) payload;
	}

	public Boolean getBoolean() {
		assert getTag() == BOOLEAN;
		return (Boolean) payload;
	}

	public BitSet getBits() {
		assert getTag() == BIT_STRING;
		return (BitSet) payload;
	}

	public int getInt() {
		assert getTag() == INTEGER || getTag() == ENUMERATED;
		return (Integer) payload;
	}

	public byte[] getBytes() {
		return (byte[]) payload;
	}

	public PDU[] getChildren() {
		assert isConstructed();
		return (PDU[]) payload;
	}

	public Date getDate() {
		assert getTag() == UTCTIME || getTag() == GENERALIZED_TIME;
		return (Date) payload;
	}

}
