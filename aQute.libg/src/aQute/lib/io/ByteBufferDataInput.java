package aQute.lib.io;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ByteBufferDataInput implements DataInput {
	private final ByteBuffer bb;

	public static DataInput wrap(ByteBuffer bb) {
		return new ByteBufferDataInput(bb);
	}

	public static DataInput wrap(byte[] b) {
		return wrap(b, 0, b.length);
	}

	public static DataInput wrap(byte[] b, int off, int len) {
		return wrap(ByteBuffer.wrap(b, off, len));
	}

	private ByteBufferDataInput(ByteBuffer bb) {
		this.bb = Objects.requireNonNull(bb);
	}

	private int ranged(int n) {
		if (n <= 0) {
			return 0;
		}
		return Math.min(n, bb.remaining());
	}

	public ByteBuffer slice(int n) {
		int limit = ranged(n);
		ByteBuffer slice = bb.slice();
		slice.limit(limit);
		bb.position(bb.position() + limit);
		return slice;
	}

	@Override
	public void readFully(byte[] b) {
		bb.get(b, 0, b.length);
	}

	@Override
	public void readFully(byte[] b, int off, int len) {
		bb.get(b, off, len);
	}

	@Override
	public int skipBytes(int n) {
		int skipped = ranged(n);
		bb.position(bb.position() + skipped);
		return skipped;
	}

	@Override
	public boolean readBoolean() {
		return bb.get() != 0;
	}

	@Override
	public byte readByte() {
		return bb.get();
	}

	@Override
	public int readUnsignedByte() {
		return Byte.toUnsignedInt(bb.get());
	}

	@Override
	public short readShort() {
		return bb.getShort();
	}

	@Override
	public int readUnsignedShort() {
		return Short.toUnsignedInt(bb.getShort());
	}

	@Override
	public char readChar() {
		return bb.getChar();
	}

	@Override
	public int readInt() {
		return bb.getInt();
	}

	@Override
	public long readLong() {
		return bb.getLong();
	}

	@Override
	public float readFloat() {
		return bb.getFloat();
	}

	@Override
	public double readDouble() {
		return bb.getDouble();
	}

	@Override
	@Deprecated
	public String readLine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String readUTF() throws IOException {
		return IO.readUTF(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName());
		sb.append("[pos=");
		sb.append(bb.position());
		sb.append(" lim=");
		sb.append(bb.limit());
		sb.append(" cap=");
		sb.append(bb.capacity());
		sb.append("]");
		return sb.toString();
	}
}
