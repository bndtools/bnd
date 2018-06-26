package aQute.maven.provider;

import java.io.File;
import java.net.URI;

import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.service.reporter.Reporter;

public class MavenFileRepository extends MavenBackingRepository {

	private File remote;

	public MavenFileRepository(File local, File remote, Reporter reporter) throws Exception {
		super(local, remote.toURI()
			.toString(), reporter);
		this.remote = remote;
	}

	@Override
	public TaggedData fetch(String path, File dest) throws Exception {
		File source = getFile(path);
		if (source.isFile()) {
			IO.mkdirs(dest.getParentFile());
			IO.delete(dest);
			IO.copy(source, dest);
			return new TaggedData(toURI(path), 200, dest);
		} else {
			return new TaggedData(toURI(path), 404, dest);
		}
	}

	@Override
	public void store(File source, String path) throws Exception {
		if (!source.isFile())
			throw new IllegalArgumentException("File does not exist: " + source);

		File dest = getFile(path);

		IO.mkdirs(dest.getParentFile());
		IO.copy(source, dest);

		SHA1 sha1 = SHA1.digest(source);
		MD5 md5 = MD5.digest(source);
		IO.store(sha1.asHex() + "\n", new File(dest.getParentFile(), dest.getName() + ".sha1"));
		IO.store(md5.asHex() + "\n", new File(dest.getParentFile(), dest.getName() + ".md5"));
	}

	@Override
	public boolean delete(String path) throws Exception {
		File dest = getFile(path);
		IO.deleteWithException(dest);
		return true;
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

	@Override
	public boolean isFile() {
		return true;
	}
}
