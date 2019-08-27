package aQute.lib.io;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class LimitedDataInput implements DataInput {
	private final DataInput	in;
	private int				remaining;

	public static DataInput wrap(DataInput in, int size) {
		requireNonNull(in);
		if (size < 0) {
			throw new IllegalArgumentException("size must be non-negative");
		}
		if (in instanceof ByteBufferDataInput) {
			ByteBufferDataInput bbin = (ByteBufferDataInput) in;
			ByteBuffer slice = bbin.slice(size);
			return ByteBufferDataInput.wrap(slice);
		}
		return new LimitedDataInput(in, size);
	}

	private LimitedDataInput(DataInput in, int size) {
		this.in = in;
		this.remaining = size;
	}

	private void consume(int n) throws IOException {
		if (n - remaining > 0) {
			throw new EOFException("request to read more bytes than available");
		}
		remaining -= n;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		consume(b.length);
		in.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		consume(len);
		in.readFully(b, off, len);
	}

	private int ranged(int n) {
		if (n <= 0) {
			return 0;
		}
		return Math.min(n, remaining);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		int limit = ranged(n);
		int skipped = in.skipBytes(limit);
		consume(skipped);
		return skipped;
	}

	@Override
	public boolean readBoolean() throws IOException {
		consume(Byte.BYTES);
		return in.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		consume(Byte.BYTES);
		return in.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		consume(Byte.BYTES);
		return in.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		consume(Short.BYTES);
		return in.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		consume(Short.BYTES);
		return in.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		consume(Character.BYTES);
		return in.readChar();
	}

	@Override
	public int readInt() throws IOException {
		consume(Integer.BYTES);
		return in.readInt();
	}

	@Override
	public long readLong() throws IOException {
		consume(Long.BYTES);
		return in.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		consume(Float.BYTES);
		return in.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		consume(Double.BYTES);
		return in.readDouble();
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
		sb.append("[remaining=");
		sb.append(remaining);
		sb.append("]");
		return sb.toString();
	}
}
