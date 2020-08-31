package aQute.maven.provider;

import static java.util.stream.Collectors.toList;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.version.MavenVersion;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.maven.api.Archive;
import aQute.maven.api.IMavenRepo;
import aQute.maven.api.Program;
import aQute.maven.api.Release;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.ProgramMetadata;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.Snapshot;
import aQute.service.reporter.Reporter;

public class MavenRepository implements IMavenRepo, Closeable {
	final static Logger								logger			= LoggerFactory.getLogger(MavenRepository.class);
	private static final long						LEASE_RELEASE	= TimeUnit.DAYS.toMillis(7);
	private static final long						LEASE_SNAPSHOT	= TimeUnit.HOURS.toMillis(1);
	private final File								base;
	private final String							id;
	private final List<MavenBackingRepository>		release			= new ArrayList<>();
	private final List<MavenBackingRepository>		snapshot		= new ArrayList<>();
	private final List<MavenBackingRepository>		combined		= new ArrayList<>();
	private final PromiseFactory					promiseFactory;
	private final boolean							localOnly;
	private final Map<Revision, Promise<POM>>		poms			= new WeakHashMap<>();
	private final Reporter							reporter;
	private final Map<Revision, RevisionMetadata>	revisions		= new ConcurrentHashMap<>();
	private final Map<Program, ProgramMetadata>		programs		= new ConcurrentHashMap<>();
	private final HttpClient						client;

	public MavenRepository(File base, String id, List<MavenBackingRepository> release,
		List<MavenBackingRepository> snapshot, HttpClient client, Reporter reporter)
		throws Exception {
		this.base = base;
		this.id = id;
		this.client = client;
		if (release != null)
			this.release.addAll(release);
		if (snapshot != null)
			this.snapshot.addAll(snapshot);

		this.promiseFactory = new PromiseFactory(Objects.requireNonNull(client.promiseFactory()
			.executor()));
		this.localOnly = this.release.isEmpty() && this.snapshot.isEmpty();
		IO.mkdirs(base);
		this.reporter = reporter;
		this.combined.addAll(this.release);
		this.combined.addAll(this.snapshot);
	}

	@Override
	public List<Revision> getRevisions(Program program) throws Exception {
		Optional<ProgramMetadata> meta = getMetadata(program);
		if (meta.isPresent())
			return meta.get().versions.stream()
				.map(v -> program.version(v))
				.collect(Collectors.toList());
		return Collections.emptyList();
	}

	private Optional<ProgramMetadata> getMetadata(Program program) throws Exception {
		ProgramMetadata metadata = programs.get(program);
		if (metadata != null) {
			if (!isStale(metadata.lastModified, LEASE_RELEASE)) {
				if (metadata.notfound)
					return Optional.empty();
				return Optional.of(metadata);
			}
		}

		File metafile = IO.getFile(base, program.metadata(id));

		State state = fetch(combined, program.metadata(), metafile);

		switch (state) {
			case NOT_FOUND :
				metadata = new ProgramMetadata();
				metadata.notfound = true;
				programs.put(program, metadata);
				return null;

			case OTHER :
				throw new IOException(); // never gets here

			case UNMODIFIED :
				if (metadata != null)
					return Optional.of(metadata);

				// fall thru

			case UPDATED :
			default :
				metadata = MetadataParser.parseProgramMetadata(metafile);
				programs.put(program, metadata);
				return Optional.of(metadata);
		}
	}

	@Override
	public List<Archive> getSnapshotArchives(Revision revision) throws Exception {

		if (!revision.isSnapshot())
			return null;

		Optional<RevisionMetadata> metadata = getMetadata(revision);
		if (!metadata.isPresent()) {
			return Collections.emptyList();
		}

		return metadata.get().snapshotVersions.stream()
			.map(snapshotVersion -> revision.archive(snapshotVersion.value, snapshotVersion.extension,
				snapshotVersion.classifier))
			.collect(toList());
	}

	@Override
	public Archive getResolvedArchive(Revision revision, String extension, String classifier) throws Exception {
		if (!revision.isSnapshot())
			return revision.archive(extension, classifier);

		Optional<RevisionMetadata> metadata = getMetadata(revision);
		if (!metadata.isPresent()) {
			reporter.error("No metadata for revision %s", revision);
			return null;
		}

		Snapshot snapshot = metadata.get().snapshot;
		if (snapshot.timestamp == null || snapshot.buildNumber == null) {
			reporter.warning("Snapshot and/or buildnumber not set %s in %s", snapshot, revision);
			return null;
		}

		MavenVersion version = revision.version.toSnapshot(snapshot.timestamp, snapshot.buildNumber);
		return revision.archive(extension, classifier)
			.resolveSnapshot(version);
	}

	@Override
	public Archive resolveSnapshot(Archive archive) throws Exception {
		if (archive.isResolved())
			return archive;

		return getResolvedArchive(archive.revision, archive.extension, archive.classifier);
	}

	@Override
	public Release release(final Revision revision, final Properties context) throws Exception {
		logger.debug("Release {} to {}", revision, this);
		if (revision.isSnapshot()) {
			return new SnapshotReleaser(this, revision, snapshot.isEmpty() ? null : snapshot.get(0), context);
		}
		return new Releaser(this, revision, release.isEmpty() ? null : release.get(0), context);
	}

	@Override
	public Promise<File> get(final Archive archive) throws Exception {
		return get(archive, true);
	}

