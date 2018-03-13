package aQute.lib.index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * <pre>
 *  0 -> 0, 122 -> 1 123 -> 123, 244 -> 2 245 -> 245, ...
 * </pre>
 */
public class Index implements Iterable<byte[]> {
	final static int					LEAF		= 0;
	final static int					INDEX		= 1;

	final static int					SIGNATURE	= 0;
	final static int					MAGIC		= 0x494C4458;
	final static int					KEYSIZE		= 4;

	FileChannel							file;
	final int							pageSize	= 4096;
	final int							keySize;
	final int							valueSize	= 8;
	final int							capacity;
	public Page							root;
	final LinkedHashMap<Integer, Page>	cache		= new LinkedHashMap<>();
	final MappedByteBuffer				settings;

	private int							nextPage;

	class Page {
		final static int		TYPE_OFFSET		= 0;
		final static int		COUNT_OFFSET	= 2;
		final static int		START_OFFSET	= 4;
		final int				number;
		boolean					leaf;
		final MappedByteBuffer	buffer;
		int						n				= 0;
		boolean					dirty;

		Page(int number) throws IOException {
			this.number = number;
			buffer = file.map(MapMode.READ_WRITE, ((long) number) * pageSize, pageSize);
			n = buffer.getShort(COUNT_OFFSET);
			int type = buffer.getShort(TYPE_OFFSET);
			leaf = type != 0;
		}

		Page(int number, boolean leaf) throws IOException {
			this.number = number;
			this.leaf = leaf;
			this.n = 0;
			buffer = file.map(MapMode.READ_WRITE, ((long) number) * pageSize, pageSize);
		}

