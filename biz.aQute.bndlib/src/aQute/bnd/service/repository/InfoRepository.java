package aQute.bnd.service.repository;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;

/**
 * Can return a {@link ResourceDescriptor} for a given bsn/version.
 */
public interface InfoRepository extends RepositoryPlugin {
	/**
	 * Return a resource descriptor for a given bsn/version.
	 *
	 * @param bsn The exact bsn
	 * @param version The exact version (must also match qualifier)
	 * @return a ResourceDescriptor describing the artifact
	 */
	ResourceDescriptor getDescriptor(String bsn, Version version) throws Exception;

}
