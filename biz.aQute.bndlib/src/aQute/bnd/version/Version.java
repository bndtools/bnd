package aQute.bnd.version;

import java.io.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;

public class Version implements Comparable<Version> {
	public final static String	VERSION_STRING				= "(\\d{1,9})(\\.(\\d{1,9})(\\.(\\d{1,9})(\\.([-_\\da-zA-Z]+))?)?)?";
	public final static String	VERSION_STRING_NON_STRICT3	= "(\\d{1,9})(\\.(\\d{1,9})(\\.(\\d{1,9})([\\.-]([-_\\da-zA-Z]+))?)?)?";
	public final static String	VERSION_STRING_NON_STRICT2	= "(\\d{1,9})(\\.(\\d{1,9})([\\.-]([-_\\da-zA-Z]+))?)?";
	public final static String	VERSION_STRING_NON_STRICT1	= "(\\d{1,9})([\\.-]([-_\\da-zA-Z]+))?";
	public final static Pattern	VERSION						= Pattern.compile(VERSION_STRING);
	public final static Version	LOWEST						= new Version();
	public final static Version	ONE							= new Version(1, 0, 0);
	public final static Version	HIGHEST						= new Version(Integer.MAX_VALUE, Integer.MAX_VALUE,
																	Integer.MAX_VALUE, "\uFFFF");	
	public final static Version	emptyVersion				= LOWEST;

	final int					major;
	final int					minor;
	final int					micro;
	final String				qualifier;


	public Version() {
		this(0);
	}

