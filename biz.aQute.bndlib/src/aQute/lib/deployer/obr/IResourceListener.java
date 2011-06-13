package aQute.lib.deployer.obr;

public interface IResourceListener {
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
	boolean processResource(Resource resource);
}
