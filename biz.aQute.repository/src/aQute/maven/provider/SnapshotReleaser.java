package aQute.maven.provider;

import java.io.File;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.SnapshotVersion;

public class SnapshotReleaser extends Releaser {

	private MavenVersion	snapshotVersion;
	private long			timestamp	= System.currentTimeMillis();
	private String			build		= "1";
	private String			dateStamp;

	public SnapshotReleaser(MavenRepository home, Revision revision, MavenRemoteRepository snapshot) throws Exception {
		super(home, revision, snapshot);
		force();
		assert revision.isSnapshot();
		setBuild(timestamp, build);
	}

	@Override
	protected void check() {}

	@Override
	public void setBuild(long timestamp, String build) {
		this.timestamp = timestamp == 0 ? System.currentTimeMillis() : timestamp;
		this.build = build == null ? "1" : build;
		snapshotVersion = revision.version.toSnapshot(timestamp, build);
		dateStamp = MavenVersion.toDateStamp(timestamp);
	}

	public void updateMetadata() throws Exception {
		final RevisionMetadata revisionMetadata;
		revisionMetadata = localOnly ? new RevisionMetadata() : repo.getMetadata(revision);
		revisionMetadata.group = revision.group;
		revisionMetadata.artifact = revision.artifact;
		revisionMetadata.version = revision.version;
		revisionMetadata.lastUpdated = programMetadata.lastUpdated;
		revisionMetadata.snapshot.buildNumber = build;
		revisionMetadata.snapshot.timestamp = dateStamp;
		for (Archive archive : upload) {
			SnapshotVersion snapshotVersion = new SnapshotVersion();
			snapshotVersion.extension = archive.extension;
			snapshotVersion.classifier = archive.classifier.isEmpty() ? null : archive.classifier;
			snapshotVersion.updated = programMetadata.lastUpdated;
			snapshotVersion.value = this.snapshotVersion;
			revisionMetadata.snapshotVersions.add(snapshotVersion);
		}

		File metafile = home.toLocalFile(revision.metadata(repo.id));
		metafile.getParentFile().mkdirs();
		IO.store(revisionMetadata.toString(), metafile);
		repo.store(metafile, revision.metadata());

		super.updateMetadata();
	}

	protected Archive resolve(Archive archive) throws Exception {
		Archive resolved = archive.resolveSnapshot(snapshotVersion);
		return resolved;
	}

}
