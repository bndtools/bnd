package aQute.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
	private ByteBuffer bb;

	public ByteBufferOutputStream() {
		this(IOConstants.PAGE_SIZE);
	}

	public ByteBufferOutputStream(int size) {
		bb = ByteBuffer.allocate(size);
	}

	public ByteBuffer toByteBuffer() {
		ByteBuffer obb = bb.duplicate();
		obb.flip();
		return obb;
	}

	public byte[] toByteArray() {
		ByteBuffer obb = toByteBuffer();
		int len = obb.remaining();
		byte[] result = new byte[len];
		obb.get(result, 0, len);
		return result;
	}

	private ByteBuffer bb(int len) {
		ByteBuffer obb = bb;
		if (obb.remaining() - len >= 0) {
			return obb;
		}
		int minCap = obb.position() + len;
		int newCap = obb.capacity() << 1;
		if ((newCap - minCap) < 0) {
			newCap = minCap;
		}
		obb.flip();
		return bb = ByteBuffer.allocate(newCap)
			.put(obb);
	}

	@Override
	public void write(int b) {
		bb(Byte.BYTES).put((byte) b);
	}

	@Override
	public void write(byte[] b) {
		int len = b.length;
		bb(len).put(b, 0, len);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		bb(len).put(b, off, len);
	}

	public void write(ByteBuffer src) {
		bb(src.remaining()).put(src);
	}

	public void write(InputStream in) throws IOException {
		if (in instanceof ByteBufferInputStream) {
			ByteBufferInputStream bbin = (ByteBufferInputStream) in;
			write(bbin.buffer());
			return;
		}
		ByteBuffer obb;
		do {
			obb = bb(in.available() + 1);
			byte[] buffer = obb.array();
			for (int size, position; obb.hasRemaining()
				&& (size = in.read(buffer, position = obb.position(), obb.remaining())) > 0;) {
				obb.position(position + size);
			}
		} while (!obb.hasRemaining());
	}

	@Override
	public void flush() {}

	@Override
	public void close() {}

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
