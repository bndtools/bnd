package aQute.maven.api;

import java.nio.file.Path;

import aQute.bnd.version.MavenVersion;

public class Revision implements Comparable<Revision> {
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
		result = prime * result + program.hashCode();
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

	@Override
	public String toString() {
		return program.toString() + ":" + version;
	}

	public Archive pomArchive() {
		return archive(Archive.POM_EXTENSION, null);
	}

	public static Revision fromProjectPath(Path projectDirPath) {
		int l = projectDirPath.getNameCount();
		if (l < 3)
			return null;

		String version = projectDirPath.getName(l - 1)
			.toString();
		String artifact = projectDirPath.getName(l - 2)
			.toString();
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < l - 2; i++) {
			sb.append(del)
				.append(projectDirPath.getName(i));
			del = ".";
		}
		String group = sb.toString();

		return Program.valueOf(group, artifact)
			.version(version);
	}

	public Archive getPomArchive() {
		return archive(Archive.POM_EXTENSION, null);
	}

	@Override
	public int compareTo(Revision o) {
		int n = program.compareTo(o.program);
		if (n != 0)
			return n;

		return version.compareTo(o.version);
	}

	public static Revision valueOf(String s) {
		if (s == null)
			return null;

		String[] parts = s.split(":");
		if (parts.length != 3)
			return null;

		return Program.valueOf(parts[0], parts[1])
			.version(parts[2]);

	}

}
