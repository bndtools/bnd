package aQute.bnd.build;

import java.util.Locale;

import aQute.lib.strings.Strings;

/**
 * Matches the Eclipse platform coordinates (os/ws/arch as used in Eclipse
 * feature.xml plugin and includes elements) against the running platform.
 * Values follow the Eclipse conventions, e.g. os=win32|linux|macosx,
 * ws=win32|gtk|cocoa, arch=x86_64|aarch64.
 */
class EclipsePlatform {
	static final EclipsePlatform	CURRENT	= new EclipsePlatform(
		toOs(System.getProperty("os.name", "")), toArch(System.getProperty("os.arch", "")));

	private final String			os;
	private final String			ws;
	private final String			arch;

	EclipsePlatform(String os, String arch) {
		this.os = os;
		this.ws = toWs(os);
		this.arch = arch;
	}

	/**
	 * Match this platform against the given filter values. Each filter value
	 * is a comma separated list of accepted values as used in feature.xml; a
	 * null or empty filter matches any platform.
	 */
	boolean matches(String osFilter, String wsFilter, String archFilter) {
		return matches(osFilter, os) && matches(wsFilter, ws) && matches(archFilter, arch);
	}

	private static boolean matches(String filter, String value) {
		if (filter == null || filter.isBlank())
			return true;
		return Strings.splitAsStream(filter)
			.anyMatch(value::equals);
	}

	static String toOs(String osName) {
		String name = osName.toLowerCase(Locale.ROOT);
		if (name.contains("win"))
			return "win32";
		if (name.contains("mac") || name.contains("darwin"))
			return "macosx";
		if (name.contains("linux"))
			return "linux";
		if (name.contains("sunos") || name.contains("solaris"))
			return "solaris";
		if (name.contains("aix"))
			return "aix";
		if (name.contains("hp-ux"))
			return "hpux";
		if (name.contains("qnx"))
			return "qnx";
		return name;
	}

	static String toWs(String os) {
		return switch (os) {
			case "win32" -> "win32";
			case "macosx" -> "cocoa";
			default -> "gtk";
		};
	}

	static String toArch(String osArch) {
		String arch = osArch.toLowerCase(Locale.ROOT);
		return switch (arch) {
			case "amd64", "x86_64", "x86-64", "em64t" -> "x86_64";
			case "aarch64", "arm64" -> "aarch64";
			case "x86", "i386", "i486", "i586", "i686", "pentium" -> "x86";
			case "ppc64le" -> "ppc64le";
			default -> arch;
		};
	}
}
