package aQute.maven.repo.api;

import aQute.bnd.version.MavenVersion;

public class Revision {
	public final Program		program;
	public final String			group;
	public final String			artifact;
	public final MavenVersion	version;
	public final String			path;

	Revision(Program program, MavenVersion version) {
		this.program = program;
		this.group = program.group;
		this.artifact = program.artifact;
		this.version = version;
		this.path = program.path + "/" + version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + super.hashCode();
		result = prime * result + version.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		Revision other = (Revision) obj;

		if (!version.equals(other.version))
			return false;

		if (!program.equals(other.program))
			return false;

		return true;
	}

	public StringBuilder toStringBuilder(StringBuilder sbb) {
		StringBuilder sb = program.toStringBuilder(sbb);
		sb.append("@");
		sb.append(version);
		return sb;
	}

	public boolean isSnapshot() {
		return version.isSnapshot();
	}

	public Archive archive(String extension, String classifier) {
		return new Archive(this, null, extension, classifier);
	}

	public Archive archive(MavenVersion version, String extension, String classifier) {
		return new Archive(this, version, extension, classifier);
	}

	public String metadata() {
		return path + "/maven-metadata.xml";
	}

	public String metadata(String id) {
		return path + "/maven-metadata-" + id + ".xml";
	}

	public String toString() {
		return program.toString() + "@" + version;
	}

	public Archive pomArchive() {
		return archive("pom", null);
	}
}
