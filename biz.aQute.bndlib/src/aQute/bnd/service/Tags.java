package aQute.bnd.service;

import static java.util.stream.Collectors.toCollection;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set of tags. A tag is a string-token which can be attached to an entity for
 * categorization and filtering. Typically these entities then implement the
 * {@link Tagged} interface.
 */
public final class Tags extends LinkedHashSet<String> {

	private static final long serialVersionUID = 1L;

	public final static Tags	NO_TAGS				= of();

	private Tags(Collection<String> c) {
		super(c);
	}

	static Tags of(String... name) {
		return new Tags(Set.of(name));
	}

	/**
	 * Parses a comma-separated string of tags into a Tags object.
	 *
	 * @param csvTags
	 * @param defaultTags a default used when csvTags is null or blank
	 * @return populated Tags or the passed defaultTags.
	 */
	public static Tags parse(String csvTags, Tags defaultTags) {
		if (csvTags == null || csvTags.isBlank()) {
			return defaultTags; // default
		}

		return new Tags(Arrays.stream(csvTags.split(","))
			.map(String::trim)
			.collect(toCollection(LinkedHashSet::new)));
	}

	/**
	 * @param <T>
	 * @param obj
	 * @param tags
	 * @return <code>true</code> if the passed object matches any of the given
	 *         tags, otherwise returns <code>false</code>
	 */
	public static <T> boolean matchesTags(T obj, String... tags) {
		if (obj instanceof Tagged tagged) {
			Tags taggedTags = tagged.getTags();
			for (String tag : tags) {
				if (taggedTags.contains(tag)) {
					return true;
				}
			}
		}
		return false;
	}

}
