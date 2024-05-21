package aQute.bnd.service;

import java.util.List;
import java.util.stream.Collectors;

import aQute.bnd.service.tags.Tagged;

/**
 * A registry for objects.
 */
public interface Registry {
	<T> List<T> getPlugins(Class<T> c);

	default <T> List<T> getPlugins(Class<T> c, String... tags) {

		if (tags.length == 0) {
			return getPlugins(c);
		}

		return getPlugins(c).stream()
			.filter(repo -> repo instanceof Tagged taggedRepo && taggedRepo.getTags()
				.isIncluded(tags))
			.collect(Collectors.toList());
	}

	<T> T getPlugin(Class<T> c);
}