	private Promise<File> get(Archive archive, boolean thrw) throws Exception {
		final File file = toLocalFile(archive);

		if (file.isFile() && !archive.isSnapshot() && !isStale(file.lastModified(), LEASE_RELEASE)) {
			return promiseFactory.resolved(file);
		}

		if (localOnly) {
			return promiseFactory.resolved(file.isFile() ? file : null);
		}
		return promiseFactory.submit(() -> {
			File f = getFile(archive, file);
			if (thrw && f == null) {
				throw new FileNotFoundException(
					"For Maven artifact " + archive + " from " + (archive.isSnapshot() ? snapshot : release));
			}
			return f;
		});
	}

	private File getFile(Archive archive, File file) throws Exception {
		State result = null;

		if (archive.isSnapshot()) {
			Archive resolved = resolveSnapshot(archive);
			if (resolved == null) {
				// Cannot resolved snapshot
				if (file.isFile()) // use local copy
					return file;
				return null;
			}
			result = fetch(snapshot, resolved.remotePath, file);
		}

		if (result == null)
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
					logger.error("Fetching artifact gives error {} : {} {}", remotePath, fetch.getResponseCode(),
						fetch);
					break;

				case UNMODIFIED :
				case UPDATED :
					return fetch.getState();
			}
		}
		return error;
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

		return Program.valueOf(group, artifact)
			.version(version)
			.archive(extension, classifier);
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
		if (archive.revision.isSnapshot()) {
			if (!snapshot.isEmpty()) {
				return snapshot.get(0)
					.toURI(archive.remotePath);
			}
		} else if (!release.isEmpty()) {
			return release.get(0)
				.toURI(archive.remotePath);
		}
		return toLocalFile(archive).toURI();
	}

	public void store(Archive archive, InputStream in) throws IOException {
		File file = IO.getFile(base, archive.localPath);
		IO.copy(in, file);
	}

	@Override
	public boolean refresh() throws IOException {
		programs.clear();
		revisions.clear();
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
		return new POM(this, pomFile, true);
	}

	private POM getPom(File pomFile) throws Exception {
		return new POM(this, pomFile, true);
	}

	@Override
	public POM getPom(Revision revision) throws Exception {
		if (revision == null) {
			return null;
		}
		return getPomPromise(revision).getValue();
	}

	private Promise<POM> getPomPromise(final Revision revision) throws Exception {
		Deferred<POM> deferred;
		synchronized (poms) {
			Promise<POM> promise = poms.get(revision);
			if (promise != null) {
				return promise;
			}
			deferred = promiseFactory.deferred();
			poms.put(revision, deferred.getPromise());
		}
		Archive pomArchive = revision.getPomArchive();
		deferred.resolveWith(get(pomArchive, false).map(pomFile -> {
			if (pomFile == null) {
				return null;
			}
			try {
				return getPom(pomFile);
			} catch (Exception e) {
				logger.error("Failed to parse pom {} from file {}", revision, pomFile, e);
				return null;
			}
		}));
		return deferred.getPromise();
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
		File file = File.createTempFile(Archive.POM_EXTENSION, ".xml");
		try {
			File result = getFile(archive.getPomArchive(), file);
			return result != null;
		} catch (Exception e) {
			return false;
		} finally {
			IO.delete(file);
		}
	}

	public void clear(Revision revision) {
		synchronized (poms) {
			revisions.remove(revision);
			poms.remove(revision);
		}
	}

	@Override
	public void refresh(Archive archive) {
		revisions.remove(archive.revision);
	}

	Optional<RevisionMetadata> getMetadata(Revision revision) throws Exception {

		long lease = revision.isSnapshot() ? LEASE_SNAPSHOT : LEASE_RELEASE;

		RevisionMetadata metadata = revisions.get(revision);

		if (metadata != null) {
			if (!isStale(metadata.lastModified, lease)) {
				if (metadata.notfound)
					return Optional.empty();
				else
					return Optional.of(metadata);
			}
		}

		File metafile = toLocalFile(revision.metadata(id));

		boolean requiresFetch = //
			!metafile.isFile() //
				|| revision.isSnapshot() //
				|| isStale(metafile.lastModified(), LEASE_RELEASE);

		if (requiresFetch) {
			State tag;
			if (revision.isSnapshot()) {
				tag = fetch(snapshot, revision.metadata(), metafile);
				if (tag == State.NOT_FOUND)
					tag = fetch(release, revision.metadata(), metafile);
			}
			else
				tag = fetch(combined, revision.metadata(), metafile);

			if (tag == State.NOT_FOUND || tag == State.OTHER || !metafile.isFile()) {
				metadata = new RevisionMetadata();
				metadata.notfound = true;
				revisions.put(revision, metadata);
				return Optional.empty();
			}
		}

		metadata = MetadataParser.parseRevisionMetadata(metafile);
		revisions.put(revision, metadata);
		return Optional.of(metadata);
	}

	private boolean isStale(long lastModified, long lease) {
		return lastModified + lease < System.currentTimeMillis();
	}

	@Override
	public boolean isStale(Archive archive) {
		if (archive == null)
			return true;

		File file = toLocalFile(archive);
		if (file == null)
			return true;

		if (!file.isFile())
			return true;

		long lease = archive.isSnapshot() ? LEASE_SNAPSHOT : LEASE_RELEASE;
		return isStale(file.lastModified(), lease);
	}

	@Override
	public boolean isRemote() {
		boolean remote = false;
		for (MavenBackingRepository mbr : combined) {
			if (mbr.isRemote()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void validateUris(Formatter f) {
		combined.stream()
			.map(mb -> {
				try {
					return mb.toURI("");
				} catch (Exception e) {
					f.format("Invalid url %s : %s\n", mb, Exceptions.causes(e));
					return null;
				}
			})
			.filter(Objects::nonNull)
			.forEach(u -> {
				String validateURI = client.validateURI(u);
				if (validateURI != null)
					f.format("%s : %s\n", u, validateURI);
			});
	}

}
