package aQute.libg.cafs;

import static aQute.lib.io.IO.copy;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import aQute.lib.index.Index;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;

/**
 * CAFS implements a SHA-1 based file store. The basic idea is that every file
 * in the universe has a unique SHA-1. Hard to believe but people smarter than
 * me have come to that conclusion. This class maintains a compressed store of
 * SHA-1 identified files. So if you have the SHA-1, you can get the contents.
 * This makes it easy to store a SHA-1 instead of the whole file or maintain a
 * naming scheme. An added advantage is that it is always easy to verify you get
 * the right stuff. The SHA-1 Content Addressable File Store is the core
 * underlying idea in Git.
 */
public class CAFS implements Closeable, Iterable<SHA1> {
	final static byte[]	CAFS;
	final static byte[]	CAFE;
	final static String	INDEXFILE		= "index.idx";
	final static String	STOREFILE		= "store.cafs";
	final static String	ALGORITHM		= "SHA-1";
	final static int	KEYLENGTH		= 20;
	final static int	HEADERLENGTH	= 4				// CAFS
		+ 4												// flags
		+ 4												// compressed length
		+ 4												// uncompressed length
		+ KEYLENGTH										// key
		+ 2												// header checksum
	;

	final File			home;
	Index				index;
	RandomAccessFile	store;
	FileChannel			channel;

