package aQute.maven.repo.provider;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.maven.repo.api.Archive;
import aQute.maven.repo.api.Revision;
import aQute.maven.repo.provider.MetadataParser.RevisionMetadata;
import aQute.maven.repo.provider.MetadataParser.SnapshotVersion;

public class SnapshotReleaser extends Releaser {

	final RevisionMetadata revisionMetadata;

	MavenVersion snapshotVersion;

	public SnapshotReleaser(MavenStorage home, Revision revision) throws Exception {
		super(home, revision);
		force();


		assert revision.isSnapshot();
		revisionMetadata = localOnly ? new RevisionMetadata() : home.getMetadata(revision);
		revisionMetadata.group = revision.group;
		revisionMetadata.artifact = revision.artifact;
		revisionMetadata.lastUpdated = programMetadata.lastUpdated;

		setBuild(System.currentTimeMillis(), null);
	}

	public void updateMetadata() throws Exception {
		File metafile = home.toFile(revision.metadata(home.id));
		metafile.getParentFile().mkdirs();
		IO.store(revisionMetadata.toString(), metafile);
		home.remote.store(metafile, revision.metadata());

		super.updateMetadata();
	}

	@Override
	public void add(Archive archive, InputStream in) throws Exception {
		try {
			File to = IO.getFile(tmp, archive.getName(snapshotVersion));
			IO.copy(in, to);
			upload.add(to);

			SnapshotVersion snapshotVersion = new SnapshotVersion();
			snapshotVersion.extension = archive.extension;
			snapshotVersion.classifier = archive.classifier.isEmpty() ? null : archive.classifier;
			snapshotVersion.updated = programMetadata.lastUpdated;
			snapshotVersion.value = this.snapshotVersion;
			revisionMetadata.snapshotVersions.add(snapshotVersion);

		} catch (Exception e) {
			aborted = true;
			throw e;
		}
	}

	@Override
	public void setBuild(String timestamp, String build) {
		this.snapshotVersion = revision.version.toSnapshot(timestamp, build);
	}

	@Override
	public void setBuild(long timestamp, String build) {
		String t = MetadataParser.snapshotTimestamp.format(new Date(timestamp));
		this.snapshotVersion = revision.version.toSnapshot(t, build);
	}
}
