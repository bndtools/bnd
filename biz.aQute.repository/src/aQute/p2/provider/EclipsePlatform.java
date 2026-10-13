package aQute.p2.provider;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.lib.strings.Strings;

/**
 * The Eclipse platform (os/ws/arch as used in feature.xml and
 * Eclipse-PlatformFilter) a run targets. It is taken from the osgi.os, osgi.ws
 * and osgi.arch run properties; a coordinate that is not set defaults to the
 * host, and the value {@code *} matches anything.
 */
public final class EclipsePlatform {

	/**
	 * Namespace Equinox uses for the requirement made from the
	 * Eclipse-PlatformFilter manifest header.
	 */
	public static final String			NAMESPACE			= "eclipse.platform";
	public static final String			FILTER_HEADER		= "Eclipse-PlatformFilter";

	static final String					PROPERTY_OS			= "osgi.os";
	static final String					PROPERTY_WS			= "osgi.ws";
	static final String					PROPERTY_ARCH		= "osgi.arch";
	static final String					ANY					= "*";

	private static final List<String>	KNOWN_OS			= List.of("win32", "linux", "macosx", "solaris", "aix",
		"hpux", "qnx");
	private static final List<String>	KNOWN_WS			= List.of("win32", "gtk", "cocoa", "motif");
	private static final List<String>	KNOWN_ARCH			= List.of("x86", "x86_64", "aarch64", "arm", "ppc", "ppc64",
		"ppc64le", "riscv64", "loongarch64", "sparc", "s390x");

	private static final Set<String>	COORDINATES			= Set.of(PROPERTY_OS, PROPERTY_WS, PROPERTY_ARCH);
	private static final Pattern		FILTER_ATTRIBUTE	= Pattern
		.compile("\\(\\s*([^()=<>~\\s&|!]+)\\s*(?:=|<=|>=|~=)");

	private static final String			HOST_OS				= toOs(System.getProperty("os.name", ""));
	private static final String			HOST_ARCH			= toArch(System.getProperty("os.arch", ""));

	private final String				os;
	private final String				ws;
	private final String				arch;

	private EclipsePlatform(String os, String ws, String arch) {
		this.os = os;
		this.ws = ws;
		this.arch = arch;
	}

	/**
	 * The platform described by run properties; {@code ws} is derived from the
	 * os when not set.
	 */
	public static EclipsePlatform of(Map<String, String> properties) {
		String os = property(properties, PROPERTY_OS, HOST_OS);
		String ws = property(properties, PROPERTY_WS, null);
		String arch = property(properties, PROPERTY_ARCH, HOST_ARCH);
		if (ws == null)
			ws = ANY.equals(os) ? ANY : toWs(os);
		return new EclipsePlatform(os, ws, arch);
	}

	/**
	 * A requirement made from the {@link #FILTER_HEADER} value of a bundle, or
	 * null when the value is absent, invalid or constrains more than os, ws and
	 * arch, which cannot be evaluated here.
	 */
	public static CapReqBuilder requirement(String platformFilter) {
		if (platformFilter == null || platformFilter.isBlank())
			return null;

		String filter;
		try {
			filter = FrameworkUtil.createFilter(platformFilter)
				.toString();
		} catch (InvalidSyntaxException e) {
			return null;
		}

		boolean evaluable = false;
		Matcher matcher = FILTER_ATTRIBUTE.matcher(filter);
		while (matcher.find()) {
			if (!COORDINATES.contains(matcher.group(1)))
				return null;
			evaluable = true;
		}
		if (!evaluable)
			return null;

		return new CapReqBuilder(NAMESPACE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
	}

	/**
	 * Whether a requirement applies to this platform. Only feature members (an
	 * identity requirement marked {@link Feature#RELATION_PLUGIN} or
	 * {@link Feature#RELATION_INCLUDE}) carry platform coordinates; any other
	 * requirement always applies.
	 */
	public boolean matches(Requirement requirement) {
		if (!IdentityNamespace.IDENTITY_NAMESPACE.equals(requirement.getNamespace()))
			return true;

		Map<String, Object> attributes = requirement.getAttributes();
		Object relation = attributes.get(Feature.RELATION_ATTRIBUTE);
		if (!Feature.RELATION_PLUGIN.equals(relation) && !Feature.RELATION_INCLUDE.equals(relation))
			return true;

		return matches(string(attributes.get("os")), string(attributes.get("ws")), string(attributes.get("arch")));
	}

	/**
	 * The capability a resolve provides for this platform, matched by
	 * {@link #NAMESPACE} requirements.
	 */
	public Capability capability() {
		return new CapabilityBuilder(NAMESPACE).addAttribute(PROPERTY_OS, values(os, KNOWN_OS))
			.addAttribute(PROPERTY_WS, values(ws, KNOWN_WS))
			.addAttribute(PROPERTY_ARCH, values(arch, KNOWN_ARCH))
			.buildSyntheticCapability();
	}

	/**
	 * Each filter value is a comma separated list of accepted values as used
	 * in feature.xml; a null or empty value accepts any platform.
	 */
	boolean matches(String osFilter, String wsFilter, String archFilter) {
		return matches(osFilter, os) && matches(wsFilter, ws) && matches(archFilter, arch);
	}

	private static boolean matches(String filter, String value) {
		if (ANY.equals(value) || filter == null || filter.isBlank())
			return true;
		return Strings.splitAsStream(filter)
			.anyMatch(value::equals);
	}

	private static Object values(String value, List<String> known) {
		return ANY.equals(value) ? known : value;
	}

	private static String property(Map<String, String> properties, String key, String deflt) {
		String value = properties == null ? null : properties.get(key);
		if (value == null || value.isBlank())
			return deflt;
		return value.trim();
	}

	private static String string(Object value) {
		if (value instanceof Collection<?> values)
			return values.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		return value == null ? null : value.toString();
	}

	static String toOs(String osName) {
		String name = osName.toLowerCase(Locale.ROOT);
		// before win: "darwin" contains it
		if (name.contains("mac") || name.contains("darwin"))
			return "macosx";
		if (name.contains("win"))
			return "win32";
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

	@Override
	public String toString() {
		return PROPERTY_OS + "=" + os + ", " + PROPERTY_WS + "=" + ws + ", " + PROPERTY_ARCH + "=" + arch;
	}
}
