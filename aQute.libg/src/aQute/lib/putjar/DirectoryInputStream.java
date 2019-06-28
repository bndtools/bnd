package aQute.lib.putjar;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.libg.fileiterator.FileIterator;

public class DirectoryInputStream extends InputStream {
	static final int			BUFFER_SIZE	= IOConstants.PAGE_SIZE * 16;

	final File					root;
	final FileIterator			fi;
	File						element;
	int							entries		= 0;
	int							state		= START;
	long						where		= 0;

	final static int			START		= 0;
	final static int			HEADER		= 1;
	final static int			DATA		= 2;
	final static int			DIRECTORY	= 4;
	final static int			EOF			= 5;

	final static InputStream	eof			= new ByteArrayInputStream(new byte[0]);
	ByteArrayOutputStream		directory	= new ByteArrayOutputStream();
	InputStream					current		= eof;

	public DirectoryInputStream(File dir) {
		root = dir;
		fi = new FileIterator(dir);
	}

	@Override
	public int read() throws IOException {
		if (fi == null)
			return -1;

		int c = current.read();
		if (c < 0) {
			next();
			c = current.read();
		}
		if (c >= 0)
			where++;

		return c;
	}

	void next() throws IOException {
		switch (state) {
			case START :
			case DATA :
				nextHeader();
				break;

			case HEADER :
				if (element.isFile() && element.length() > 0) {
					current = IO.stream(element);
					state = DATA;
				} else
					nextHeader();
				break;

			case DIRECTORY :
				state = EOF;
				current = eof;
				break;

			case EOF :
				break;
		}
	}

	private void nextHeader() throws IOException {
		if (fi.hasNext()) {
			element = fi.next();
			state = HEADER;
			current = getHeader(root, element);
			entries++;
		} else {
			current = getDirectory();
			state = DIRECTORY;
		}
	}

	/**
	 * <pre>
	 *  end of central dir signature 4 bytes (0x06054b50) number of this
	 * disk 2 bytes number of the disk with the start of the central directory 2
	 * bytes total number of entries in the central directory on this disk 2
	 * bytes total number of entries in the central directory 2 bytes size of
	 * the central directory 4 bytes offset of start of central directory with
	 * respect to the starting disk number 4 bytes .ZIP file comment length 2
	 * bytes .ZIP file comment (variable size)
	 * </pre>
	 */
	InputStream getDirectory() throws IOException {
		long where = this.where;
		int sizeDirectory = directory.size();

		writeInt(directory, 0x504b0506); // Signature
		writeShort(directory, 0); // # of disk
		writeShort(directory, 0); // # of the disk with start of the central
		// dir
		writeShort(directory, entries); // # of entries
		writeInt(directory, sizeDirectory); // Size of central dir
		writeInt(directory, (int) where);
		writeShort(directory, 0);

		directory.close();

		byte[] data = directory.toByteArray();
		return new ByteArrayInputStream(data);
	}

	private void writeShort(OutputStream out, int v) throws IOException {
		for (int i = 0; i < 2; i++) {
			out.write((byte) (v & 0xFF));
			v = v >> 8;
		}
	}

	private void writeInt(OutputStream out, int v) throws IOException {
		for (int i = 0; i < 4; i++) {
			out.write((byte) (v & 0xFF));
			v = v >> 8;
		}
	}

	/**
	 * Local file header:
	 *
	 * <pre>
	 *  local file header signature 4 bytes (0x04034b50)
	 * version needed to extract 2 bytes general purpose bit flag 2 bytes
	 * compression method 2 bytes last mod file time 2 bytes last mod file date
	 * 2 bytes crc-32 4 bytes compressed size 4 bytes uncompressed size 4 bytes
	 * file name length 2 bytes extra field length 2 bytes file name (variable
	 * size) extra field (variable size) central file header signature 4 bytes
	 * (0x02014b50) version made by 2 bytes version needed to extract 2 bytes
	 * general purpose bit flag 2 bytes compression method 2 bytes last mod file
	 * time 2 bytes last mod file date 2 bytes crc-32 4 bytes compressed size 4
	 * bytes uncompressed size 4 bytes file name length 2 bytes extra field
	 * length 2 bytes file comment length 2 bytes disk number start 2 bytes
	 * internal file attributes 2 bytes external file attributes 4 bytes
	 * relative offset of local header 4 bytes file name (variable size) extra
	 * field (variable size) file comment (variable size)
	 * </pre>
	 * </pre>
	 */
	private InputStream getHeader(File root, File file) throws IOException {
		long where = this.where;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		// Signature
		writeInt(bout, 0x04034b50);
		writeInt(directory, 0x504b0102);

		// Version needed to extract
		writeShort(directory, 0);

		// Version needed to extract
		writeShort(bout, 10);
		writeShort(directory, 10);

		// General purpose bit flag (use descriptor)
		writeShort(bout, 0); // descriptor follows data
		writeShort(directory, 0); // descriptor follows data

		// Compresson method (stored)
		writeShort(bout, 0);
		writeShort(directory, 0);

		// Mod time
		writeInt(bout, 0);
		writeInt(directory, 0);

		if (file.isDirectory()) {
			writeInt(bout, 0); // CRC
			writeInt(bout, 0); // Compressed size
			writeInt(bout, 0); // Uncompressed Size
			writeInt(directory, 0);
			writeInt(directory, 0);
			writeInt(directory, 0);
		} else {
			CRC32 crc = getCRC(file);
			writeInt(bout, (int) crc.getValue());
			writeInt(bout, (int) file.length());
			writeInt(bout, (int) file.length());
			writeInt(directory, (int) crc.getValue());
			writeInt(directory, (int) file.length());
			writeInt(directory, (int) file.length());
		}

		String p = getPath(root, file);
		if (file.isDirectory())
			p = p + "/";
		byte[] path = p.getBytes(UTF_8);
		writeShort(bout, path.length);
		writeShort(directory, path.length);

		writeShort(bout, 0); // extra length
		writeShort(directory, 0);

		bout.write(path);

		writeShort(directory, 0); // File comment length
		writeShort(directory, 0); // disk number start 2 bytes
		writeShort(directory, 0); // internal file attributes 2 bytes
		writeInt(directory, 0); // external file attributes 4 bytes
		writeInt(directory, (int) where); // relative offset of local header 4
		// bytes

		directory.write(path);

		byte[] bytes = bout.toByteArray();
		return new ByteArrayInputStream(bytes);
	}

	private String getPath(File root, File file) {
		if (file.equals(root))
			return "";

		String p = getPath(root, file.getParentFile());
		if (p.length() == 0)
			p = file.getName();
		else {
			p = p + "/" + file.getName();
		}
		return p;
	}

	private CRC32 getCRC(File file) throws IOException {
		CRC32 crc = new CRC32();
		try (InputStream in = IO.stream(file)) {
			byte data[] = new byte[BUFFER_SIZE];
			int size = in.read(data);
			while (size > 0) {
				crc.update(data, 0, size);
				size = in.read(data);
			}
		}
		return crc;
	}

}
