package aQute.maven.provider;

import java.io.File;
import java.util.Properties;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.Revision;
import aQute.maven.provider.MetadataParser.RevisionMetadata;
import aQute.maven.provider.MetadataParser.SnapshotVersion;

public class SnapshotReleaser extends Releaser {

	private MavenVersion		snapshotVersion;
	private String				build;
	private String				dateStamp;
	private RevisionMetadata	revisionMetadata;

	public SnapshotReleaser(MavenRepository home, Revision revision, MavenBackingRepository snapshot,
		Properties context) throws Exception {
		super(home, revision, snapshot, context);
		revisionMetadata = localOnly || repo == null ? new RevisionMetadata() : repo.getMetadata(revision);
		force();
		assert revision.isSnapshot();
		setBuild(0, null);
	}

	@Override
	protected void check() {}

	@Override
	public void setBuild(long timestamp, String build) {
		timestamp = timestamp == 0 ? System.currentTimeMillis() : timestamp;
		if (build == null) {
			build = nextBuildNumber();
		}
		this.build = build;
		snapshotVersion = revision.version.toSnapshot(timestamp, build);
		dateStamp = MavenVersion.toDateStamp(timestamp);
	}

	private String nextBuildNumber() {
		try {
			return "" + (Integer.parseInt(revisionMetadata.snapshot.buildNumber) + 1);
		} catch (Exception e) {
			return "1";
		}
	}

	@Override
	public void updateMetadata() throws Exception {
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
		IO.mkdirs(metafile.getParentFile());
		IO.store(revisionMetadata.toString(), metafile);
		repo.store(metafile, revision.metadata());

		super.updateMetadata();
	}

	@Override
	protected Archive resolve(Archive archive) throws Exception {
		Archive resolved = archive.resolveSnapshot(snapshotVersion);
		return resolved;
	}

	@Override
	protected boolean isUpdateProgramMetadata() {
		return true;
	}

	@Override
	public void sign(Archive archive, File f) throws Exception {}

	/*
	 * Snapshots are never signed
	 */
	@Override
	public void setPassphrase(String passphrase) {
		// ignore
	}

}
