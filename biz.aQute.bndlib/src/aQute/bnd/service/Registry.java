package aQute.bnd.service;

import java.util.List;
import java.util.stream.Collectors;

import aQute.bnd.service.tags.Tagged;

/**
 * A registry for objects.
 */
public interface Registry {

	/**
	 * @param <T>
	 * @param c
	 * @return all plugins matching the given class
	 */
	<T> List<T> getPlugins(Class<T> c);

	/**
	 * @param <T>
	 * @param c
	 * @param tags
	 * @return all plugins matching the given class and any of the given tags.
	 *         If no tags are given, all plugins are returned without filtering.
	 */
	default <T> List<T> getPlugins(Class<T> c, String... tags) {

		if (tags.length == 0) {
			return getPlugins(c);
		}

		return getPlugins(c).stream()
			.filter(repo -> repo instanceof Tagged taggedRepo && taggedRepo.getTags()
				.includesAny(tags))
			.collect(Collectors.toList());
	}

	<T> T getPlugin(Class<T> c);
}
