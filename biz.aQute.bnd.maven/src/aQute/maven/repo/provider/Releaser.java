package aQute.maven.repo.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import aQute.lib.io.IO;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.Release;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MetadataParser.ProgramMetadata;
import aQute.maven.repo.provider.MetadataParser.RevisionMetadata;

class Releaser implements Release {
	final List<File>		upload			= new ArrayList<>();
	final MavenStorage		home;
	final Revision			revision;
	final File				tmp;
	final RevisionMetadata	programMetadata	= new RevisionMetadata();

	boolean				force;
	boolean				aborted;
	private File		dir;
	protected boolean	localOnly;

	Releaser(MavenStorage home, Revision revision) throws Exception {
		this.home = home;
		this.revision = revision;
		File tmpBase = new File(home.base, "tmp");
		tmpBase.mkdirs();

		this.tmp = Files.createTempDirectory(tmpBase.toPath(), revision.toString()).toFile();
		this.dir = home.toFile(revision.path);

		if (dir.exists() && !force)
			throw new IllegalArgumentException("The target directory already exists " + dir);
	}

	@Override
	public void close() throws IOException {
		try {
			if (!aborted) {

				if (!localOnly)
					uploadAll(upload.iterator());

				IO.delete(dir);
				IO.copy(tmp, dir);

				if (!localOnly)
					updateMetadata();
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			IO.delete(tmp);
		}
	}

	protected void updateMetadata() throws Exception, InterruptedException {
		int n = 0;
		while (true)
			try {
				File metafile = home.toFile(revision.program.metadata(home.id));
				home.remote.fetch(revision.program.metadata(), metafile);

				long lastModified = metafile.lastModified();

				ProgramMetadata metadata = MetadataParser.parseProgramMetadata(metafile);

				if (metadata.versions.contains(revision.version)) {
					if (force)
						return;

					throw new IllegalStateException(
							"Revision already exists on remote system " + revision + " " + home.remote);

				} else {
					metadata.versions.add(revision.version);
					IO.store(metadata.toString(), metafile);
					home.remote.store(metafile, revision.program.metadata());
					return;
				}

			} catch (Exception e) {
				if (n++ > 3)
					throw e;
				Thread.sleep(1000);
			}
	}

	void uploadAll(Iterator<File> iterator) throws Exception {

		if (!iterator.hasNext())
			return;

		File file = iterator.next();
		try {

			home.remote.store(file, revision.path + "/" + file.getName());
			uploadAll(iterator);
		} catch (Exception e) {

			try {
				home.remote.delete(revision.path + "/" + file.getName());
			} catch (Exception ee) {
				// We ignore this one, best effort, but need to throw the
				// original
				throw e;
			}
		}
	}

	@Override
	public void add(Archive archive, InputStream in) throws Exception {
		try {
			File to = IO.getFile(tmp, archive.getName());
			IO.copy(in, to);
			upload.add(to);
		} catch (Exception e) {
			aborted = true;
			throw e;
		}
	}

	@Override
	public void abort() {
		aborted = true;
	}

	public void force() {
		force = true;
	}

	@Override
	public void add(String extension, String classifier, InputStream in) throws Exception {
		Archive a = revision.archive(extension, classifier);
		add(a, in);
	}

	@Override
	public void setBuild(long timestamp, String build) {
		throw new IllegalArgumentException("This is not a snapshot release so you cannot set the timestamp");
	}

	@Override
	public void setBuild(String timestamp, String build) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLocalOnly() {
		localOnly = true;
	}

}
