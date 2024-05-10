package aQute.bnd.service;

import static aQute.bnd.service.Tagged.RepoTags.resolve;
import static java.util.stream.Collectors.toCollection;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Allows to add tags to implementing classes. Originally intended for tagging
 * repositories.
 */
public interface Tagged {


	enum RepoTags {
		/**
		 * tag for repos which should be used for Resolving bundles. This is
		 * also the default tag for all repos which not have specified tags
		 * (also for bc reasons) Also see {@link Tagged#DEFAULT_REPO_TAGS}
		 */
		resolve
		// add more if neded e.g. relase, baseline
	}

	/**
	 * Each repo has by default the tag {@link RepoTags#resolve} if not tags are
	 * set at the repo definition in build.bnd That means it is consider
	 */
	Set<String> DEFAULT_REPO_TAGS = Set.of(resolve.name());

	/**
	 * @return a non-null list of tags.
	 */
	Set<String> getTags();

	static Set<String> toTags(String csvTags, Set<String> defaultTags) {
		if (csvTags == null || csvTags.isBlank()) {
			return defaultTags; // default
		}

		return Arrays.stream(csvTags.split(","))
			.map(String::trim)
			.collect(toCollection(LinkedHashSet::new));
	}

	static <T> boolean matchesTags(T obj, String... tags) {
		if (obj instanceof Tagged tagged) {
			Set<String> taggedTags = tagged.getTags();
			for (String tag : tags) {
				if (taggedTags.contains(tag)) {
					return true;
				}
			}
		}
		return false;
	}

}
