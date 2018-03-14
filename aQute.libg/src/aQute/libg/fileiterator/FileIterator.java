package aQute.libg.fileiterator;

import java.io.File;
import java.util.Iterator;

public class FileIterator implements Iterator<File> {
	File			dir;
	int				n	= 0;
	FileIterator	next;

	public FileIterator(File nxt) {
		assert nxt.isDirectory();
		this.dir = nxt;
	}

	@Override
	public boolean hasNext() {
		if (next != null)
			return next.hasNext();
		return n < dir.list().length;
	}

	@Override
	public File next() {
		if (next != null) {
			File answer = next.next();
			if (!next.hasNext())
				next = null;
			return answer;
		}
		File nxt = dir.listFiles()[n++];
		if (nxt.isDirectory()) {
			next = new FileIterator(nxt);
			return nxt;
		} else if (nxt.isFile()) {
			return nxt;
		} else
			throw new IllegalStateException("File disappeared");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cannot remove from a file iterator");
	}
}
