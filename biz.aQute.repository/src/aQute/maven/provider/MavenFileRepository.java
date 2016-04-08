package aQute.maven.provider;

import java.io.File;
import java.net.URI;

import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class MavenFileRepository extends MavenBackingRepository {

	private File remote;

	public MavenFileRepository(File local, File remote, Reporter reporter) throws Exception {
		super(local, remote.toURI().toString(), reporter);
		this.remote = remote;
	}

	@Override
	public TaggedData fetch(String path, File dest) throws Exception {
		File source = getFile(path);
		if (source.isFile()) {
			dest.getParentFile().mkdirs();
			IO.copy(source, dest);
			return new TaggedData(toURI(path), 200, dest);
		} else {
			return new TaggedData(toURI(path), 404, dest);
		}
	}

	@Override
	public void store(File source, String path) throws Exception {
		File dest = getFile(path);
		if (!source.isFile())
			throw new IllegalArgumentException("File does not exist: " + source);

		dest.getParentFile().mkdirs();
		IO.copy(source, dest);
	}

	@Override
	public boolean delete(String path) throws Exception {
		File dest = getFile(path);
		return dest.delete();
	}

	@Override
	public String toString() {
		return "MavenFileRepo[" + remote + "]";
	}

	@Override
	public URI toURI(String path) throws Exception {
		File result = getFile(path);
		return result.toURI();
	}

	private File getFile(String path) {
		return IO.getFile(remote, path);
	}

}
