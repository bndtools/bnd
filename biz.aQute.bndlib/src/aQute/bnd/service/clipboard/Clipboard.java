package aQute.bnd.service.clipboard;

import java.util.Optional;

/**
 * Abstraction of the clip board since this can vary per driver
 */
public interface Clipboard {
	/**
	 * Copy the content and return true if successful.
	 *
	 * @param <T> the type of the content
	 * @param content the content
	 * @return true if succesfully copied to the clipboard
	 */
	<T> boolean copy(T content);

	/**
	 * Get the content from the clipboard
	 *
	 * @param <T>
	 * @param type the type to fetch
	 * @return a instance or empty if no such type was on the clipboard
	 */
	<T> Optional<T> paste(Class<T> type);
}
