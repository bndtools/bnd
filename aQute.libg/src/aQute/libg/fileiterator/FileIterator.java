package aQute.libg.fileiterator;

import java.io.*;
import java.util.*;

public class FileIterator implements Iterator<File> {
    File         dir;
    int          n = 0;
    FileIterator next;

    public FileIterator(File nxt) {
        assert nxt.isDirectory();
        this.dir = nxt;
    }

    public boolean hasNext() {
        if (next != null)
            return next.hasNext();
		return n < dir.list().length;
    }

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

    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove from a file iterator");
    }
}
