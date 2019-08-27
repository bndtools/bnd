package aQute.lib.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

public class ByteBufferDataOutput implements DataOutput {
	private ByteBuffer bb;

	public ByteBufferDataOutput() {
		this(IOConstants.PAGE_SIZE);
	}

	public ByteBufferDataOutput(int size) {
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
	public void writeBoolean(boolean v) {
		bb(Byte.BYTES).put(v ? (byte) 1 : (byte) 0);

	}

	@Override
	public void writeByte(int v) {
		bb(Byte.BYTES).put((byte) v);
	}

	@Override
	public void writeShort(int v) {
		bb(Short.BYTES).putShort((short) v);
	}

	@Override
	public void writeChar(int v) {
		bb(Character.BYTES).putChar((char) v);
	}

	@Override
	public void writeInt(int v) {
		bb(Integer.BYTES).putInt(v);
	}

	@Override
	public void writeLong(long v) {
		bb(Long.BYTES).putLong(v);
	}

	@Override
	public void writeFloat(float v) {
		bb(Float.BYTES).putFloat(v);
	}

	@Override
	public void writeDouble(double v) {
		bb(Double.BYTES).putDouble(v);
	}

	@Override
	public void writeBytes(String s) {
		final int len = s.length();
		ByteBuffer b = bb(len * Byte.BYTES);
		for (int i = 0; i < len; i++) {
			b.put((byte) s.charAt(i));
		}
	}

	@Override
	public void writeChars(String s) {
		final int len = s.length();
		ByteBuffer b = bb(len * Character.BYTES);
		for (int i = 0; i < len; i++) {
			b.putChar(s.charAt(i));
		}
	}

	@Override
	public void writeUTF(String s) throws IOException {
		final int len = s.length();
		int size = len;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c >= 0x0800) {
				size += 2;
			} else if ((c >= 0x0080) || (c == 0x0000)) {
				size++;
			}
		}
		if (size > 0xffff) {
			throw new UTFDataFormatException("encoding too long: " + size);
		}

		ByteBuffer b = bb(Short.BYTES + size);
		b.putShort((short) size);
		if (size == len) { // all chars single byte
			for (int i = 0; i < len; i++) {
				b.put((byte) s.charAt(i));
			}
		} else {
			for (int i = 0; i < len; i++) {
				char c = s.charAt(i);
				if ((c > 0x0000) && (c < 0x0080)) {
					b.put((byte) c);
				} else if (c >= 0x0800) {
					b.put((byte) (0b1110_0000 | ((c >> 12) & 0x0F)));
					b.put((byte) (0b1000_0000 | ((c >> 6) & 0x3F)));
					b.put((byte) (0b1000_0000 | (c & 0x3F)));
				} else {
					b.put((byte) (0b1100_0000 | ((c >> 6) & 0x1F)));
					b.put((byte) (0b1000_0000 | (c & 0x3F)));
				}
			}
		}
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
