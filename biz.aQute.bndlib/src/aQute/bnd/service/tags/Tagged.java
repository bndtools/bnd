package aQute.bnd.service.tags;

/**
 * Allows to add tags to implementing classes. Originally intended for tagging
 * repositories.
 */
public interface Tagged {
	
	/**
	 * Dummy placeholder for "empty tags".
	 */
	String EMPTY_TAGS = "<<EMPTY>>"; 

	/**
	 * @return a non-null list of tags. Default is empty (meaning 'no tags').
	 */
	default Tags getTags() {
		return Tags.NO_TAGS;
	}

}
