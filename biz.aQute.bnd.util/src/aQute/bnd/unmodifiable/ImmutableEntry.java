package aQute.bnd.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

final class ImmutableEntry<K, V> extends SimpleImmutableEntry<K, V> implements Entry<K, V> {
	private static final long serialVersionUID = 1L;

	ImmutableEntry(K key, V value) {
		super(requireNonNull(key), requireNonNull(value));
	}
}
