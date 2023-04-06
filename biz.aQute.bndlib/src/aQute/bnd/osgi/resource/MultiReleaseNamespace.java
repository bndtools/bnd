package aQute.bnd.osgi.resource;

/**
 * Multi release jars (MRJ) have different requirements based on the VM they
 * run.
 * <p>
 * This class defines constants related to the multi-release capabilities used
 * for multi-release JAR files in OSGi. These jars contain content that varies
 * depending on the JVM release it is deployed. This could be modelled with a
 * supporting resource. For each range of VMs in the MRJ, a supporting resource
 * is created that has a marker attribute in the identity capability and
 * requires the proper VM range. The only way to resolve this constellation is
 * to pick one of the synthetic resources that provides the capability and can
 * resolve all its requirements.
 */
public class MultiReleaseNamespace {

	/**
	 * The namespace name
	 */
	public static final String	MULTI_RELEASE_NAMESPACE			= "bnd.multirelease";
	/**
	 * The version attribute.
	 */
	public static final String	CAPABILITY_VERSION_ATTRIBUTE	= "version";

	/**
	 * The marker fragment attribute added to the content capability url
	 * Deprecated for removal before releasing bnd 7.0
	 */
	@Deprecated
	public static final String	MULTI_RELEASE_VERSION_ATTRIBUTE	= "mr-version";

	/**
	 * A system property for temporary use in enabling one multi-release
	 * indexing format vs another. Deprecated for removal before releasing bnd
	 * 7.0
	 */
	@Deprecated
	public static final String	MULTI_RELEASE_INDEXING				= "bnd.multirelease.indexing";

	/**
	 * A bnd instruction for temporary use in enabling one multi-release
	 * indexing format vs another. Deprecated for removal before releasing bnd
	 * 7.0
	 */
	@Deprecated
	public static final String	MULTI_RELEASE_INDEXING_INSTRUCTION	= "-xxxmultireleaseindexing";

	/**
	 * Multiple resource indexing. Each resource is an osgi.bundle Deprecated
	 * for removal before releasing bnd 7.0
	 */
	@Deprecated
	public static final String	MULTI_RELEASE_INDEXING_MULTIPLE		= "multiple";

	/**
	 * Synthetic resource indexing. There is one main bundle and additional
	 * synthetic resources of type bnd.synthetic Deprecated for removal before
	 * releasing bnd 7.0
	 */
	@Deprecated
	public static final String	MULTI_RELEASE_INDEXING_SYNTHETIC	= "synthetic";

}
