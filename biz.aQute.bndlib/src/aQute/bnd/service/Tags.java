package aQute.bnd.service;

import static java.util.Collections.unmodifiableSortedSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A set of tags. A tag is a string-token which can be attached to an entity for
 * categorization and filtering. Typically these entities then implement the
 * {@link Tagged} interface.
 */
public final class Tags implements Set<String> {
	private final SortedSet<String>	internalSet;

	public final static Tags	NO_TAGS				= of();

	private Tags() {
		this.internalSet = unmodifiableSortedSet(new TreeSet<>());
	}

	private Tags(Collection<? extends String> c) {
		this.internalSet = unmodifiableSortedSet(new TreeSet<>(c));
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
			.collect(Collectors.toCollection(LinkedHashSet::new)));
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



}
