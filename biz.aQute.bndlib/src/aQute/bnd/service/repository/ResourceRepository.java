package aQute.bnd.service.repository;

import java.io.File;
import java.util.List;
import java.util.SortedSet;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.VersionRange;

/**
 * A Resource Repository represents a repository local to the workspace. A
 * Workspace will always create one Workspace Repository. References to the
 * contents are stored in a text file in {@code ./cnf/dependencies.json}.
 * Associated with the repository is a cache (which might be shared with other
 * subsystems).
 * <p>
 * This repository can be used to get plugin dependencies.
 */
public interface ResourceRepository {
	String FILENAME = "repo.json";

	enum TYPE {
		ADD,
		REMOVE,
		START_DOWNLOAD,
		END_DOWNLOAD,
		ERROR
	}

	class ResourceRepositoryEvent {
		public ResourceRepositoryEvent(TYPE type, ResourceDescriptor rds, Exception exception) {
			this.type = type;
			this.descriptor = rds;
			this.exception = exception;
		}

		public TYPE					type;
		public ResourceDescriptor	descriptor;
		public Exception			exception;
	}

	interface Listener {
		void events(ResourceRepositoryEvent... event) throws Exception;
	}

	/**
	 * Get the list of Resource Descriptors. This contains all the descriptors
	 * that are n the file, regardless of cache.
	 *
	 * @param filter An OSGi filter matched against the
	 *            {@link ResourceDescriptor}
	 * @return an immutable list of resource descriptors
	 */
	List<? extends ResourceDescriptor> filter(String repository, String filter) throws Exception;

	File getResource(byte[] id, RepositoryPlugin.DownloadListener... listeners) throws Exception;

	ResourceDescriptor getResourceDescriptor(byte[] sha) throws Exception;

	boolean delete(String repoId, byte[] rd) throws Exception;

	boolean add(String repoId, ResourceDescriptor rd) throws Exception;

	void addListener(Listener rrl);

	boolean deleteCache(byte[] id) throws Exception;

	SortedSet<ResourceDescriptor> find(String repository, String bsn, VersionRange range) throws Exception;

	File getCacheDir(String name);
}
