package aQute.bnd.service.repository;

import java.net.URI;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Requirement;

import aQute.bnd.version.Version;

/**
 * A Searchable Repository is backed by a search engine that holds more
 * revisions than that are currently available locally. For example, it is
 * backed by a database. This interface provides a query interface for text
 * search as well as a requirement based search.
 */
@ProviderType
public interface SearchableRepository {
	/**
	 * Describes a resource that is a member of the underlying remote
	 * repository.
	 */
	class ResourceDescriptor {
		/**
		 * SHA-1 for the resource.
		 */
		public byte[]	id;

		/**
		 * Also need the sha 256 because of the OSGi Repository stuff
		 */
		public byte[]	sha256;

		/**
		 * A description of the resource.
		 */
		public String	description;

		/**
		 * The name of the resource, usually the Bundle Symbolic Name
		 */
		public String	bsn;

		/**
		 * The version of the resource.
		 */
		public Version	version;

		/**
		 * The phase of the resource
		 */
		public Phase	phase;

		/**
		 * True if already included in the local repository.
		 */
		public boolean	included;

		/**
		 * True if this resource was added as a dependency
		 */
		public boolean	dependency;

		/**
		 * Location of the resource
		 */
		public URI		url;

		/**
		 * An ID of the resource owner.
		 */
		public String	owner;
	}

	/**
	 * Convert a URL to a set of resource descriptors. If the url is not
	 * recognized null is returned. This method can be used if a URL is dropped
	 * and you need to know the resources identified by this url. The returned
	 * set is owned by the caller and may be modified.
	 * <p/>
	 * The @{code includeDependencies} parameter indicates that if possible any
	 * mandatory compile and runtime dependencies should be added to the result
	 * set.
	 *
	 * @param url the dropped url
	 * @param includeDependencies Include any dependent revisions
	 * @return null or the modifiable set of associated resource descriptors.
	 */
	Set<ResourceDescriptor> getResources(URI url, boolean includeDependencies) throws Exception;

	/**
	 * Search a repository and return a set of resource descriptors that match
	 * the query. The query string may use any syntax. If the syntax is not
	 * recognized or no results are returned an empty set should be returned.
	 * The returned set is owned by the caller and may be modified. Returned
	 * items are not automatically added to the repository.
	 *
	 * @param query The query syntax
	 * @return a set of resource descriptors.
	 * @throws Exception
	 */
	Set<ResourceDescriptor> query(String query) throws Exception;

	/**
	 * Add a resource descriptors to the underlying repository. Only descriptors
	 * recognized to be from the designated repository are added, others must be
	 * ignored. True must be returned if this descriptor was accepted.
	 *
	 * @param resource the descriptor to add
	 * @return true if added, false if rejected
	 * @throws Exception
	 */
	boolean addResource(ResourceDescriptor resource) throws Exception;

	/**
	 * Find a set of resources that match the given requirement.This is intended
	 * to be used to provide extra resources when a resolve fails. Returned are
	 * all revisions that have a matching capability.
	 * <p/>
	 * The @{code includeDependencies} parameter indicates that if possible any
	 * mandatory compile and runtime dependencies should be added to the result
	 * set.
	 *
	 * @param requirement The requirement to match
	 * @param includeDependencies Include any dependent revisions
	 * @return the set of resource descriptors that match, potentially empty
	 * @throws Exception
	 */
	Set<ResourceDescriptor> findResources(Requirement requirement, boolean includeDependencies) throws Exception;

	/**
	 * Return the URL to a web page that allows browsing or searching of the
	 * repository.
	 *
	 * @param searchString A search string, or null for general browsing
	 * @return A URL that may be opened in a web browser, or null if the
	 *         repository does not support web browsing.
	 * @throws Exception
	 */
	URI browse(String searchString) throws Exception;

}
