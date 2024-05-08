package aQute.bnd.service;

import java.util.Arrays;
import java.util.List;

/**
 * Allows to add tags to implementing classes. Originally intended for tagging
 * repositories.
 */
public interface Tagged {

	/**
	 * @return a non-null list of tags.
	 */
	List<String> getTags();

	static List<String> toTags(String csvTags) {
		if (csvTags == null || csvTags.isBlank()) {
			return List.of("all"); // default
		}

		return Arrays.stream(csvTags.split(","))
			.map(String::trim)
			.toList();
	}

}
