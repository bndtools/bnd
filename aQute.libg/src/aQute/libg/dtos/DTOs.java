package aQute.libg.dtos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;

/**
 * This interface provides a number of utilities to make it easy to work with
 * DTOs. It contains a number of utility functions.
 */
@ProviderType
public interface DTOs {

	DTOs INSTANCE = new DTOsImpl();

	/**
	 * Return a partially read only Map object that maps directly to a DTO. I.e.
	 * changes are reflected in the DTO. If a field is a DTO, then this field
	 * will also become a Map.
	 *
	 * @param dto the DTO
	 * @return a Map where the keys map to the field names and the values to the
	 *         field values. This map is not modifiable.
	 */
	Map<String, Object> asMap(Object dto);

	/**
	 * Convert a DTO to a human readable string presentation. This is primarily
	 * for debugging since the toString can truncate fields. This method must
	 * print all public fields, also non primary. Output formats can vary (e.g.
	 * YAML like) so the actual output should NOT be treated as standard.
	 *
	 * @param dto the dto to turn into a string
	 * @return a human readable string (not json!)
	 */
	String toString(Object dto);

	/**
	 * Check if two dtos fields are equal. This is shallow equal, that is the
	 * fields of this DTO are using the equals() instance method.
	 *
	 * @param a the first object
	 * @param b the second object
	 * @return true if both are null or the DTO's primary fields are equal
	 */
	boolean equals(Object a, Object b);

	/**
	 * Check if two DTOs fields are equal. This is deep equal, that is the
	 * fields of this DTO are using this method is the object at a field is a
	 * DTO, recursively.
	 *
	 * @param a the first object
	 * @param b the second object
	 * @return true if both are null or the DTO's primary fields are equal
	 */
	boolean deepEquals(Object a, Object b);

	/**
	 * Calculate a hash Code for the fields in this DTO. The dto must have at
	 * least one public field.
	 *
	 * @param dto the object to calculate the hashcode for, must not be null .
	 * @return a hashcode
	 */
	int hashCode(Object dto);

	/**
	 * Access a DTO with a path. A path is a '.' separated string. Each part in
	 * the path is either a field name, key in a map, or an index in a list. If
	 * the path segments contain dots or backslashes, then these must be escaped
	 *
	 * @param dto the root
	 * @param path the path, should only contain dots as separators
	 * @return the value of the object or empty if not found.
	 */

	Optional<Object> get(Object dto, String path);

	/**
	 * Access a DTO with a path that consists of an array with segments. Each
	 * segment in the path is either a field name, key in a map, or an index in
	 * a list.
	 *
	 * @param dto the root
	 * @param path the path
	 * @return the value of the object or empty if not found.
	 */
	Optional<Object> get(Object dto, String... path);

	/**
	 * Return a list of paths where the two objects differ. The objects must be
	 * of the same class.
	 *
	 * @param older the older object
	 * @param newer the newer object
	 * @return A list of differences, if there is no difference, the list is
	 *         empty.
	 */
	List<Difference> diff(Object older, Object newer);

	/**
	 * The details of a difference
	 */
	class Difference extends DTO {
		/**
		 * The path where there was a difference
		 */
		public String	path[];

		/**
		 * The reason why there was a difference
		 */
		public Reason	reason;
	}

	/**
	 * The reason for a difference.
	 */
	enum Reason {
		UNEQUAL,
		REMOVED,
		ADDED,
		DIFFERENT_TYPES,
		SIZE,
		KEYS,
		NO_STRING_MAP,
		INVALID_KEY;
	}

	/**
	 * Takes a path with escaped '.'and '\' and then turns it into an array of
	 * unescaped keys
	 *
	 * @param path the path with escaped \ and .
	 * @return a path array with unescaped segments
	 */
	String[] fromPathToSegments(String path);

	/**
	 * Takes a path with unescaped keys and turns it into a string path where
	 * the \ and . are escaped.
	 *
	 * @param segments The unescaped segments of the path
	 * @return a string path where the . and \ are escaped.
	 */
	String fromSegmentsToPath(String[] segments);

	/**
	 * Escape a string to be used in a path. This will put a backslash ('\') in
	 * front of full stops ('.') and the backslash ('\').
	 *
	 * @param unescaped the string to be escaped
	 * @return a string where all '.' and '\' are escaped with a '\'.
	 */
	String escape(String unescaped);

	/**
	 * Unescapes a string to be used in a path. This will remove a backslash
	 * ('\') in front of full stops ('.') and the backslash ('\').
	 *
	 * @param escaped the string to be unescaped
	 * @return a string where all '\.' and '\\' have the preceding backslash
	 *         removed with a '\'.
	 */
	String unescape(String escaped);

	/**
	 * Return true if the give dto is complex (either Map, Collection, Array, or
	 * has public fields.
	 *
	 * @param object The DTO to check
	 * @return <code>true</code> if this is a DTO with fields or length.
	 */

	boolean isComplex(Object object);

	/**
	 * An object with public non-static non-synthetic fields.
	 *
	 * @param dto the object to check
	 * @return true if this object has public fields or extends DTO
	 */
	boolean isDTO(Object dto);

	/**
	 * Create a shallow copy of a DTO. This will create a new object of the same
	 * type and copy the public fields of the source to the new copy. It will
	 * not create a copy for these values.
	 *
	 * @param object the source object
	 * @return a shallow copy of object
	 */

	<T> T shallowCopy(T object);

	/**
	 * Create a deep copy of a DTO. This will copy the fields of the DTO. Copied
	 * values will also be created anew if they are complex (Map, Collection,
	 * DTO, or Array). Other objects are assumed to be immutable unless they
	 * implement Cloneable.
	 *
	 * @param object the object to deep copy
	 * @return the deep copied object
	 */

	<T> T deepCopy(T object);
}
