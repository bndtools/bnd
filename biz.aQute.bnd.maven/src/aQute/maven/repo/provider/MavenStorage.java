package aQute.maven.repo.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.IMavenRepo;
import aQute.maven.repo.api.Program;
import aQute.maven.repo.api.Release;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MetadataParser.ProgramMetadata;
import aQute.maven.repo.provider.MetadataParser.RevisionMetadata;
import aQute.maven.repo.provider.MetadataParser.SnapshotVersion;

public class MavenStorage implements IMavenRepo {
	final File								base;
	final String							id;
	final RemoteRepo						remote;
	final Map<Revision,RevisionMetadata>	revisions	= new ConcurrentHashMap<>();
	final Map<Program,ProgramMetadata>		programs	= new ConcurrentHashMap<>();

	long STALE_TIME = TimeUnit.DAYS.toMillis(1);

	public MavenStorage(File base, String id, RemoteRepo remote) {
		this.base = base;
		this.id = id;
		this.remote = remote;
	}

	@Override
	public List<Revision> getRevisions(Program program) throws Exception {
		ProgramMetadata metadata = getMetadata(program);

		List<Revision> revisions = new ArrayList<>();
		for (MavenVersion version : metadata.versions) {
			Revision revision = program.version(version);
			revisions.add(revision);
		}

		return revisions;
	}

	@Override
	public List<Archive> getSnapshotArchives(Revision revision) throws Exception {

		if (!revision.isSnapshot())
			return null;

		RevisionMetadata metadata = getMetadata(revision);
		List<Archive> archives = new ArrayList<>();
		for (SnapshotVersion snapshotVersion : metadata.snapshotVersions) {
			Archive archive = revision.archive(snapshotVersion.value, snapshotVersion.extension,
					snapshotVersion.classifier);
			archives.add(archive);
		}

		return archives;
	}

	@Override
	public Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception {
		if (revision.isSnapshot()) {
			RevisionMetadata metadata = getMetadata(revision);
			MavenVersion v = revision.version.toSnapshot(metadata.snapshot.timestamp, metadata.snapshot.buildNumber);

			return revision.archive(v, extension, classifier);

		} else {
			return revision.archive(extension, classifier);
		}
	}

	@Override
	public Release release(final Revision revision) throws Exception {
		final File target = toFile(revision.path);
		final File temp = IO.createTempFile(base, revision.toString(), ".tmp");
		temp.mkdirs();
		Releaser r = revision.isSnapshot() ? new SnapshotReleaser(this, revision) : new Releaser(this, revision);
		r.force();
		return r;
	}

	@Override
	public File get(Archive archive) throws Exception {
		if (archive.isSnapshot())
			archive = resolveSnapshot(archive);

		File file = toFile(archive.localPath);
		if (!file.isFile() || (archive.isSnapshot() && isStale(file)))
			remote.fetch(archive.remotePath, file);

		if (file.isFile())
			return file;

		return null;
	}

	@Override
	public Archive resolveSnapshot(Archive archive) throws Exception {
		if (archive.isResolved())
			return archive;

		Revision revision = archive.getRevision();
		RevisionMetadata metadata = getMetadata(revision);
		MavenVersion v = revision.version.toSnapshot(metadata.snapshot.timestamp, metadata.snapshot.buildNumber);
		return archive.resolveSnapshot(v);
	}

	RevisionMetadata getMetadata(Revision revision) throws Exception {
		File metafile = toFile(revision.metadata(id));
		RevisionMetadata metadata = revisions.get(revision);

		if (isStale(metafile)) {

			if (!remote.fetch(revision.metadata(), metafile))
				return metadata;

			metadata = null;
		}
		if (metadata == null)
			metadata = MetadataParser.parseRevisionMetadata(metafile);

		revisions.put(revision, metadata);
		return metadata;
	}

	ProgramMetadata getMetadata(Program program) throws Exception {
		File metafile = toFile(program.metadata(id));
		ProgramMetadata metadata = programs.get(program);

		if (isStale(metafile)) {

			if (!remote.fetch(program.metadata(), metafile))
				return metadata;

			metadata = null;
		}
		if (metadata == null)
			metadata = MetadataParser.parseProgramMetadata(metafile);

		programs.put(program, metadata);
		return metadata;
	}

	private boolean isStale(File file) {
		return !file.isFile() || isStale(file.lastModified());
	}

	private boolean isStale(long lastModified) {
		return System.currentTimeMillis() > lastModified + STALE_TIME;
	}

	File toFile(String path) {
		return IO.getFile(base, path);
	}

	@Override
	public long getLastUpdated(Revision revision) throws Exception {
		if (revision.isSnapshot()) {
			File metafile = toFile(revision.metadata(id));
			return metafile.lastModified();
		} else {
			File dir = toFile(revision.path);
			return dir.lastModified();
		}
	}

}