		Iterator<byte[]> iterator() {
			return new Iterator<byte[]>() {
				Iterator<byte[]>	i;
				int					rover	= 0;

				@Override
				public byte[] next() {
					if (leaf) {
						return k(rover++);
					}

					return i.next();
				}

				@Override
				public boolean hasNext() {
					try {
						if (leaf)
							return rover < n;
						while (i == null || i.hasNext() == false) {
							int c = (int) c(rover++);
							i = getPage(c).iterator();
						}
						return i.hasNext();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};
		}

		void write() throws IOException {
			buffer.putShort(COUNT_OFFSET, (short) n);
			buffer.put(TYPE_OFFSET, (byte) (leaf ? 1 : 0));
			buffer.force();
		}

		int compare(byte[] key, int i) {
			int index = pos(i);
			for (int j = 0; j < keySize; j++, index++) {
				int a = 0;
				if (j < key.length)
					a = key[j] & 0xFF;

				int b = buffer.get(index) & 0xFF;
				if (a == b)
					continue;

				return a > b ? 1 : -1;
			}
			return 0;
		}

		int pos(int i) {
			return START_OFFSET + size(i);
		}

		int size(int n) {
			return n * (keySize + valueSize);
		}

		void copyFrom(Page page, int start, int length) {
			copy(page.buffer, pos(start), buffer, pos(0), size(length));
		}

		void copy(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int length) {
			if (srcPos < dstPos) {
				while (length-- > 0)
					dst.put(dstPos + length, src.get(srcPos + length));

			} else {
				while (length-- > 0)
					dst.put(dstPos++, src.get(srcPos++));
			}
		}

		long search(byte[] k) throws Exception {
			int cmp = 0;
			int i = n - 1;
			while (i >= 0 && (cmp = compare(k, i)) < 0)
				i--;

			if (leaf) {
				if (cmp != 0)
					return -1;
				return c(i);
			}
			long value = c(i);
			Page child = getPage((int) value);
			return child.search(k);
		}

		void insert(byte[] k, long v) throws IOException {
			if (n == capacity) {
				int t = capacity / 2;
				Page left = allocate(leaf);
				Page right = allocate(leaf);
				left.copyFrom(this, 0, t);
				left.n = t;
				right.copyFrom(this, t, capacity - t);
				right.n = capacity - t;
				leaf = false;
				set(0, left.k(0), left.number);
				set(1, right.k(0), right.number);
				n = 2;
				left.write();
				right.write();
			}
			insertNonFull(k, v);
		}

		byte[] k(int i) {
			buffer.position(pos(i));
			byte[] key = new byte[keySize];
			buffer.get(key);
			return key;
		}

		long c(int i) {
			if (i < 0) {
				System.err.println("Arghhh");
			}
			int index = pos(i) + keySize;
			return buffer.getLong(index);
		}

		void set(int i, byte[] k, long v) {
			int index = pos(i);
			for (int j = 0; j < keySize; j++) {
				byte a = 0;
				if (j < k.length)
					a = k[j];
				buffer.put(index + j, a);
			}
			buffer.putLong(index + keySize, v);
		}

		void insertNonFull(byte[] k, long v) throws IOException {
			int cmp = 0;
			int i = n - 1;
			while (i >= 0 && (cmp = compare(k, i)) < 0)
				i--;

			if (leaf) {
				if (cmp != 0) {
					i++;
					if (i != n)
						copy(buffer, pos(i), buffer, pos(i + 1), size(n - i));
				}
				set(i, k, v);
				n++;
				dirty = true;
			} else {
				long value = c(i);
				Page child = getPage((int) value);

				if (child.n == capacity) {
					Page left = child;
					int t = capacity / 2;
					Page right = allocate(child.leaf);
					right.copyFrom(left, t, capacity - t);
					right.n = capacity - t;
					left.n = t;
					i++; // place to insert
					if (i < n) // ok if at end
						copy(buffer, pos(i), buffer, pos(i + 1), size(n - i));
					set(i, right.k(0), right.number);
					n++;
					assert i < n;
					child = right.compare(k, 0) >= 0 ? right : left;
					left.dirty = true;
					right.dirty = true;
					this.dirty = true;
				}
				child.insertNonFull(k, v);
			}
			write();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			try {
				toString(sb, "");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

		public void toString(StringBuilder sb, String indent) throws IOException {
			for (int i = 0; i < n; i++) {
				sb.append(String.format("%s %02d:%02d %20s %s %d%n", indent, number, i, hex(k(i), 0, 4),
					leaf ? "==" : "->", c(i)));
				if (!leaf) {
					long c = c(i);
					Page sub = getPage((int) c);
					sub.toString(sb, indent + " ");
				}
			}
		}

		private String hex(byte[] k, int i, int j) {
			StringBuilder sb = new StringBuilder();

			while (i < j) {
				int b = 0xFF & k[i];
				sb.append(nibble(b >> 4));
				sb.append(nibble(b));
				i++;
			}
			return sb.toString();
		}

		private char nibble(int i) {
			i = i & 0xF;
			return (char) (i >= 10 ? i + 'A' - 10 : i + '0');
		}

	}

	public Index(File file, int keySize) throws IOException {
		capacity = (pageSize - Page.START_OFFSET) / (keySize + valueSize);

		@SuppressWarnings("resource")
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		try {
			this.file = raf.getChannel();
			settings = this.file.map(MapMode.READ_WRITE, 0, pageSize);
			if (this.file.size() == pageSize) {
				this.keySize = keySize;
				settings.putInt(SIGNATURE, MAGIC);
				settings.putInt(KEYSIZE, keySize);
				nextPage = 1;
				root = allocate(true);
				root.n = 1;
				root.set(0, new byte[KEYSIZE], 0);
				root.write();
			} else {
				if (settings.getInt(SIGNATURE) != MAGIC)
					throw new IllegalStateException("No Index file, magic is not " + MAGIC);

				this.keySize = settings.getInt(KEYSIZE);
				if (keySize != 0 && this.keySize != keySize)
					throw new IllegalStateException("Invalid key size for Index file. The file is " + this.keySize
						+ " and was expected to be " + this.keySize);

				root = getPage(1);
				nextPage = (int) (this.file.size() / pageSize);
			}
		} finally {
			// raf.close();
		}
	}

	public void insert(byte[] k, long v) throws Exception {
		root.insert(k, v);
	}

	public long search(byte[] k) throws Exception {
		return root.search(k);
	}

	Page allocate(boolean leaf) throws IOException {
		Page page = new Page(nextPage++, leaf);
		cache.put(page.number, page);
		return page;
	}

	Page getPage(int number) throws IOException {
		Page page = cache.get(number);
		if (page == null) {
			page = new Page(number);
			cache.put(number, page);
		}
		return page;
	}

	@Override
	public String toString() {
		return root.toString();
	}

	public void close() throws IOException {
		file.close();
		cache.clear();
	}

	@Override
	public Iterator<byte[]> iterator() {
		return root.iterator();
	}
}
