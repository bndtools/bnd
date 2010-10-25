package aQute.libg.cafs;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

import aQute.lib.index.*;

/**
 * CAFS implements a SHA-1 based file store. The basic idea is that every file
 * in the universe has a unique SHA-1. Hard to believe but people smarter than
 * me have come to that conclusion. This class maintains a compressed store of
 * SHA-1 identified files. So if you have the SHA-1, you can get the contents.
 * 
 * This makes it easy to store a SHA-1 instead of the whole file or maintain a
 * naming scheme. An added advantage is that it is always easy to verify you get
 * the right stuff. The SHA-1 Content Addressable File Store is the core
 * underlying idea in Git.
 */
public class CAFS implements Closeable {
	final static byte[]	CAFS			= "CAFS".getBytes();
	final static byte[]	CAFE			= "CAFE".getBytes();
	final static String	INDEXFILE		= "index.idx";
	final static String	STOREFILE		= "store.cafs";
	final static String	ALGORITHM		= "SHA-1";
	final static int	KEYLENGTH		= 20;
	final static int	HEADERLENGTH	= 4 // CAFS
												+ 4 // flags
												+ 4 // compressed length
												+ 4 // uncompressed length
												+ KEYLENGTH;	// key

	final File			home;
	Index				index;
	RandomAccessFile	store;
	FileChannel			channel;

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
				home.mkdirs();
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
				throw new IllegalArgumentException("Invalid store file, length is too short "
						+ store);
		}
		store.seek(0);
		if (!verifySignature(store, CAFS))
			throw new IllegalArgumentException("Not a valid signature: CAFS at start of file");

	}

	/**
	 * Store an input stream in the CAFS while calculating and returning the
	 * SHA-1 code.
	 * 
	 * @param in
	 *            The input stream to store.
	 * @return The SHA-1 code.
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public byte[] write(InputStream in) throws Exception {

		Deflater deflater = new Deflater();
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		DigestInputStream din = new DigestInputStream(in, md);
		DeflaterInputStream dfl = new DeflaterInputStream(din, deflater);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		copy(dfl, bout);

		synchronized (store) {
			// First check if it already exists
			byte[] sha1 = md.digest();
			long search = index.search(sha1);
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
				update(sha1, compressed, totalLength);
				index.insert(sha1, insertPoint);
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
	 * @param sha1
	 *            The key
	 * @return An Input Stream on the content or null of key not found
	 * @throws Exception
	 */
	public InputStream read(final byte[] sha1) throws Exception {
		synchronized (store) {
			long offset = index.search(sha1);
			if (offset < 0)
				return null;

			byte[] readSha1;
			byte[] buffer;
			store.seek(offset);
			if (!verifySignature(store, CAFE))
				throw new IllegalArgumentException("No signature");

			/* int flags = */store.readInt();
			int compressedLength = store.readInt();
			/* uncompressedLength = */store.readInt();
			readSha1 = new byte[KEYLENGTH];
			store.read(readSha1);
			if (!Arrays.equals(sha1, readSha1))
				throw new IOException("SHA1 read and asked mismatch");

			buffer = new byte[compressedLength];
			store.readFully(buffer);
			return getSha1Stream(sha1, buffer);
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
				throw new IllegalArgumentException(
						"Store file is too small, need to be at least 256 bytes: " + store);
		}

		RandomAccessFile in = new RandomAccessFile(new File(home, STOREFILE), "r");
		try {
			byte[] signature = new byte[4];
			in.readFully(signature);
			if (!Arrays.equals(CAFS, signature))
				throw new IllegalArgumentException("Store file does not start with CAFS: " + in);

			in.seek(0x100);
			File ixf = new File(home, "index.new");
			Index index = new Index(ixf, KEYLENGTH);

			while (in.getFilePointer() < length) {
				long entry = in.getFilePointer();
				byte[] sha1 = verifyEntry(in);
				index.insert(sha1, entry);
			}

			synchronized (store) {
				index.close();
				File indexFile = new File(home, INDEXFILE);
				ixf.renameTo(indexFile);
				index = new Index(indexFile, KEYLENGTH);
			}
		} finally {
			in.close();
		}
	}

	public void close() throws IOException {
		synchronized (store) {
			try {
				store.close();
			} finally {
				index.close();
			}
		}
	}

	private byte[] verifyEntry(RandomAccessFile in) throws IOException, NoSuchAlgorithmException {
		byte[] signature = new byte[4];
		in.readFully(signature);
		if (!Arrays.equals(CAFE, signature))
			throw new IllegalArgumentException("File is corrupted: " + in);

		/* int flags = */in.readInt();
		int compressedSize = in.readInt();
		int uncompressedSize = in.readInt();
		byte[] sha1 = new byte[KEYLENGTH];
		in.readFully(sha1);
		byte[] buffer = new byte[compressedSize];
		in.readFully(buffer);

		InputStream xin = getSha1Stream(sha1, buffer);
		xin.skip(uncompressedSize);
		xin.close();
		return sha1;
	}

	private boolean verifySignature(DataInput din, byte[] org) throws IOException {
		byte[] read = new byte[org.length];
		din.readFully(read);
		return Arrays.equals(read, org);
	}

	private DigestInputStream getSha1Stream(final byte[] sha1, byte[] buffer)
			throws NoSuchAlgorithmException {
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		Inflater inflater = new Inflater();
		final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
		InflaterInputStream iin = new InflaterInputStream(in, inflater);

		DigestInputStream din = new DigestInputStream(iin, digest) {
			@Override public int read(byte[] data, int offset, int length) throws IOException {
				int size = super.read(data, offset, length);
				if (size <= 0)
					eof();
				return size;
			}

			public int read() throws IOException {
				int c = super.read();
				if (c < 0)
					eof();
				return c;
			}

			void eof() throws IOException {
				byte[] calculatedSha1 = digest.digest();
				if (!Arrays.equals(sha1, calculatedSha1))
					throw new IOException("SHA1 caclulated and asked mismatch, asked: "
							+ Arrays.toString(sha1) + ", found: " + Arrays.toString(calculatedSha1));
			}

			public void close() throws IOException {
				eof();
				super.close();
			}
		};
		return din;
	}

	/**
	 * Update a record in the store, assuming the store is at the right
	 * position.
	 * 
	 * @param sha1
	 *            The checksum
	 * @param compressed
	 *            The compressed length
	 * @param totalLength
	 *            The uncompressed length
	 * @throws IOException
	 *             The exception
	 */
	private void update(byte[] sha1, byte[] compressed, int totalLength) throws IOException {
		store.write(CAFE); // 00-03 Signature
		store.writeInt(0); // 04-07 Flags for the future
		store.writeInt(compressed.length); // 08-11 Length deflated data
		store.writeInt(totalLength); // 12-15 Length
		store.write(sha1); // 16-35
		store.write(compressed);
		channel.force(false);
	}

}
