package aQute.maven.repo.api;

import aQute.bnd.version.MavenVersion;

public class Archive {
	public final Revision		revision;
	public final String			classifier;
	public final String			extension;
	public final String			localPath;
	public final String			remotePath;
	public final MavenVersion	snapshot;

	Archive(Revision revision, MavenVersion snapshot, String extension, String classifier) {
		this.revision = revision;
		this.extension = extension == null ? "jar" : extension;
		this.classifier = classifier == null ? "" : classifier;
		this.snapshot = snapshot;
		this.localPath = revision.path + "/" + getName();
		this.remotePath = revision.path + "/" + getName(snapshot);
	}

	public StringBuilder toStringBuilder(StringBuilder sbb) {
		StringBuilder sb = revision.toStringBuilder(sbb);

		sb.append(":");

		if (extension != null)
			sb.append(extension);

		sb.append(":");
		if (classifier != null)
			sb.append(classifier);

		return sb;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + ((extension == null) ? 0 : extension.hashCode());
		result = prime * result + ((snapshot == null) ? 0 : snapshot.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		Archive other = (Archive) obj;

		if (!this.classifier.equals(other.classifier))
			return false;

		if (!this.extension.equals(other.extension))
			return false;

		if (!this.revision.equals(other.revision))
			return false;

		if (this.snapshot != other.snapshot) {
			if (this.snapshot == null)
				return false;

			if (!this.snapshot.equals(other.snapshot))
				return false;
		}

		return true;
	}

	public Revision getRevision() {
		return revision;
	}

	public String getName() {
		return getName(revision.version);
	}

	public boolean isSnapshot() {
		return revision.isSnapshot();
	}

	public String getName(MavenVersion version) {
		return revision.program.artifact + "-" + (version == null ? revision.version : version)
				+ (this.classifier.isEmpty() ? "" : "-" + this.classifier)
				+ "." + this.extension;
	}

	public String toString() {
		return revision.program.toString() + "@" + (snapshot == null ? revision.version : snapshot) + ":" + extension
				+ (classifier.isEmpty() ? "" : ":" + classifier);
	}

	public boolean isResolved() {
		return !isSnapshot() || snapshot != null;
	}

	public Archive resolveSnapshot(MavenVersion version) {
		if (version.equals(this.snapshot))
			return this;

		return new Archive(revision, version, extension, classifier);
	}
}
