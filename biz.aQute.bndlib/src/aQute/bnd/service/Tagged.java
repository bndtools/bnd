package aQute.bnd.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows to add tags to implementing classes. Originally intended for tagging
 * repositories.
 */
public interface Tagged {

	/**
	 * @return a non-null list of tags.
	 */
	Set<String> getTags();

	static Set<String> toTags(String csvTags) {
		if (csvTags == null || csvTags.isBlank()) {
			return Set.of("all"); // default
		}

		return Arrays.stream(csvTags.split(","))
			.map(String::trim)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

}
