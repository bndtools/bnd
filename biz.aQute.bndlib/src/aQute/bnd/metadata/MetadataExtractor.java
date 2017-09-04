package aQute.bnd.metadata;

import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.osgi.Jar;

abstract class MetadataExtractor extends Extractor {

	abstract public void extract(BundleMetadataDTO dto, Jar jar);

	/**
	 * Verifies that the dto is conform to its specification.
	 * <p>
	 * This method removes all {@code null} values from collections, turns
	 * {@code null} collections to empty collections and checks if the all required
	 * fields are correctly set.
	 * </p>
	 * 
	 * @param dto the dto to verify
	 * @throws Exception if the dto has a missing field
	 */
	abstract public void verify(BundleMetadataDTO dto) throws Exception;

	@Override
	protected void error(String error) throws Exception {

		throw new Exception("Bundle description error: " + error);
	}
}
