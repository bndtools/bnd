package aQute.bnd.service.merge;

import java.util.Optional;

import aQute.bnd.osgi.Resource;

/**
 * For plugins knowing how to merge two resources.
 */
public interface MergeResources {
	/**
	 * @param path a path (used for validation if the path segment is supported
	 *            for merge)
	 * @param a first resource
	 * @param b second resource to be merged with a
	 * @return the merged resource if possible or an empty optional if merging
	 *         was not possible.
	 */
	Optional<Resource> tryMerge(String path, Resource a, Resource b);
}
