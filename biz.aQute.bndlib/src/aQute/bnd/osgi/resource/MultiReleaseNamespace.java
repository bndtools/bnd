package aQute.bnd.osgi.resource;

/**
 * Multi release jars (MRJ) have different requirements based on the VM they
 * run.
 * <p>
 * This class defines constants related to the osgi.multirelease namespace used
 * for multi-release JAR files in OSGi. These jars contain content that varies
 * depending on the JVM release it is deployed. This could be modeled with a
 * supporting resource. The primary resource requires a multi-release
 * capability. For each range of VMs as used in the MRJ, a supporting resource
 * is created that provides the special capability and requires the proper VM
 * range. The only way to resolve this constellation is to pick one of the
 * synthetic resources that provides the capability and can resolve all its
 * requirements.
 */
public class MultiReleaseNamespace {

	/**
	 * The namespace name
	 */
	public static final String	MULTI_RELEASE_NAMESPACE			= "osgi.multirelease";
	/**
	 * The version attribute.
	 */
	public static final String	CAPABILITY_VERSION_ATTRIBUTE	= "version";

}
