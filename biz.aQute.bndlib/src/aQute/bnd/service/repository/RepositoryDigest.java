package aQute.bnd.service.repository;

public interface RepositoryDigest {
	/**
	 * Return a SHA-1 for the collection. This sha is calculated as follows: 1)
	 * If there is a single file, return its sha 2) Otherwise, create a set
	 * (skipping duplicates) of all shas, sort them, and then create the sha of
	 * this file.
	 */

	byte[] getDigest();
}
