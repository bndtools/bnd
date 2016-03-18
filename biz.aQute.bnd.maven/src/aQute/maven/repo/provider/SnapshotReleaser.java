package aQute.maven.repo.provider;

import java.io.File;
import java.io.InputStream;

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
		File metafile = home.toLocalFile(revision.metadata(home.id));
		metafile.getParentFile().mkdirs();
		IO.store(revisionMetadata.toString(), metafile);
		home.release.store(metafile, revision.metadata());

		super.updateMetadata();
	}

	@Override
	public void add(Archive archive, InputStream in) throws Exception {
		try {
			File to = IO.getFile(tmp, archive.getName(getSnapshotVersion()));
			IO.copy(in, to);
			upload.add(to);

			SnapshotVersion snapshotVersion = new SnapshotVersion();
			snapshotVersion.extension = archive.extension;
			snapshotVersion.classifier = archive.classifier.isEmpty() ? null : archive.classifier;
			snapshotVersion.updated = programMetadata.lastUpdated;
			snapshotVersion.value = getSnapshotVersion();
			revisionMetadata.snapshotVersions.add(snapshotVersion);

		} catch (Exception e) {
			aborted = true;
			throw e;
		}
	}

	private MavenVersion getSnapshotVersion() {
		if (snapshotVersion == null) {
			long tstamp = System.currentTimeMillis();
			snapshotVersion = revision.version.toSnapshot(tstamp, null);
		}
		return snapshotVersion;
	}

}
