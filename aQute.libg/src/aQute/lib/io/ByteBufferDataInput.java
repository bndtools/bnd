package aQute.lib.io;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
public class ByteBufferDataInput implements DataInput {
	private final ByteBuffer bb;

	public static DataInput wrap(ByteBuffer bb) {
		return new ByteBufferDataInput(bb);
	}

	private ByteBufferDataInput(ByteBuffer bb) {
		this.bb = Objects.requireNonNull(bb);
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
		if (n <= 0) {
			return 0;
		}
		int skipped = Math.min(n, bb.remaining());
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
		return 0xFF & bb.get();
	}

	@Override
	public short readShort() {
		return bb.getShort();
	}

	@Override
	public int readUnsignedShort() {
		return 0xFFFF & bb.getShort();
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
	public String readLine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String readUTF() throws IOException {
		return DataInputStream.readUTF(this);
	}
}
