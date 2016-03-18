package aQute.maven.repo.api;

import java.util.Map;
import java.util.WeakHashMap;

import aQute.bnd.version.MavenVersion;
import aQute.lib.strings.Strings;

public class Program {
	public final String	group;
	public final String	artifact;
	public final String	path;

	final private static Map<String, Program>	programCache	= new WeakHashMap<>();
	private Map<MavenVersion, Revision>			revisionCache	= new WeakHashMap<>();

	Program(String group, String artifact) {
		this.group = group;
		this.artifact = artifact;
		this.path = group + "/" + artifact;
	}

	public synchronized Revision version(String version) {
		MavenVersion v = new MavenVersion(version);
		return version(v);
	}

	public Revision version(MavenVersion v) {
		Revision r = revisionCache.get(v);
		if (r == null) {
			r = new Revision(this, v);
			revisionCache.put(v, r);
		}
		return r;
	}

	static String validate(String gav) {
		String parts[] = gav.split(":");
		return validate(parts);
	}

	static String validate(String parts[]) {
		if (parts.length != 1)
			return "A GAV must consists of at least of <g>:<a>:<v>";

		if (!isValidName(parts[0]))
			return "Invalid group " + parts[0];

		if (!isValidName(parts[1]))
			return "Invalid artifact " + parts[1];

		return null;
	}

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

	public StringBuilder toStringBuilder(StringBuilder sbb) {
		StringBuilder sb = sbb == null ? new StringBuilder() : sbb;
		sb.append(group);
		sb.append(":").append(artifact);
		return sb;
	}

	public String toString() {
		return toStringBuilder(null).toString();
	}

	public static Program valueOf(String group, String artifact) {
		synchronized (programCache) {
			String key = group + ":" + artifact;
			Program p = programCache.get(key);
			if (p == null) {
				p = new Program(group, artifact);
				programCache.put(key, p);
			}
			return p;
		}
	}

	public String metadata() {
		return path + "/maven-metadata.xml";
	}

	public String metadata(String id) {
		return path + "/maven-metadata-" + id + ".xml";
	}

	public String getCoordinate() {
		return group + ":" + artifact;
	}

	public static Program valueOf(String bsn) {
		String parts[] = Strings.trim(bsn).split(":");
		if (parts.length != 2)
			return null;

		return valueOf(parts[0], parts[1]);
	}
}
