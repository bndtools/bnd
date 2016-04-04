package aQute.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.http.HttpRequestException;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.service.reporter.Reporter;

public class MavenRepository implements IMavenRepo, Closeable {
	final File			base;
	final String		id;
	final MaventRemoteRepository	release;
	final MaventRemoteRepository	snapshot;
	final Executor		executor;
	final boolean		localOnly;
	final Reporter		reporter;

	long STALE_TIME = TimeUnit.DAYS.toMillis(1);

	public MavenRepository(File base, String id, MaventRemoteRepository release, MaventRemoteRepository snapshot, Executor executor,
			Reporter reporter, Callable<Boolean> callback) throws Exception {
		this.base = base;
		this.id = id;
		this.release = release;
		this.snapshot = snapshot == null ? release : snapshot;
		this.executor = executor == null ? Executors.newCachedThreadPool() : executor;
		this.localOnly = release == null && snapshot == null;
		this.reporter = reporter;
	}

	@Override
	public List<Revision> getRevisions(Program program) throws Exception {
		List<Revision> revisions = new ArrayList<>();

		if (release != null)
			release.getRevisions(program, revisions);

		if (snapshot != null && snapshot != release)
			snapshot.getRevisions(program, revisions);

		return revisions;
	}

	@Override
	public List<Archive> getSnapshotArchives(Revision revision) throws Exception {

		if (!revision.isSnapshot() || snapshot == null)
			return null;

		return snapshot.getSnapshotArchives(revision);
	}

	@Override
	public Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception {
		if (revision.isSnapshot()) {
			MavenVersion v = snapshot.getVersion(revision);
			return revision.archive(v, extension, classifier);

		} else {
			return revision.archive(extension, classifier);
		}
	}

	@Override
	public Release release(final Revision revision) throws Exception {
		reporter.trace("Release %s to %s", revision, this);
		final File target = toLocalFile(revision.path);
		final File temp = IO.createTempFile(base, revision.toString(), ".tmp");
		temp.mkdirs();
		Releaser r = revision.isSnapshot() ? new SnapshotReleaser(this, revision, snapshot)
				: new Releaser(this, revision, release);
		r.force();
		return r;
	}

	@Override
	public Promise<File> get(final Archive archive) throws Exception {
		final Deferred<File> deferred = new Deferred<>();
		final File file = toLocalFile(archive);

		if (localOnly || isFresh(file)) {
			if (file.isFile())
				deferred.resolve(file);
			else
				deferred.resolve(null);
		} else {
			executor.execute(new Runnable() {

				@Override
				public void run() {
					try {

						File f = get0(archive, file);
						deferred.resolve(f);
					} catch (Throwable e) {
						deferred.fail(e);
					}
				}

			});
		}
		return deferred.getPromise();
	}

	private boolean isFresh(File file) {
		if (!file.isFile())
			return false;

		long now = System.currentTimeMillis();
		long diff = now - file.lastModified();
		return diff < TimeUnit.DAYS.toMillis(1);
	}

	private File get0(Archive archive, File file) throws Exception {
		TaggedData result = null;

		if (archive.isSnapshot()) {
			Archive resolved = resolveSnapshot(archive);
			if (resolved == null) {
				// Cannot resolved snapshot
				if (file.isFile()) // use local copy
					return file;
				return null;
			}
			if (snapshot != null && resolved != null) {
				result = snapshot.fetch(resolved.remotePath, file);
			}
		} else {
			if (release != null)
				result = release.fetch(archive.remotePath, file);
		}

		if (result == null)
			throw new IllegalStateException("Neither release nor remote repo set");

		switch (result.getState()) {
			case NOT_FOUND :
				return null;
			case OTHER :
				throw new HttpRequestException((HttpURLConnection) result.getConnection());

			case UNMODIFIED :
			case UPDATED :
			default :
				return file;
		}
	}

	@Override
	public Archive resolveSnapshot(Archive archive) throws Exception {
		if (archive.isResolved())
			return archive;

		if (snapshot == null)
			return null;

		MavenVersion version = snapshot.getVersion(archive.revision);
		if (version == null)
			return null;

		return archive.resolveSnapshot(version);
	}

	public File toLocalFile(String path) {
		return IO.getFile(base, path);
	}

	@Override
	public File toLocalFile(Archive archive) {
		return toLocalFile(archive.localPath);
	}

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
	public void close() throws IOException {
		if (release != null)
			release.close();
	}

	@Override
	public URI toRemoteURI(Archive archive) throws Exception {
		if (archive.revision.isSnapshot()) {
			if (snapshot != null)
				return snapshot.toURI(archive.remotePath);
		} else {
			if (release != null)
				return release.toURI(archive.remotePath);
		}
		return toLocalFile(archive).toURI();
	}

	public void store(Archive archive, InputStream in) throws IOException {
		File file = IO.getFile(base, archive.localPath);
		IO.copy(in, file);
	}

	@Override
	public boolean refresh() throws IOException {
		// TODO
		return false;
	}

	@Override
	public String toString() {
		return "MavenStorage [base=" + base + ", id=" + id + ", release=" + release + ", snapshot=" + snapshot
				+ ", localOnly=" + localOnly + "]";
	}

	@Override
	public String getName() {
		return id;
	}

}
