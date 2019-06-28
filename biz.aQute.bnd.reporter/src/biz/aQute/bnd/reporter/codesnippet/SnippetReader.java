package biz.aQute.bnd.reporter.codesnippet;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base class for a snippet reader.
 * <p>
 * Snippet readers do the job of extracting snippet from files. They are
 * specific to a programming language.
 */
abstract class SnippetReader {

	private final List<String>			_extensions;
	private Function<String, String>	_idGenerator;

	SnippetReader(final String... extensions) {
		Objects.requireNonNull(extensions, "extensions");

		_extensions = Arrays.asList(extensions);
	}

	final protected void init(final Function<String, String> idGenerator) {
		_idGenerator = idGenerator;
	}

	final public List<String> getSupportedExtension() {
		return _extensions;
	}

	/**
	 * Generate a unique Id given the base Id in argument.
	 * <p>
	 * Should be used to generated Ids of snippets.
	 *
	 * @param baseId the base Id
	 * @return the new Id to be used.
	 */
	final protected String generateId(final String baseId) {
		return _idGenerator.apply(baseId);
	}

	/**
	 * Read a file and extract snippets from it.
	 *
	 * @param file the file to analyze
	 * @return an ordered list of snippets
	 * @throws Exception if any error occures
	 */
	abstract public List<Snippet> read(final File file) throws Exception;
}
