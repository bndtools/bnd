package aQute.maven.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.version.MavenVersion;

public class Archive implements Comparable<Archive> {
	public static final Pattern	ARCHIVE_P			= Pattern.compile(																																																							//
		"\\s*"																																																																					// skip
																																																																								// whitespace
			+ "(?<program>[^:]+:[^:]+)         # program\n"																																																										//
			+ "(:(?<extension>[^:]+)         # optional extension\n"																																																							//
			+ "    (:(?<classifier>[^:]*))?  # optional classifer (must be preceded by extension)\n"																																															//
			+ ")?                            # end of extension\n"																																																								//
			+ ":(?<version>[^:]+)           # version is last\n"																																																								//
			+ "\\s*",																																																																			// skip
																																																																								// whitespace
		Pattern.COMMENTS);

	public static final String	SOURCES_CLASSIFIER	= "sources";
	public static final String	JAVADOC_CLASSIFIER	= "javadoc";
	public static final String	JAR_EXTENSION		= "jar";
	public static final String	DEFAULT_EXTENSION	= JAR_EXTENSION;
	public static final String	POM_EXTENSION		= "pom";
	public static final String	ZIP_EXTENSION		= "zip";

	public final Revision		revision;
	public final String			classifier;
	public final String			extension;
	public final String			localPath;
	public final String			remotePath;
	public final MavenVersion	snapshot;

	public Archive(String s) {
		Archive v = valueOf(s);
		this.revision = v.revision;
		this.extension = v.extension;
		this.classifier = v.classifier;
		this.snapshot = v.snapshot;
		this.localPath = v.localPath;
		this.remotePath = v.remotePath;
	}

	public Archive(Revision revision, MavenVersion snapshot, String extension, String classifier) {
		this.revision = revision;
		this.extension = extension == null || extension.isEmpty() ? "jar" : extension;
		this.classifier = classifier == null || classifier.isEmpty() ? "" : classifier;
		this.snapshot = snapshot;
		this.localPath = revision.path + "/" + getName();
		this.remotePath = revision.path + "/" + getName(snapshot);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + classifier.hashCode();
		result = prime * result + extension.hashCode();
		result = prime * result + revision.hashCode();
		result = prime * result + (snapshot == null ? 0 : snapshot.hashCode());
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

		Archive other = (Archive) obj;
		if (!classifier.equals(other.classifier))
			return false;

		if (!extension.equals(other.extension))
			return false;

		if (!revision.equals(other.revision))
			return false;

		if (snapshot == null) {
			if (other.snapshot != null)
				return false;
		} else if (!snapshot.equals(other.snapshot))
			return false;

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
			+ (this.classifier.isEmpty() ? "" : "-" + this.classifier) + "." + this.extension;
	}

	@Override
	public String toString() {
		StringBuilder sb = prefix();
		sb.append(":")
			.append(revision.version);
		return sb.toString();
	}

	public String getWithoutVersion() {
		return prefix().toString();
	}

	private StringBuilder prefix() {
		StringBuilder sb = new StringBuilder();
		sb.append(revision.program.group);
		sb.append(":");
		sb.append(revision.program.artifact);

		if (!extension.isEmpty() && !extension.equals("jar")) {
			sb.append(":")
				.append(extension);
			if (!classifier.isEmpty())
				sb.append(":")
					.append(classifier);
		} else {
			if (!classifier.isEmpty()) {
				sb.append(":jar:")
					.append(classifier);
			}
		}
		return sb;
	}

	public boolean isResolved() {
		return !isSnapshot() || snapshot != null;
	}

	public Archive resolveSnapshot(MavenVersion version) {
		if (version.equals(this.snapshot))
			return this;

		return new Archive(revision, version, extension, classifier);
	}

	public boolean isPom() {
		return Archive.POM_EXTENSION.equals(extension);
	}

	public Archive getPomArchive() {
		return revision.getPomArchive();
	}

	public static boolean isValid(String archive) {
		return ARCHIVE_P.matcher(archive)
			.matches();
	}

	public static Archive valueOf(String archive) {
		Matcher m = ARCHIVE_P.matcher(archive);
		if (!m.matches())
			return null;

		Program p = Program.valueOf(m.group("program"));
		return p.version(m.group("version"))
			.archive(m.group("extension"), m.group("classifier"));
	}

	@Override
	public int compareTo(Archive o) {
		int n = revision.compareTo(o.revision);
		if (n != 0)
			return n;

		n = extension.compareTo(o.extension);
		if (n != 0)
			return n;

		return classifier.compareTo(o.classifier);
	}

	private final static Pattern FILEPATH_P = Pattern
		.compile("(?<group>([^/]+)(/[^/]+)+)/(?<artifact>[^/]+)/(?<version>[^/]+)/(?<name>[^/]+)");

	public static Archive fromFilepath(String filePath) {
		Matcher matcher = FILEPATH_P.matcher(filePath);
		if (!matcher.matches())
			return null;

		String group = matcher.group("group")
			.replace('/', '.');
		String artifact = matcher.group("artifact");
		Program program = Program.valueOf(group, artifact);

		String version = matcher.group("version");
		Revision revision = program.version(version);

		String name = matcher.group("name");

		String prefix = artifact + "-" + version;
		if (!name.startsWith(prefix))
			return null;

		int n = name.lastIndexOf(".");
		if (n < prefix.length())
			return null;

		String extension = name.substring(n + 1);

		String classifier = null;
		if (prefix.length() < n)
			classifier = name.substring(prefix.length() + 1, n);

		return revision.archive(extension, classifier);
	}

	public static Archive valueOf(String group, String artifact, String version, String extension, String classifier) {
		return Program.valueOf(group, artifact)
			.version(version)
			.archive(extension, classifier);
	}

	public boolean hasClassifier() {
		return classifier != null && !classifier.isEmpty();
	}

	public boolean hasExtension() {
		return extension != null && !extension.isEmpty() && !extension.equals("jar");
	}

	public Archive getOther(String extension, String classifier) {
		return getRevision().archive(extension, classifier);
	}

	public Archive update(MavenVersion version) {
		return new Archive(new Revision(revision.program, version), version, extension, classifier);
	}
}
