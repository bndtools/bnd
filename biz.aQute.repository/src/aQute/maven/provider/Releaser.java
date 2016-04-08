package aQute.maven.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;

class Releaser implements Release {
	final List<Archive>					upload			= new ArrayList<>();
	final MavenRepository				home;
	final Revision						revision;
	final RevisionMetadata				programMetadata	= new RevisionMetadata();
	boolean								force;
	boolean								aborted;
	private File						dir;
	protected boolean					localOnly;
	protected MavenBackingRepository	repo;

	Releaser(MavenRepository home, Revision revision, MavenBackingRepository repo) throws Exception {
		this.home = home;
		this.revision = revision;
		this.repo = repo;
		this.dir = home.toLocalFile(revision.path);

		IO.delete(this.dir);
		check();
		this.dir.mkdirs();
	}

	protected void check() {
	}

	@Override
	public void close() throws IOException {
		try {
			if (!aborted) {

				if (!localOnly) {
					uploadAll(upload.iterator());
					updateMetadata();
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected void updateMetadata() throws Exception, InterruptedException {
		int n = 0;
		while (true)
			try {
				File metafile = home.toLocalFile(revision.program.metadata(repo.id));
				ProgramMetadata metadata;

				TaggedData tag = repo.fetch(revision.program.metadata(), metafile);
				switch (tag.getState()) {
					case NOT_FOUND :
						metadata = new ProgramMetadata();
						break;

					case OTHER :
						throw new HttpRequestException((HttpURLConnection) tag.getConnection());

					case UNMODIFIED :
					case UPDATED :
					default :
						metadata = MetadataParser.parseProgramMetadata(metafile);
						break;

				}

				long lastModified = metafile.lastModified();

				if (metadata.versions.contains(revision.version)) {
					if (force || revision.isSnapshot())
						return;

					throw new IllegalStateException(
							"Revision already exists on remote system " + revision + " " + repo);

				} else {
					metadata.versions.add(revision.version);
					IO.store(metadata.toString(), metafile);
					repo.store(metafile, revision.program.metadata());
					return;
				}

			} catch (Exception e) {
				if (n++ > 3)
					throw e;
				Thread.sleep(1000);
			}
	}

	void uploadAll(Iterator<Archive> iterator) throws Exception {

		if (!iterator.hasNext())
			return;

		Archive archive = iterator.next();
		File f = home.toLocalFile(archive);
		try {
			repo.store(f, archive.remotePath);
			uploadAll(iterator);
		} catch (Exception e) {

			try {
				repo.delete(archive.remotePath);
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
			archive = resolve(archive);

			home.store(archive, in);
			upload.add(archive);
		} catch (Exception e) {
			aborted = true;
			throw e;
		}
	}

	protected Archive resolve(Archive archive) throws Exception {
		return archive;
	}

	@Override
	public void add(Archive archive, File in) throws Exception {
		try (FileInputStream fin = new FileInputStream(in)) {
			add(archive, fin);
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
