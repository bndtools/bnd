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
	 * @return All plugins that have a tag that matches at least one of the
     *         given tags or have no tags.
	 */
	default <T> List<T> getPlugins(Class<T> c, String... tags) {

		if (tags.length == 0) {
			return getPlugins(c);
		}

		return getPlugins(c).stream()
			.filter(plugin -> (plugin instanceof Tagged taggedPlugin) ? taggedPlugin.getTags()
				.includesAny(tags) : true)
			.collect(Collectors.toList());
	}

	<T> T getPlugin(Class<T> c);
}
