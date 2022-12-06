package aQute.bnd.unmodifiable;

import java.util.Map.Entry;

final class ImmutableEntry<K, V> implements Entry<K, V> {
	private final K	key;
	private final V	value;

	/**
	 * Constructor.
	 *
	 * @param key Must not be {@code null}.
	 * @param value Must not be {@code null}.
	 */
	ImmutableEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return key.hashCode() ^ value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Entry<?, ?> entry)) {
			return false;
		}
		return key.equals(entry.getKey()) && value.equals(entry.getValue());
	}

	@Override
	public String toString() {
		return key + "=" + value;
	}
}