	static {
		try {
			CAFS = "CAFS".getBytes(UTF_8);
			CAFE = "CAFE".getBytes(UTF_8);
		} catch (Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Constructor for a Content Addressable File Store
	 *
	 * @param home
	 * @param create
	 * @throws Exception
	 */
	public CAFS(File home, boolean create) throws Exception {
		this.home = home;
		if (!home.isDirectory()) {
			if (create) {
				IO.mkdirs(home);
			} else
				throw new IllegalArgumentException("CAFS requires a directory with create=false");
		}

		index = new Index(new File(home, INDEXFILE), KEYLENGTH);
		store = new RandomAccessFile(new File(home, STOREFILE), "rw");
		channel = store.getChannel();
		if (store.length() < 0x100) {
			if (create) {
				store.write(CAFS);
				for (int i = 1; i < 64; i++)
					store.writeInt(0);
				channel.force(true);
			} else
				throw new IllegalArgumentException("Invalid store file, length is too short " + store);
			System.err.println(store.length());
		}
		store.seek(0);
		if (!verifySignature(store, CAFS))
			throw new IllegalArgumentException("Not a valid signature: CAFS at start of file");

	}

	/**
	 * Store an input stream in the CAFS while calculating and returning the
	 * SHA-1 code.
	 *
	 * @param in The input stream to store.
	 * @return The SHA-1 code.
	 * @throws Exception if anything goes wrong
	 */
	public SHA1 write(InputStream in) throws Exception {

		Deflater deflater = new Deflater();
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		DigestInputStream din = new DigestInputStream(in, md);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DeflaterOutputStream dout = new DeflaterOutputStream(bout, deflater);
		copy(din, dout);

		synchronized (store) {
			// First check if it already exists
			SHA1 sha1 = new SHA1(md.digest());

			long search = index.search(sha1.digest());
			if (search > 0)
				return sha1;

			byte[] compressed = bout.toByteArray();

			// we need to append this file to our store,
			// which requires a lock. However, we are in a race
			// so others can get the lock between us getting
			// the length and someone else getting the lock.
			// So we must verify after we get the lock that the
			// length was unchanged.
			FileLock lock = null;
			try {
				long insertPoint;
				int recordLength = compressed.length + HEADERLENGTH;

				while (true) {
					insertPoint = store.length();
					lock = channel.lock(insertPoint, recordLength, false);

					if (store.length() == insertPoint)
						break;

					// We got the wrong lock, someone else
					// got in between reading the length
					// and locking
					lock.release();
				}
				int totalLength = deflater.getTotalIn();
				store.seek(insertPoint);
				update(sha1.digest(), compressed, totalLength);
				index.insert(sha1.digest(), insertPoint);
				return sha1;
			} finally {
				if (lock != null)
					lock.release();
			}
		}
	}

	/**
	 * Read the contents of a sha 1 key.
	 *
	 * @param sha1 The key
	 * @return An Input Stream on the content or null of key not found
	 * @throws Exception
	 */
	public InputStream read(final SHA1 sha1) throws Exception {
		synchronized (store) {
			long offset = index.search(sha1.digest());
			if (offset < 0)
				return null;

			byte[] readSha1;
			byte[] buffer;
			store.seek(offset);
			if (!verifySignature(store, CAFE))
				throw new IllegalArgumentException("No signature");

			int flags = store.readInt();
			int compressedLength = store.readInt();
			int uncompressedLength = store.readInt();
			readSha1 = new byte[KEYLENGTH];
			store.read(readSha1);
			SHA1 rsha1 = new SHA1(readSha1);

			if (!sha1.equals(rsha1))
				throw new IOException("SHA1 read and asked mismatch: " + sha1 + " " + rsha1);

			short crc = store.readShort(); // Read CRC
			if (crc != checksum(flags, compressedLength, uncompressedLength, readSha1))
				throw new IllegalArgumentException("Invalid header checksum: " + sha1);

			buffer = new byte[compressedLength];
			store.readFully(buffer);
			return getSha1Stream(sha1, buffer, uncompressedLength);
		}
	}

	public boolean exists(byte[] sha1) throws Exception {
		return index.search(sha1) >= 0;
	}

	public void reindex() throws Exception {
		long length;
		synchronized (store) {
			length = store.length();
			if (length < 0x100)
				throw new IllegalArgumentException("Store file is too small, need to be at least 256 bytes: " + store);
		}

		try (RandomAccessFile in = new RandomAccessFile(new File(home, STOREFILE), "r")) {
			byte[] signature = new byte[4];
			in.readFully(signature);
			if (!Arrays.equals(CAFS, signature))
				throw new IllegalArgumentException("Store file does not start with CAFS: " + in);

			in.seek(0x100);
			File ixf = new File(home, "index.new");
			Index index = new Index(ixf, KEYLENGTH);

			while (in.getFilePointer() < length) {
				long entry = in.getFilePointer();
				SHA1 sha1 = verifyEntry(in);
				index.insert(sha1.digest(), entry);
			}

			synchronized (store) {
				index.close();
				File indexFile = new File(home, INDEXFILE);
				IO.rename(ixf, indexFile);
				this.index = new Index(indexFile, KEYLENGTH);
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (store) {
			try {
				store.close();
			} finally {
				index.close();
			}
		}
	}

	private SHA1 verifyEntry(RandomAccessFile in) throws IOException, NoSuchAlgorithmException {
		byte[] signature = new byte[4];
		in.readFully(signature);
		if (!Arrays.equals(CAFE, signature))
			throw new IllegalArgumentException("File is corrupted: " + in);

		/* int flags = */in.readInt();
		int compressedSize = in.readInt();
		int uncompressedSize = in.readInt();
		byte[] key = new byte[KEYLENGTH];
		in.readFully(key);
		SHA1 sha1 = new SHA1(key);

		byte[] buffer = new byte[compressedSize];
		in.readFully(buffer);

		try (InputStream xin = getSha1Stream(sha1, buffer, uncompressedSize)) {
			xin.skip(uncompressedSize);
		}
		return sha1;
	}

	private boolean verifySignature(DataInput din, byte[] org) throws IOException {
		byte[] read = new byte[org.length];
		din.readFully(read);
		return Arrays.equals(read, org);
	}

	private InputStream getSha1Stream(final SHA1 sha1, byte[] buffer, final int total) throws NoSuchAlgorithmException {
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		InflaterInputStream iin = new InflaterInputStream(in) {
			int					count		= 0;
			final MessageDigest	digestx		= MessageDigest.getInstance(ALGORITHM);
			final AtomicBoolean	calculated	= new AtomicBoolean();

			@Override
			public int read(byte[] data, int offset, int length) throws IOException {
				int size = super.read(data, offset, length);
				if (size <= 0)
					eof();
				else {
					count += size;
					this.digestx.update(data, offset, size);
				}
				return size;
			}

			@Override
			public int read() throws IOException {
				int c = super.read();
				if (c < 0)
					eof();
				else {
					count++;
					this.digestx.update((byte) c);
				}
				return c;
			}

			void eof() throws IOException {
				if (calculated.getAndSet(true))
					return;

				if (count != total)
					throw new IOException(
						"Counts do not match. Expected to read: " + total + " Actually read: " + count);

				SHA1 calculatedSha1 = new SHA1(digestx.digest());
				if (!sha1.equals(calculatedSha1))
					throw (new IOException(
						"SHA1 caclulated and asked mismatch, asked: " + sha1 + ", \nfound: " + calculatedSha1));
			}

			@Override
			public void close() throws IOException {
				eof();
				super.close();
			}
		};
		return iin;
	}

	/**
	 * Update a record in the store, assuming the store is at the right
	 * position.
	 *
	 * @param sha1 The checksum
	 * @param compressed The compressed length
	 * @param totalLength The uncompressed length
	 * @throws IOException The exception
	 */
	private void update(byte[] sha1, byte[] compressed, int totalLength) throws IOException {
		// System.err.println("pos: " + store.getFilePointer());
		store.write(CAFE); // 00-03 Signature
		store.writeInt(0); // 04-07 Flags for the future
		store.writeInt(compressed.length); // 08-11 Length deflated data
		store.writeInt(totalLength); // 12-15 Length
		store.write(sha1); // 16-35
		store.writeShort(checksum(0, compressed.length, totalLength, sha1));
		store.write(compressed);
		channel.force(false);
	}

	short checksum(int flags, int compressedLength, int totalLength, byte[] sha1) {
		CRC32 crc = new CRC32();
		crc.update(flags);
		crc.update(flags >> 8);
		crc.update(flags >> 16);
		crc.update(flags >> 24);
		crc.update(compressedLength);
		crc.update(compressedLength >> 8);
		crc.update(compressedLength >> 16);
		crc.update(compressedLength >> 24);
		crc.update(totalLength);
		crc.update(totalLength >> 8);
		crc.update(totalLength >> 16);
		crc.update(totalLength >> 24);
		crc.update(sha1);
		return (short) crc.getValue();
	}

	@Override
	public Iterator<SHA1> iterator() {

		return new Iterator<SHA1>() {
			long position = 0x100;

			@Override
			public boolean hasNext() {
				synchronized (store) {
					try {
						return position < store.length();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public SHA1 next() {
				synchronized (store) {
					try {
						store.seek(position);
						byte[] signature = new byte[4];
						store.readFully(signature);
						if (!Arrays.equals(CAFE, signature))
							throw new IllegalArgumentException("No signature");

						int flags = store.readInt();
						int compressedLength = store.readInt();
						int totalLength = store.readInt();
						byte[] sha1 = new byte[KEYLENGTH];
						store.readFully(sha1);
						short crc = store.readShort();
						if (crc != checksum(flags, compressedLength, totalLength, sha1))
							throw new IllegalArgumentException("Header checksum fails");

						position += HEADERLENGTH + compressedLength;
						return new SHA1(sha1);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Remvoe not supported, CAFS is write once");
			}
		};
	}

	public boolean isEmpty() throws IOException {
		synchronized (store) {
			return store.getFilePointer() <= 256;
		}
	}
}
