package aQute.lib.index;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

/**
 * <pre>
 *   0   ->   0, 122   -> 1
 *   123 -> 123, 244   -> 2
 *   245 -> 245, ...
 * </pre>
 * 
 * 
 */
public class Index {
	final static int					SIGNATURE	= 0;
	final static int					MAGIC		= 0x494C4458;
	final static int					KEYSIZE		= 4;

	private FileChannel					file;
	final int							pageSize	= 4096;
	final int							keySize;
	final int							valueSize	= 8;
	final int							capacity;
	public Page							root;
	final LinkedHashMap<Integer, Page>	cache		= new LinkedHashMap<Integer, Index.Page>();
	final MappedByteBuffer				settings;

	private int							nextPage;

	class Page {
		final int				TYPE_OFFSET		= 0;
		final int				COUNT_OFFSET	= 2;
		final static int		START_OFFSET	= 4;
		final int				number;
		boolean					leaf;
		final MappedByteBuffer	buffer;
		int						n				= 0;
		private boolean	dirty;

		Page(int number) throws IOException {
			this.number = number;
			buffer = file.map(MapMode.READ_WRITE, number * pageSize, pageSize);
			n = buffer.getShort(COUNT_OFFSET);
			int type = buffer.getShort(TYPE_OFFSET);
			leaf = type != 0;
		}

		Page(int number, boolean leaf) throws IOException {
			this.number = number;
			this.leaf = leaf;
			this.n = 0;
			buffer = file.map(MapMode.READ_WRITE, number * pageSize, pageSize);
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
				else
					return c(i);
			} else {
				long value = c(i);
				Page child = getPage((int) value);
				return child.search(k);
			}
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
				left.buffer.force();
				right.buffer.force();
				this.buffer.force();
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
				System.out.println("Arghhh");
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
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("number=").append(number).append("\n");
			sb.append("leaf=").append(leaf).append("\n");
			sb.append("n=").append(n).append("\n");
			sb.append("capacity=").append(capacity).append("\n");
			sb.append("pageSize=").append(pageSize).append("\n");
			sb.append("keySize=").append(keySize).append("\n");
			sb.append("---\n");
			for (int i = 0; i < n; i++) {
				byte[] k = k(i);
				for (int j = 0; j < 4 && j < k.length; j++)
					sb.append(Integer.toHexString(k[j]));
				sb.append(" = ");
				sb.append(c(i));
				sb.append("\n");
			}
			sb.append("---\n");
			return sb.toString();
		}

	}

	public Index(File file, int keySize) throws IOException {
		capacity = (pageSize - Page.START_OFFSET) / (keySize + valueSize);
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		this.file = raf.getChannel();
		settings = this.file.map(MapMode.READ_WRITE, 0, pageSize);
		if (this.file.size() == pageSize) {
			this.keySize = keySize;
			settings.putInt(SIGNATURE, MAGIC);
			settings.putInt(KEYSIZE, keySize);
			nextPage = 1;
			root = allocate(true);
			root.n = 1;
			root.set(0, new byte[0], 0);
			root.write();
		} else {
			if (settings.getInt(SIGNATURE) != MAGIC)
				throw new IllegalStateException("No Index file, magic is not " + MAGIC);

			this.keySize = settings.getInt(KEYSIZE);
			if (keySize != 0 && this.keySize != keySize)
				throw new IllegalStateException("Invalid key size for Index file. The file is "
						+ this.keySize + " and was expected to be " + this.keySize);

			root = getPage(1);
			nextPage = (int) (this.file.size() / pageSize);
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

	public void close() throws IOException {
		file.close();
		cache.clear();
	}

	public void report() throws IOException {
		for (int i = 1; i < nextPage; i++) {
			Page page = getPage(i);
			System.out.printf("%4d %s %d\n", i, page.leaf, page.n);
			for (int j = 0; j < page.n; j++) {
				System.out.printf("%4s %s = %d\n", "", key(page.k(j)), page.c(j));

			}
		}
		System.out.println();
	}

	String key(byte k[]) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < 4 && j < k.length; j++)
			sb.append(Integer.toHexString(k[j]));
		return sb.toString();
	}

	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < nextPage; i++) {
				sb.append(getPage(i).toString());
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void report(String string) throws IOException {
		System.out.println("----------------------------");
		System.out.println(string);
		System.out.println("----------------------------");
		report();
	}
}
