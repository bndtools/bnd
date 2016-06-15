package aQute.maven.provider;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.reporter.slf4j.Slf4jReporter;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.service.reporter.Reporter;

public class MavenRepository implements IMavenRepo, Closeable {
	final File									base;
	final String								id;
	final List<MavenBackingRepository>			release		= new ArrayList<>();
	final List<MavenBackingRepository>			snapshot	= new ArrayList<>();
	final Executor								executor;
	final boolean								localOnly;
	final Reporter								reporter	= new Slf4jReporter();
	final WeakHashMap<Revision,Promise<POM>>	poms		= new WeakHashMap<>();
	long										STALE_TIME	= TimeUnit.DAYS.toMillis(1);

	public MavenRepository(File base, String id, List<MavenBackingRepository> release,
			List<MavenBackingRepository> snapshot, Executor executor, Reporter reporter, Callable<Boolean> callback)
					throws Exception {
		this.base = base;
		this.id = id;
		if (release != null)
			this.release.addAll(release);
		if (snapshot != null)
			this.snapshot.addAll(snapshot);

		this.executor = executor == null ? Executors.newCachedThreadPool() : executor;
		this.localOnly = this.release.isEmpty() && this.snapshot.isEmpty();
		// this.reporter = reporter;
		base.mkdirs();
	}

	@Override
	public List<Revision> getRevisions(Program program) throws Exception {
		List<Revision> revisions = new ArrayList<>();

		for (MavenBackingRepository mbr : release)
			mbr.getRevisions(program, revisions);

		for (MavenBackingRepository mbr : snapshot)
			if (!release.contains(mbr))
				mbr.getRevisions(program, revisions);

		return revisions;
	}

	@Override
	public List<Archive> getSnapshotArchives(Revision revision) throws Exception {

		if (!revision.isSnapshot())
			return null;

		List<Archive> archives = new ArrayList<>();
		for (MavenBackingRepository mbr : snapshot) {
			List<Archive> snapshotArchives = mbr.getSnapshotArchives(revision);
			archives.addAll(snapshotArchives);
		}

		return archives;
	}

	@Override
	public Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception {
		if (revision.isSnapshot()) {
			for (MavenBackingRepository mbr : snapshot) {
				MavenVersion v = mbr.getVersion(revision);
				if (v != null)
					return revision.archive(v, extension, classifier);
			}
			return null;
		} else {
			return revision.archive(extension, classifier);
		}
	}

	@Override
	public Release release(final Revision revision, final Properties context) throws Exception {
		reporter.trace("Release %s to %s", revision, this);
		if (revision.isSnapshot()) {
			return new SnapshotReleaser(this, revision, snapshot.isEmpty() ? null : snapshot.get(0), context);
		}
		return new Releaser(this, revision, release.get(0), context);
	}

	@Override
	public Promise<File> get(final Archive archive) throws Exception {
		return get(archive, true);
	}

	public Promise<File> get(final Archive archive, final boolean thrw) throws Exception {
		final Deferred<File> deferred = new Deferred<>();
		final File file = toLocalFile(archive);

		if (file.isFile() && !archive.isSnapshot()) {
			deferred.resolve(file);
			return deferred.getPromise();
		}

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
						if (thrw && f == null)
							throw new FileNotFoundException("" + archive);

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
		State result = null;

		if (archive.isSnapshot()) {
			Archive resolved = resolveSnapshot(archive);
			if (resolved == null) {
				// Cannot resolved snapshot
				if (file.isFile()) // use local copy
					return file;
				return null;
			}
			if (resolved != null) {
				result = fetch(snapshot, resolved.remotePath, file);
			}
		}

		if (result == null && release != null)
			result = fetch(release, archive.remotePath, file);

		if (result == null)
			throw new IllegalStateException("Neither release nor remote repo set");

		switch (result) {
			case NOT_FOUND :
				return null;
			case OTHER :
				throw new IOException("Could not fetch " + archive.toString());

			case UNMODIFIED :
			case UPDATED :
			default :
				return file;
		}
	}

	private State fetch(List<MavenBackingRepository> mbrs, String remotePath, File file) throws Exception {
		State error = State.NOT_FOUND;

		for (MavenBackingRepository mbr : mbrs) {
			TaggedData fetch = mbr.fetch(remotePath, file);
			switch (fetch.getState()) {
				case NOT_FOUND :
					break;
				case OTHER :
					error = State.OTHER;
					reporter.error("Fetching artifact gives error %s", remotePath);
					break;

				case UNMODIFIED :
				case UPDATED :
					return fetch.getState();
			}
		}
		return error;
	}

	@Override
	public Archive resolveSnapshot(Archive archive) throws Exception {
		if (archive.isResolved())
			return archive;

		for (MavenBackingRepository mbr : snapshot) {
			MavenVersion version = mbr.getVersion(archive.revision);
			if (version != null)
				return archive.resolveSnapshot(version);
		}

		return null;
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
		for (MavenBackingRepository mbr : snapshot)
			IO.close(mbr);
		for (MavenBackingRepository mbr : release)
			IO.close(mbr);
	}

	@Override
	public URI toRemoteURI(Archive archive) throws Exception {
		// if (archive.revision.isSnapshot()) {
		// if (snapshot != null)
		// return snapshot.toURI(archive.remotePath);
		// } else {
		// if (release != null)
		// return release.toURI(archive.remotePath);
		// }
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
		return "MavenRepository [base=" + base + ", id=" + id + ", release=" + release + ", snapshot=" + snapshot
				+ ", localOnly=" + localOnly + "]";
	}

	@Override
	public String getName() {
		return id;
	}

	@Override
	public POM getPom(InputStream pomFile) throws Exception {
		return new POM(this, pomFile);
	}

	@Override
	public POM getPom(Revision revision) throws Exception {
		Deferred<POM> deferred;
		synchronized (poms) {
			Promise<POM> p = poms.get(revision);
			if (p != null)
				return p.getValue();

			deferred = new Deferred<>();
			poms.put(revision, deferred.getPromise());
		}
		try {
			Archive pomArchive = revision.getPomArchive();
			File pomFile = get(pomArchive, false).getValue();

			if (pomFile == null) {
				deferred.resolve(null);
			} else {

				try (FileInputStream fin = new FileInputStream(pomFile)) {
					POM pom = getPom(fin);
					deferred.resolve(pom);
				} catch (Exception e) {
					reporter.exception(e, "Failed to parse pom %s from file %s", revision, pomFile);
					deferred.resolve(null);
				}
			}
		} catch (Throwable t) {
			deferred.fail(t);
		}
		return deferred.getPromise().getValue();
	}

	@Override
	public List<MavenBackingRepository> getSnapshotRepositories() {
		return Collections.unmodifiableList(snapshot);
	}

	@Override
	public List<MavenBackingRepository> getReleaseRepositories() {
		return Collections.unmodifiableList(release);
	}

	@Override
	public boolean isLocalOnly() {
		return localOnly;
	}

	@Override
	public boolean exists(Archive archive) throws Exception {
		Promise<File> promise = get(archive.getPomArchive(), false);
		File value = promise.getValue();
		return value != null;
	}

	public void clear(Revision revision) {
		synchronized (poms) {
			poms.remove(revision);
		}
	}
}
