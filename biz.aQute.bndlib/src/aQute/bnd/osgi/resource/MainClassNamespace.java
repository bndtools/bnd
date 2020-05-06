package aQute.bnd.osgi.resource;

import org.osgi.framework.Constants;
import org.osgi.framework.VersionRange;

import aQute.bnd.osgi.Domain;

/**
 * Represents the Manifest Main-Class header.
 */
public class MainClassNamespace {

	/**
	 * The attribute must be the fully qualified name of the class that acts as
	 * main class
	 */
	public static final String	MAINCLASS_NAMESPACE	= "bnd.mainclass";

	/**
	 * The version of this bundle as set by Bundle-Version, not set if absent
	 */
	public static final String	VERSION_ATTRIBUTE	= "version";

	public static void build(CapabilityBuilder mc, Domain manifest) {

		mc.addAttribute(MainClassNamespace.MAINCLASS_NAMESPACE, manifest.get("Main-Class"));
		String version = manifest.get(Constants.BUNDLE_VERSION);
		if (version != null)
			mc.addAttribute(VERSION_ATTRIBUTE, version);
	}

	public static String filter(String mainClass, VersionRange range) {
		StringBuilder sb = new StringBuilder();
		sb.append('(')
			.append(MAINCLASS_NAMESPACE)
			.append('=')
			.append(mainClass)
			.append(')');

		if (range != null) {
			sb.insert(0, "(&")
				.append(range.toFilterString(VERSION_ATTRIBUTE))
				.append(')');
		}
		return sb.toString();
	}

}