	public Version(int major, int minor, int micro, String qualifier) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.qualifier = qualifier;
	}

	public Version(int major, int minor, int micro) {
		this(major, minor, micro, null);
	}

	public Version(int major, int minor) {
		this(major, minor, 0, null);
	}

	public Version(int major) {
		this(major, 0, 0, null);
	}

	public Version(String version) {
		version = version.trim();
		Matcher m = VERSION.matcher(version);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid syntax for version: " + version);

		major = Integer.parseInt(m.group(1));
		if (m.group(3) != null)
			minor = Integer.parseInt(m.group(3));
		else
			minor = 0;

		if (m.group(5) != null)
			micro = Integer.parseInt(m.group(5));
		else
			micro = 0;

		qualifier = m.group(7);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getMicro() {
		return micro;
	}

	public String getQualifier() {
		return qualifier;
	}

	public int compareTo(Version other) {
		if (other == this)
			return 0;

		Version o = other;
		if (major != o.major)
			return major - o.major;

		if (minor != o.minor)
			return minor - o.minor;

		if (micro != o.micro)
			return micro - o.micro;

		int c = 0;
		if (qualifier != null)
			c = 1;
		if (o.qualifier != null)
			c += 2;

		switch (c) {
			case 0 :
				return 0;
			case 1 :
				return 1;
			case 2 :
				return -1;
		}
		return qualifier.compareTo(o.qualifier);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(major);
		sb.append(".");
		sb.append(minor);
		sb.append(".");
		sb.append(micro);
		if (qualifier != null) {
			sb.append(".");
			sb.append(qualifier);
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object ot) {
		if (!(ot instanceof Version))
			return false;

		return compareTo((Version) ot) == 0;
	}

	@Override
	public int hashCode() {
		return major * 97 ^ minor * 13 ^ micro + (qualifier == null ? 97 : qualifier.hashCode());
	}

	public int get(int i) {
		switch (i) {
			case 0 :
				return major;
			case 1 :
				return minor;
			case 2 :
				return micro;
			default :
				throw new IllegalArgumentException("Version can only get 0 (major), 1 (minor), or 2 (micro)");
		}
	}

	public static Version parseVersion(String version) {
		if (version == null) {
			return LOWEST;
		}

		version = version.trim();
		if (version.length() == 0) {
			return LOWEST;
		}

		return new Version(version);

	}

	public Version getWithoutQualifier() {
		return new Version(major, minor, micro);
	}

	private static Pattern	fuzzyVersion		= Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
														Pattern.DOTALL);
	private static Pattern	fuzzyVersionRange	= Pattern
														.compile(
																"(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))",
																Pattern.DOTALL);
	private static Pattern	fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);

	/**
	 * Clean up a version. Other builders (like Maven) use more fuzzy
	 * definitions of the version syntax. This method cleans up such a version
	 * to match an OSGi version.
	 * 
	 * @param version
	 *            the raw version
	 * @return the cleaned up version
	 */
	static public String cleanupVersion(String version) {
		Matcher m = Verifier.VERSIONRANGE.matcher(version);

		if (m.matches()) {
			try {
				VersionRange vr = new VersionRange(version);
				return version;
			}
			catch (Exception e) {
				// ignore
			}
		}

		m = fuzzyVersionRange.matcher(version);
		if (m.matches()) {
			String prefix = m.group(1);
			String first = m.group(2);
			String last = m.group(3);
			String suffix = m.group(4);
			return prefix + cleanupVersion(first) + "," + cleanupVersion(last) + suffix;
		}

		m = fuzzyVersion.matcher(version);
		if (m.matches()) {
			StringBuilder result = new StringBuilder();
			String major = removeLeadingZeroes(m.group(1));
			String minor = removeLeadingZeroes(m.group(3));
			String micro = removeLeadingZeroes(m.group(5));
			String qualifier = m.group(7);

			if (qualifier == null) {
				if (!isInteger(minor)) {
					qualifier = minor;
					minor = "0";
				} else if (!isInteger(micro)) {
					qualifier = micro;
					micro = "0";
				}
			}
			if (major != null) {
				result.append(major);
				if (minor != null) {
					result.append(".");
					result.append(minor);
					if (micro != null) {
						result.append(".");
						result.append(micro);
						if (qualifier != null) {
							result.append(".");
							cleanupModifier(result, qualifier);
						}
					} else if (qualifier != null) {
						result.append(".0.");
						cleanupModifier(result, qualifier);
					}
				} else if (qualifier != null) {
					result.append(".0.0.");
					cleanupModifier(result, qualifier);
				}
				return result.toString();
			}
		}
		return version;
	}

	/**
	 * The cleanup version got confused when people used numeric dates like
	 * 201209091230120 as qualifiers. These are too large for Integers. This
	 * method checks if the all digit string fits in an integer.
	 * 
	 * <pre>
	 * maxint = 2,147,483,647 = 10 digits
	 * </pre>
	 * 
	 * @param integer
	 * @return if this fits in an integer
	 */
	private static boolean isInteger(String minor) {
		return minor.length() < 10 || (minor.length() == 10 && minor.compareTo("2147483647") < 0);
	}

	private static String removeLeadingZeroes(String group) {
		if (group == null)
			return "0";

		int n = 0;
		while (n < group.length() - 1 && group.charAt(n) == '0')
			n++;
		if (n == 0)
			return group;

		return group.substring(n);
	}

	static void cleanupModifier(StringBuilder result, String modifier) {
		Matcher m = fuzzyModifier.matcher(modifier);
		if (m.matches())
			modifier = m.group(2);

		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
				result.append(c);
		}
	}

	/**
	 * Ordered list of manifest headers that are used by
	 * {@link #fromManifest(Manifest, boolean)}
	 */
	static private final String[] VERSION_HEADERS = {
			Constants.BUNDLE_VERSION /* MUST be first */,
			"Implementation-Version",
			"Specification-Version",
			"Version"
		};

	/**
	 * <p>
	 * Get the version from a manifest
	 * </p>
	 * <p>
	 * Loop over the headers listed in {@link #VERSION_HEADERS}: when the header
	 * is set in the manifest _and_ its value denotes a valid version, then
	 * return that version.
	 * </p>
	 * <p>
	 * For headers other than {@link Constants#BUNDLE_VERSION}: the header value
	 * will be cleaned up by {@link #cleanupVersion(String)} before constructing
	 * a Version.
	 * </p>
	 * <p>
	 * If construction of a Version fails then the header's value doesn't denote
	 * a valid version and the next header is tried: invalid versions are
	 * ignored.
	 * </p>
	 * 
	 * @param manifest
	 *            the manifest
	 * @param strict
	 *            when true then only the {@link Constants#BUNDLE_VERSION}
	 *            header is tried
	 * @return null when manifest is null or when there is no valid version in
	 *         the manifest, the version from the manifest otherwise
	 */
	static public Version fromManifest(Manifest manifest, boolean strict) {
		if (manifest == null) {
			return null;
		}

		Attributes attributes = manifest.getMainAttributes();
		if (!attributes.isEmpty()) {
			for (int index = 0; index < VERSION_HEADERS.length; index++) {
				String headerValue = attributes.getValue(VERSION_HEADERS[index]);
				headerValue = (headerValue == null) ? "" : headerValue.trim();
				if (headerValue.length() > 0) {
					try {
						if (index != 0) {
							headerValue = cleanupVersion(headerValue);
						}
						Version version = new Version(headerValue);
						return version;
					}
					catch (Exception e) {
						/* not a valid version */
					}
				}
				if (strict) {
					/* not a valid bundle version */
					return null;
				}
			}
		}

		/* not a valid version */
		return null;
	}

	/**
	 * The regular expression used by {@link #fromFileName(String, boolean)} for
	 * strict mode matching
	 */
	public final static Pattern	REPO_FILE_STRICT			= Pattern.compile("\\s*(?:([-\\w\\._]+?)\\s*-\\s*)("
																	+ VERSION_STRING + "|"
																	+ Constants.VERSION_ATTR_LATEST + ")\\s*\\.(jar|lib)");

	/**
	 * The regular expression used by {@link #fromFileName(String, boolean)} for
	 * non-strict mode matching
	 */
	public final static Pattern	REPO_FILE_NONSTRICT			= Pattern.compile("\\s*(?:([-a-zA-z0-9_\\.]+?)\\s*-\\s*)("
																	+ VERSION_STRING_NON_STRICT3 + "|"
																	+ VERSION_STRING_NON_STRICT2 + "|"
																	+ VERSION_STRING_NON_STRICT1 + "|"
																	+ Constants.VERSION_ATTR_LATEST + ")\\s*\\.(jar|lib)");

	/**
	 * <p>
	 * Get the version from a a file name
	 * </p>
	 * <p>
	 * In non-strict mode the version that is taken from the file name will be
	 * cleaned up by {@link #cleanupVersion(String)} before constructing a
	 * Version (when it is not {@link Constants#VERSION_ATTR_LATEST}).
	 * </p>
	 * 
	 * @param fileName
	 *            the file name
	 * @param strict
	 *            true to match against {@link #REPO_FILE_STRICT}, false to
	 *            match against {@link #REPO_FILE_NONSTRICT}
	 * @return null when fileName is null, when fileName does not denote a
	 *         '*.jar' file, when fileName contains no version, when fileName
	 *         contains an invalid version, {@link #HIGHEST} when the version in
	 *         fileName is {@link Constants#VERSION_ATTR_LATEST}, the version
	 *         otherwise
	 */
	static public Version fromFileName(String fileName, boolean strict) {
		if (fileName == null) {
			return null;
		}

		String baseName = new File(fileName).getName();

		Matcher m = (strict ? REPO_FILE_STRICT : REPO_FILE_NONSTRICT).matcher(baseName);
		if (!m.matches()) {
			return null;
		}

		/* there is a version in the fileName */

		String fileNameVersion = m.group(2);
		if (fileNameVersion.equals(Constants.VERSION_ATTR_LATEST)) {
			/* the fileName version is 'latest' */
			return HIGHEST;
		}

		if (!strict) {
			fileNameVersion = cleanupVersion(fileNameVersion);
		}

		try {
			return new Version(fileNameVersion);
		}
		catch (Exception e) {
			return null;
		}
	}
}
