package aQute.bnd.service.tags;

import static java.util.Collections.unmodifiableSortedSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import aQute.libg.glob.Glob;

/**
 * A set of tags. A tag is a string-token which can be attached to an entity for
 * categorization and filtering. Typically these entities then implement the
 * {@link Tagged} interface.
 */
public class Tags implements Set<String> {
	private final SortedSet<String>	internalSet;

	public final static Tags	NO_TAGS				= of();

	private Tags() {
		this.internalSet = unmodifiableSortedSet(new TreeSet<>());
	}

	private Tags(Collection<? extends String> c) {
		this.internalSet = unmodifiableSortedSet(new TreeSet<>(c));
	}

	@Override
	public int size() {
		return internalSet.size();
	}

	@Override
	public boolean isEmpty() {
		return internalSet.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return internalSet.contains(o);
	}

	@Override
	public Iterator<String> iterator() {
		return internalSet.iterator();
	}

	@Override
	public Object[] toArray() {
		return internalSet.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return internalSet.toArray(a);
	}

	@Override
	public boolean add(String s) {
		return internalSet.add(s);
	}

	@Override
	public boolean remove(Object o) {
		return internalSet.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return internalSet.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		return internalSet.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return internalSet.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return internalSet.removeAll(c);
	}

	@Override
	public void clear() {
		internalSet.clear();
	}

	@Override
	public boolean equals(Object o) {
		return internalSet.equals(o);
	}

	@Override
	public int hashCode() {
		return internalSet.hashCode();
	}

	@Override
	public String toString() {
		return internalSet.toString();
	}



	/**
	 * Returns {@code true} if this entity should take part in <i>any</i> of the
	 * caller-requested tags.
	 *
	 * // @formatter:off
	 * Semantics – in precedence order
	 * --------------------------------
	 * 1. **Legacy wildcard** – if this entity has no tags at all, it matches
	 *    every tag (backwards compatibility).
	 * 2. **Negative tag overrides** – if the entity contains a literal tag
	 *    {@code "no" + tag} it is <b>excluded</b> from that phase, regardless
	 *    of all other rules (example 'nocompile'.
	 * 3. **Positive tag match** – if the entity contains a tag that matches a
	 *    requested glob, it is included.
	 * // @formatter:on
	 *
	 * @param tags (globs)
	 * @return <code>true</code> if any of the given tags is included in the
	 *         current set of tags, otherwise returns <code>false</code>. Also
	 *         if the current set of tags is empty, also <code>true</code> is
	 *         returned.
	 */
	public boolean includesAny(String... tags) {

		if (isEmpty()) {
			// this is on purpose to maintain backwards compatibility for
			// entities which do not handle tags yet and return an empty set. In
			// other words: if the current set is
			// empty that means "yes I match any of what you passed".
			return true;
		}


		for (String tagGlob : tags) {

			if (matchesAny(new Glob(tagGlob))) {
				return true; // success
			}
		}

		return false;

	}


	private boolean matchesAny(Glob glob) {
		return internalSet.stream()
			.anyMatch(s -> glob.matches(s));
	}


	/**
	 * @param name
	 * @return a Tags instance with the given tags.
	 */
	public static Tags of(String... name) {
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
			.collect(Collectors.toCollection(LinkedHashSet::new)));
	}

	/**
	 * Helper printing a csv-string of the tags for display purposes. The
	 * {@link Tagged#EMPTY_TAGS} is treated specially.
	 *
	 * @return a comma separated string of tags. If tags contains just 1 tag
	 *         which is {@link Tagged#EMPTY_TAGS} then "-" is returned. If there
	 *         are more than 1 tags, then {@link Tagged#EMPTY_TAGS} is omitted.
	 */
	public static String print(Tags tags) {
		if (tags.internalSet.size() == 1 && tags.internalSet.contains(Tagged.EMPTY_TAGS)) {
			return "-";
		}

		return tags.internalSet.stream()
			.filter(tag -> !Tagged.EMPTY_TAGS.equals(tag))
			.collect(Collectors.joining(","));
	}


}
