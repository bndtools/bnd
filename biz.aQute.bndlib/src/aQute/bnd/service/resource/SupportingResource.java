package aQute.bnd.service.resource;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

/**
 * The SupportingResource interface represents a resource that requires other
 * resources that are needed to resolve the primary resource. It was introduced
 * to support Multi Release Jars (MRJ). These jars contain content that varies
 * depending on the JVM release it is deployed. This could be modeled with a
 * supporting resource. The primary resource required a special capability. For
 * each range of VMs as used in the MRJ, a supporting resource is created that
 * provides the special capability and requires the proper VM range.
 * <p>
 * However, this could be useful in other cases so the current specification is
 * independent of this use case.
 * <p>
 * This interface extends Resource, this is the primary.
 */
public interface SupportingResource extends Resource {

	/**
	 * Gets all of the supporting resources for this resource, including the
	 * primary.
	 *
	 * @return A list of all of the supporting resources for this resource.
	 */
	default List<Resource> all() {
		List<Resource> result = new ArrayList<>();
		result.add(this);
		result.addAll(getSupportingResources());
		return result;
	}

	/**
	 * Gets the supporting resources for this resource, not including itself.
	 *
	 * @return A list of the supporting resources for this resource.
	 */
	List<Resource> getSupportingResources();

	/**
	 * Checks whether this resource has an identity. This is determined by
	 * whether it has any capabilities in the {@link IdentityNamespace}.
	 *
	 * @return {@code true} if this resource has an identity, {@code false}
	 *         otherwise.
	 */
	default boolean hasIdentity() {
		return !getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).isEmpty();
	}

}
