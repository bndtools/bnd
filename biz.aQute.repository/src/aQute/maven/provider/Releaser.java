package aQute.maven.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.maven.nexus.provider.Signer;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.SnapshotVersion;

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
	private Properties					context;
	private String						passphrase;

	Releaser(MavenRepository home, Revision revision, MavenBackingRepository repo, Properties context)
		throws Exception {
		this.home = home;
		this.revision = revision;
		this.repo = repo;
		this.context = context;
		this.dir = home.toLocalFile(revision.path);

		IO.delete(this.dir);
		check();
		IO.mkdirs(this.dir);
	}

	protected void check() {}

	@Override
	public void close() throws IOException {
		try {
			if (!aborted) {
				RevisionMetadata localMetadata = localMetadata();
				File metafile = home.toLocalFile(revision.metadata("local"));
				IO.mkdirs(metafile.getParentFile());
				IO.store(localMetadata.toString(), metafile);

				if (!localOnly) {
					uploadAll(upload.iterator());
					updateMetadata();
				}
				home.clear(revision);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected RevisionMetadata localMetadata() {
		RevisionMetadata revisionMetadata = new RevisionMetadata();
		revisionMetadata.group = revision.group;
		revisionMetadata.artifact = revision.artifact;
		revisionMetadata.version = revision.version;
		revisionMetadata.lastUpdated = programMetadata.lastUpdated;
		revisionMetadata.snapshot.buildNumber = null;
		revisionMetadata.snapshot.localCopy = true;
		revisionMetadata.snapshot.timestamp = null;
		for (Archive archive : upload) {
			SnapshotVersion snapshotVersion = new SnapshotVersion();
			snapshotVersion.extension = archive.extension;
			snapshotVersion.classifier = archive.classifier.isEmpty() ? null : archive.classifier;
			snapshotVersion.updated = programMetadata.lastUpdated;
			snapshotVersion.value = revision.version;
			revisionMetadata.snapshotVersions.add(snapshotVersion);
		}
		return revisionMetadata;
	}

	protected void updateMetadata() throws Exception, InterruptedException {
		if (!isUpdateProgramMetadata())
			return;

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

	/**
	 * Nexus does not like us to update the program metadata but we should do
	 * this for file repos
	 *
	 * @return
	 */
	protected boolean isUpdateProgramMetadata() {
		return repo.isFile();
	}

	void uploadAll(Iterator<Archive> iterator) throws Exception {

		if (!iterator.hasNext())
			return;

		Archive archive = iterator.next();
		File f = home.toLocalFile(archive);
		try {
			repo.store(f, archive.remotePath);
			if (passphrase != null)
				sign(archive, f);
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

	public void sign(Archive archive, File f) throws Exception {
		if (passphrase == null)
			return;

		File sign = new File(archive.localPath + ".asc");
		int result = Signer.sign(f, context.getProperty("gpg", "gpg"), passphrase.equals("DEFAULT") ? null : passphrase,
			sign);
		if (result == 0) {
			repo.store(sign, archive.remotePath + ".asc");
		} else {
			MavenRepository.logger.error("Failed to sign {} result code {}", f, result);
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
		try (InputStream fin = IO.stream(in)) {
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

	@Override
	public void force() {
		force = true;
	}

	@Override
	public void add(String extension, String classifier, InputStream in) throws Exception {
		Archive a = revision.archive(extension, classifier);
		add(a, in);
	}

	@Override
	public void setBuild(long timestamp, String build) throws Exception {
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

	@Override
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

}
