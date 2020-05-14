package aQute.maven.api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import aQute.bnd.version.MavenVersion;
import aQute.lib.strings.Strings;

/**
 * In Maven, the groupId and artifactId represent an anonymous concept. This
 * concept is represented by this class.
 * <p>
 * We do not validate the groupId and artifactId because it is completely
 * unclear what the character set and syntax is :-(
 */
public class Program implements Comparable<Program> {
	public final String							group;
	public final String							artifact;
	public final String							path;

	Program(String group, String artifact) {
		this.group = group;
		this.artifact = artifact;
		this.path = group.replace('.', '/') + "/" + artifact;
	}

	/**
	 * Create a revision by giving it a version
	 *
	 * @param version the version
	 * @return the revision
	 */
	public Revision version(String version) {
		MavenVersion v = new MavenVersion(version);
		return version(v);
	}

	/**
	 * Create a revision by giving it a version
	 *
	 * @param version the version
	 * @return the revision
	 */
	public Revision version(MavenVersion version) {
		return new Revision(this, version);
	}

	static String validate(String gav) {
		String parts[] = split(gav);
		return validate(parts);
	}

	static String validate(String parts[]) {
		if (parts.length != 3)
			return "A GAV must consists of at least of <g>:<a>:<v>";

		if (!isValidName(parts[0]))
			return "Invalid group " + parts[0];

		if (!isValidName(parts[1]))
			return "Invalid artifact " + parts[1];

		return null;
	}

	/**
	 * Validate if this is a valid g:a pair.
	 *
	 * @param string the g:a pair
	 * @return true if valid, false otherwise
	 */
	public static boolean isValidName(String string) {
		if (string == null || string.isEmpty())
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + group.hashCode();
		result = prime * result + artifact.hashCode();
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

		Program other = (Program) obj;
		if (!artifact.equals(other.artifact))
			return false;

		if (!group.equals(other.group))
			return false;

		return true;
	}

	@Override
	public String toString() {
		return group + ":" + artifact;
	}

	/**
	 * Creates a program out of a g:a
	 *
	 * @param group the g
	 * @param artifact the a
	 * @return the Program
	 */
	public static Program valueOf(String group, String artifact) {
		return new Program(group, artifact);
	}

	/**
	 * @return Return the remote path to the program's metadata
	 */
	public String metadata() {
		return path + "/maven-metadata.xml";
	}

	/**
	 * @return Return the local path to the program's metadata. The id is the id
	 *         of the repository
	 */
	public String metadata(String id) {
		return path + "/maven-metadata-" + id + ".xml";
	}

	private static final Pattern GAV_SPLITTER = Pattern.compile(":");

	public static Program valueOf(String bsn) {
		String parts[] = split(Strings.trim(bsn));
		if (parts.length != 2)
			return null;

		return valueOf(parts[0], parts[1]);
	}

	public static String[] split(String line) {
		String parts[] = GAV_SPLITTER.split(line);
		return parts;
	}

	@Override
	public int compareTo(Program o) {
		int n = group.compareTo(o.group);
		if (n != 0)
			return n;

		return artifact.compareTo(o.artifact);
	}

	public Map<String, String> attributes() {
		Map<String, String> attrs = new HashMap<>();
		attrs.put("maven-groupId", group);
		attrs.put("maven-artifactId", artifact);
		return attrs;
	}
}
