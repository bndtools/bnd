package aQute.bnd.deployer.repository.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

import org.osgi.service.log.LogService;

import aQute.bnd.service.Registry;

public interface IRepositoryContentProvider {

	/**
	 * Get the name of this provider (keep it short!).
	 */
	String getName();

	/**
	 * Parse the index provided via the input stream.
	 *
	 * @param stream The stream that provides the index data.
	 * @param baseUri
	 * @param processor
	 */
	void parseIndex(InputStream stream, URI baseUri, IRepositoryIndexProcessor processor, LogService log)
		throws Exception;

	/**
	 * <p>
	 * Check the stream for compatibility with this provider.
	 * </p>
	 * </ul>
	 *
	 * @param name The name of the stream, which may be the file name, or
	 *            {@code null}. Providers can use this as a hint to the nature
	 *            of the file.
	 * @param stream An input stream that MUST support mark/reset operations.
	 * @return Whether the stream can be handled by this provider.
	 * @throws IOException
	 */
	CheckResult checkStream(String name, InputStream stream) throws IOException;

	/**
	 * Return whether the content provider supports index generation.
	 */
	boolean supportsGeneration();

	/**
	 * Generate a new repository index to a stream. Clients must not call this
	 * method if the provider returns {@code false} from
	 * {@link #supportsGeneration()}.
	 *
	 * @param files The files to be indexed.
	 * @param output The output stream, on which the index should be written.
	 * @param repoName The name of the repository, which may be entered into the
	 *            generated index.
	 * @param rootUri The URI of the repository.
	 * @param pretty Hint to request "pretty printing", i.e. uncompressed,
	 *            indented output.
	 * @param registry The bnd workspace plug-in registry if available, or
	 *            {@code null}.
	 * @param log The OSGi log service if available, or {@code
	 * null}.
	 * @throws Exception If any other error unrecoverable occurs.
	 */
	void generateIndex(Set<File> files, OutputStream output, String repoName, URI rootUri, boolean pretty,
		Registry registry, LogService log) throws Exception;

	/**
	 * Get the default name for an index file supported by this provider;
	 * however the actual index file may take a different name if it is
	 * overridden by the user or build settings.
	 *
	 * @param pretty Whether the pretty-printing option will be used. Providers
	 *            may return a different default file name depending on this
	 *            value, e.g. a non-pretty file may be compressed with the
	 *            {@code .gz} extension.
	 */
	String getDefaultIndexName(boolean pretty);

}
