package aQute.bnd.deployer.repository.api;

import java.net.URI;

import org.osgi.resource.Resource;

public interface IRepositoryIndexProcessor {

	/**
	 * Process an OBR resource descriptor from the index document, and possibly
	 * request early termination of the parser.
	 *
	 * @param resource The resource to be processed. The content URI of the
	 *            resource must be a resolved, absolute URI.
	 */
	void processResource(Resource resource);

	/**
	 * Process an OBR referral
	 *
	 * @param parentUri The URI of the Repository that referred to this Referral
	 * @param referral The referral to be processed
	 * @param maxDepth The depth of referrals this repository acknowledges.
	 * @param currentDepth The current depth
	 */
	void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth);

}
