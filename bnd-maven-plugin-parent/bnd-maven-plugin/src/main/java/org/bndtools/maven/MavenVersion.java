package org.bndtools.maven;

/**
 * Represents a Maven version with the ability to translate to a bnd version,
 * with SNAPSHOT being replaced by bnd's ${tstamp} macro.
 * 
 * @author Neil Bartlett <neil.bartlett@paremus.com>
 */
public class MavenVersion {

	private static final String SNAPSHOT = "SNAPSHOT";
	private static final String TSTAMP = "${tstamp}";

	private int major;
	private int minor;
	private int micro;
	private String qualifier;
	
	public MavenVersion(String versionString) throws IllegalArgumentException {
		if (versionString == null) {
			major = minor = micro = 0;
			qualifier = null;
		} else {
			String main;
			int dashIndex = versionString.indexOf('-');
			if (dashIndex > -1) {
				main = versionString.substring(0, dashIndex);
				qualifier = versionString.substring(dashIndex+1);
			} else {
				main = versionString;
				qualifier = null;
			}

			String[] segments = main.split("\\.", 3);
			major = segments.length > 0 ? Integer.parseInt(segments[0]) : 0;
			minor = segments.length > 1 ? Integer.parseInt(segments[1]) : 0;
			micro = segments.length > 2 ? Integer.parseInt(segments[2]) : 0;
		}
		
		if (qualifier != null) {
			int i = qualifier.indexOf(SNAPSHOT);
			if (i >= 0) {
				qualifier = new StringBuilder()
					.append(qualifier.substring(0, i))
					.append(TSTAMP)
					.append(qualifier.substring(i + SNAPSHOT.length()))
					.toString();
			}
		}
	}

	/**
	 * Convert the Maven version to a bnd version. This is an OSGi-compatible version
	 * string except that it may contain macros such as <code>${tstamp}</code>.
	 */
	public String toBndVersion() {
		String s = String.format("%d.%d.%d", major, minor, micro);
		if (qualifier != null && qualifier.length() > 0)
			s += "." + qualifier;
		return s;
	}
	
}
