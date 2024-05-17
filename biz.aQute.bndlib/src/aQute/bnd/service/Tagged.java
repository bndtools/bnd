package aQute.bnd.service;

/**
 * Allows to add tags to implementing classes. Originally intended for tagging
 * repositories.
 */
public interface Tagged {

	/**
	 * @return a non-null list of tags. Default is empty (meaning 'no tags').
	 */
	default Tags getTags() {
		return Tags.NO_TAGS;
	}

	/**
	 * @param <T>
	 * @param obj
	 * @param tags
	 * @return <code>true</code> if the passed object matches any of the given
	 *         tags, otherwise returns <code>false</code>
	 */
	static <T> boolean matchesTags(T obj, String... tags) {
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
