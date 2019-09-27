package aQute.bnd.service.resolve.hook;

import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public interface ResolverHook {

	/**
	 * Filter matches hook method. This method is called during the resolve
	 * process for the specified requirement. The collection of candidates match
	 * the specified requirement. This method can filter the collection of
	 * matching candidates by removing candidates from the collection. Removing
	 * a candidate will prevent the resolve process from choosing the removed
	 * candidate to satisfy the requirement. Implementing classes must be
	 * registered as plugins in bnd.
	 *
	 * @param requirement the requirement to filter candidates for
	 * @param candidates a collection of candidates that match the requirement
	 */
	void filterMatches(Requirement requirement, List<Capability> candidates);

}
