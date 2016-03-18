package aQute.maven.repo.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.IMavenRepo;
import aQute.maven.repo.api.Program;
import aQute.maven.repo.api.Release;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MetadataParser.ProgramMetadata;
import aQute.maven.repo.provider.MetadataParser.RevisionMetadata;
import aQute.maven.repo.provider.MetadataParser.SnapshotVersion;
import aQute.service.reporter.Reporter;

public class MavenStorage implements IMavenRepo, Closeable {
	final File								base;
	final String							id;
	final RemoteRepo						release;
	final RemoteRepo						snapshot;
	final Map<Revision,RevisionMetadata>	revisions	= new ConcurrentHashMap<>();
	final Map<Program,ProgramMetadata>		programs	= new ConcurrentHashMap<>();
	final Executor							executor;
	private final LocalRepoWatcher			repoWatcher;

	long STALE_TIME = TimeUnit.DAYS.toMillis(1);

	public MavenStorage(File base, String id, RemoteRepo release, RemoteRepo snapshot, Executor executor,
			Reporter reporter, Callable<Boolean> callback) throws Exception {
		this.base = base;
		this.id = id;
		this.release = release;
		this.snapshot = snapshot == null ? release : snapshot;
		this.executor = executor == null ? Executors.newCachedThreadPool() : executor;
		repoWatcher = new LocalRepoWatcher(executor, base, reporter, callback);
		repoWatcher.open();
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
		final File target = toLocalFile(revision.path);
		final File temp = IO.createTempFile(base, revision.toString(), ".tmp");
		temp.mkdirs();
		Releaser r = revision.isSnapshot() ? new SnapshotReleaser(this, revision) : new Releaser(this, revision);
		r.force();
		return r;
	}

	@Override
	public Promise<File> get(final Archive archive) throws Exception {
		final Deferred<File> deferred = new Deferred<>();
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Archive local = resolveSnapshot(archive);

					File file = toLocalFile(archive);
					if (!file.isFile() || (archive.isSnapshot() && isStale(file)))
						release.fetch(archive.remotePath, file);

					if (file.isFile())
						deferred.resolve(file);
					else
						deferred.resolve(null);

				} catch (Throwable e) {
					deferred.fail(e);
				}
			}

		});
		return deferred.getPromise();
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
		File metafile = toLocalFile(revision.metadata(id));
		RevisionMetadata metadata = revisions.get(revision);

		if (isStale(metafile)) {

			if (!release.fetch(revision.metadata(), metafile))
				return metadata;

			metadata = null;
		}
		if (metadata == null)
			metadata = MetadataParser.parseRevisionMetadata(metafile);

		revisions.put(revision, metadata);
		return metadata;
	}

	ProgramMetadata getMetadata(Program program) throws Exception {
		File metafile = toLocalFile(program.metadata(id));
		ProgramMetadata metadata = programs.get(program);

		if (isStale(metafile)) {

			if (!release.fetch(program.metadata(), metafile))
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

	public File toLocalFile(String path) {
		return IO.getFile(base, path);
	}

	@Override
	public File toLocalFile(Archive archive) {
		return toLocalFile(archive.localPath);
	}

	@Override
	public long getLastUpdated(Revision revision) throws Exception {
		if (revision.isSnapshot()) {
			File metafile = toLocalFile(revision.metadata(id));
			return metafile.lastModified();
		} else {
			File dir = toLocalFile(revision.path);
			return dir.lastModified();
		}
	}

	@Override
	public Archive getArchive(String s) throws Exception {
		Matcher matcher = ARCHIVE_P.matcher(Strings.trim(s));
		if (!matcher.matches())
			return null;

		String group = Strings.trim(matcher.group("group"));
		String artifact = Strings.trim(matcher.group("artifact"));
		String extension = Strings.trim(matcher.group("extension"));
		String classifier = Strings.trim(matcher.group("classifier"));
		String version = Strings.trim(matcher.group("version"));

		return Program.valueOf(group, artifact).version(version).archive(extension, classifier);
	}

	@Override
	public List<Program> getLocalPrograms() throws Exception {
		return repoWatcher.getLocalPrograms();
	}

	@Override
	public void close() throws IOException {
		if (release != null)
			release.close();
	}

	@Override
	public URI toRemoteURI(Archive archive) throws Exception {
		return archive.revision.isSnapshot() ? snapshot.toURI(archive.remotePath) : release.toURI(archive.remotePath);
	}

}
