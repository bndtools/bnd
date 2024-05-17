package aQute.bnd.service;

import static aQute.bnd.service.Tags.matchesTags;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A registry for objects.
 */
public interface Registry {
	<T> List<T> getPlugins(Class<T> c);

	default <T> List<T> getPlugins(Class<T> c, String... tags) {

		if (tags == null || tags.length == 0) {
			return getPlugins(c);
		}

		return getPlugins(c).stream()
			.filter(repo -> matchesTags(repo, tags))
			.collect(Collectors.toList());
	}

	<T> T getPlugin(Class<T> c);
}
