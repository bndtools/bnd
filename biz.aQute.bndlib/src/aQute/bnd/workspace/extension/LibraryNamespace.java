package aQute.bnd.workspace.extension;

import org.osgi.framework.Constants;

import aQute.bnd.version.VersionRange;

/**
 * A workspace extension can be applied on the workspace level
 *
 * <pre>
 * bnd.workspace.extension      name of the workspace extension
 * version                      the version of the extension
 * where                      	declares the root of the library in the bundle
 * </pre>
 */
public interface LibraryNamespace {

	String	NAMESPACE						= "bnd.library";

	/**
	 * The name of the external plugin
	 */
	String	CAPABILITY_NAME_ATTRIBUTE		= NAMESPACE;

	/**
	 * The version of the extension
	 */
	String	CAPABILITY_VERSION_ATTRIBUTE	= Constants.VERSION_ATTRIBUTE;

	/**
	 * An extension needs to declare a bndfile to include when activated.
	 */
	String	CAPABILITY_PATH_ATTRIBUTE		= "path";

	static String filter(String name, String versionRange) {
		StringBuilder sb = new StringBuilder();
		if (versionRange != null)
			sb.append("(&");
		sb.append("(")
			.append(CAPABILITY_NAME_ATTRIBUTE)
			.append("=")
			.append(name)
			.append(")");
		if (versionRange != null)
			sb.append(new VersionRange(versionRange).toFilter())
				.append(")");
		return sb.toString();
	}

}
