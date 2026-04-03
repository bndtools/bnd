package aQute.bnd.osgi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Resource for a directory. This class implementation assumes the file does not
 * change underneath this object.
 */
public class DirectoryResource extends WriteResource {
	private final File	dir;

	public DirectoryResource(File dir) throws IOException {
		this.dir = dir;
		assert dir.isDirectory();
	}

	public DirectoryResource(Path path) throws IOException {
		this(path.toFile());
	}

	@Override
	public String toString() {
		return dir.toString();
	}

	@Override
	public void write(OutputStream out) throws Exception {
		try (Jar jar = new Jar(dir)) {
			jar.write(out);
		}
	}

	@Override
	public long lastModified() {
		return dir.lastModified();
	}
}
