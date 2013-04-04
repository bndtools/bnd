package aQute.bnd.service.repository;

import java.net.*;
import java.util.*;

import org.osgi.resource.*;

import aQute.bnd.version.*;

/**
 * A Searchable Repository is backed by a search engine that holds more
 * revisions than that are currently available locally. For example, it is
 * backed by a database. This interface provides a query interface for text
 * search as well as a requirement based search.
 */
public interface SearchableRepository {
	/**
	 * Describes a resource that is a member of the underlying remote
	 * repository.
	 */
	class ResourceDescriptor {
		/**
		 * Unique id for the resource, for example the sha.
		 */
		public byte[]	id;

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
	}

	/**
	 * Convert a URL to a set of resource descriptors. If the url is not
	 * recognized null is returned. This method can be used if a URL is dropped
	 * and you need to know the resources identified by this url. The returned
	 * set is owned by the caller and may be modified.
	 * 
	 * @param url
	 *            the dropped url
	 * @return null or the modifiable set of associated resource descriptors.
	 */
	Set<ResourceDescriptor> getResources(URI url) throws Exception;

	/**
	 * Search a repository and return a set of resource descriptors that match
	 * the query. The query string may use any syntax. If the syntax is not
	 * recognized or no results are returned an empty set should be returned.
	 * The returned set is owned by the caller and may be modified. Returned
	 * items are not automatically added to the repository.
	 * 
	 * @param query
	 *            The query syntax
	 * @return a set of resource descriptors.
	 * @throws Exception
	 */
	Set<ResourceDescriptor> query(String query) throws Exception;

	/**
	 * Add a set of resource descriptors to the underlying repositories. Only
	 * descriptors recognized to be from this repository are added, others must
	 * be ignored. The returned set contains unrecognized descriptors. The
	 * descriptors passed must be the same (identity) objects returned from the
	 * {@link #getResources(URI)} or {@link #query(String)} methods so that
	 * these methods can return subclasses for easy identification.
	 * 
	 * @param resources
	 *            the set of descriptors to add, potentially empty
	 * @return the set of descriptors not added
	 * @throws Exception
	 */
	Set<ResourceDescriptor> addResources(Set<ResourceDescriptor> resources) throws Exception;

	/**
	 * Find a set of resources that match the given requirement.This is intended
	 * to be used to provide extra resources when a resolve fails. Returned are
	 * all revisions that have a matching capability.
	 * 
	 * @param requirement
	 *            The requirement to match
	 * @return the set of resource descriptors that match, potentially empty
	 * @throws Exception
	 */
	Set<ResourceDescriptor> findResources(Requirement requirement) throws Exception;
}
