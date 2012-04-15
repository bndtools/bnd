package aQute.lib.deployer.repository.api;

public interface IRepositoryListener {

	/**
	 * Process an OBR resource descriptor from the index document, and possibly
	 * request early termination of the parser.
	 * 
	 * @param resource
	 *            The resource descriptor to be processed.
	 * @return Whether to continue parsing the document; returning {@code false}
	 *         will result in the parser being stopped with a
	 *         {@link StopParseException}.
	 */
	boolean processResource(BaseResource resource);
	
	/**
	 * Process an OBR referral
	 * @param parentUrl The url of the Repository that referred to this Referral
	 * @param referral The referral to be processed
	 * @param maxDepth The depth of referrals this repository acknowledges.
	 * @param currentDepth The current depth
	 * @return Whether to continue parsing the document; returning {@code false}
	 *         will result in the parser being stopped with a
	 *         {@link StopParseException}.
	 */
	boolean processReferral(String parentUrl, Referral referral, int maxDepth, int currentDepth);

}
