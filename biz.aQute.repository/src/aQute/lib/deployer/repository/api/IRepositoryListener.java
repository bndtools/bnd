package aQute.lib.deployer.repository.api;

import java.net.URI;

import org.osgi.resource.Resource;

public interface IRepositoryListener {

	/**
	 * Process an OBR resource descriptor from the index document, and possibly
	 * request early termination of the parser.
	 * 
	 * @param resource
	 *            The resource to be processed.
	 * @param baseUri
	 *            The base URI of the repository. Where Resources contain
	 *            relative content URIs, those URIs are to be resolved relative
	 *            to this base URI.
	 */
	void processResource(Resource resource, URI baseUri);

	/**
	 * Process an OBR referral
	 * 
	 * @param parentUri
	 *            The URI of the Repository that referred to this Referral
	 * @param referral
	 *            The referral to be processed
	 * @param maxDepth
	 *            The depth of referrals this repository acknowledges.
	 * @param currentDepth
	 *            The current depth
	 */
	void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth);

}
