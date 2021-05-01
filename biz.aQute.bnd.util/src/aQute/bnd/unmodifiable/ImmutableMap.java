package aQute.bnd.unmodifiable;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

final class ImmutableMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable {
	@SuppressWarnings("unchecked")
	final static ImmutableMap<?, ?>	EMPTY	= new ImmutableMap<>();
	final Entry<K, V>[]				entries;
	final transient short[]			hash_bucket;
	transient Set<Entry<K, V>>		entrySet;

	@SafeVarargs
	ImmutableMap(Entry<K, V>... entries) {
		this.entries = entries;
		this.hash_bucket = hash(entries);
	}

	private static <K, V> short[] hash(Entry<K, V>[] entries) {
		int length = entries.length;
		if (length == 0) {
			return new short[1];
		}
		if (length >= (1 << Short.SIZE)) {
			throw new IllegalArgumentException("map too large: " + length);
		}
		short[] hash_bucket = new short[length * 2];
		for (int i = 0; i < length;) {
			int slot = linear_probe(entries, hash_bucket, entries[i].getKey());
			if (slot >= 0) {
				throw new IllegalArgumentException("duplicate key: " + entries[i].getKey());
			}
			hash_bucket[-1 - slot] = (short) ++i;
		}
		return hash_bucket;
	}

	// https://en.wikipedia.org/wiki/Linear_probing
	private static <K, V> int linear_probe(Entry<K, V>[] entries, short[] hash_bucket, Object key) {
		int length = hash_bucket.length;
		for (int hash = (key.hashCode() & 0x7FFF_FFFF) % length;; hash = (hash + 1) % length) {
			int slot = Short.toUnsignedInt(hash_bucket[hash]) - 1;
			if (slot < 0) { // empty
				return -1 - hash;
			}
			if (entries[slot].getKey()
				.equals(key)) { // found
				return slot;
			}
		}
	}

	private int linear_probe(Object key) {
		return linear_probe(entries, hash_bucket, key);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> set = entrySet;
		if (set != null) {
			return set;
		}
		return entrySet = new ImmutableSet<>(entries);
	}

	@Override
	public int size() {
		return entries.length;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key != null) {
			return linear_probe(key) >= 0;
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value != null) {
			for (Entry<K, V> entry : entries) {
				if (value.equals(entry.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		if (key != null) {
			int slot = linear_probe(key);
			if (slot >= 0) {
				return entries[slot].getValue();
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Map)) {
			return false;
		}
		Map<?, ?> other = (Map<?, ?>) o;
		if (entries.length != other.size()) {
			return false;
		}
		try {
			for (Entry<K, V> entry : entries) {
				if (!entry.getValue()
					.equals(other.get(entry.getKey()))) {
					return false;
				}
			}
		} catch (ClassCastException checkedMap) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (Entry<K, V> entry : entries) {
			hashCode += entry.hashCode();
		}
		return hashCode;
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V putIfAbsent(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V replace(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	// Serialization support
	private static final long serialVersionUID = 1L;

	private void readObject(ObjectInputStream ois) throws InvalidObjectException {
		throw new InvalidObjectException("proxy required");
	}

	private Object writeReplace() {
		return new SerializationProxy(this);
	}

	private static final class SerializationProxy implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private transient Object[]	data;

		SerializationProxy(ImmutableMap<?, ?> map) {
			final Entry<?, ?>[] entries = map.entries;
			final int length = entries.length;
			final Object[] local = new Object[length * 2];
			int i = 0;
			for (Entry<?, ?> entry : entries) {
				local[i++] = entry.getKey();
				local[i++] = entry.getValue();
			}
			data = local;
		}

		private void writeObject(ObjectOutputStream oos) throws IOException {
			oos.defaultWriteObject();
			final Object[] local = data;
			final int length = local.length;
			oos.writeInt(length);
			for (int i = 0; i < length; i++) {
				oos.writeObject(local[i]);
			}
		}

		private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
			ois.defaultReadObject();
			final int length = ois.readInt();
			if (length < 0) {
				throw new InvalidObjectException("negative length");
			}
			if ((length % 2) != 0) {
				throw new InvalidObjectException("odd length");
			}
			final Object[] local = new Object[length];
			for (int i = 0; i < length; i++) {
				local[i] = ois.readObject();
			}
			data = local;
		}

		private Object readResolve() throws InvalidObjectException {
			try {
				final Object[] local = data;
				if (local.length == 0) {
					return EMPTY;
				}
				final int length = local.length / 2;
				@SuppressWarnings("unchecked")
				Entry<Object, Object>[] entries = new Entry[length];
				for (int i = 0; i < length; i++) {
					final int j = i * 2;
					entries[i] = new ImmutableEntry<>(local[j], local[j + 1]);
				}
				return new ImmutableMap<>(entries);
			} catch (RuntimeException e) {
				InvalidObjectException ioe = new InvalidObjectException("invalid");
				ioe.initCause(e);
				throw ioe;
			}
		}
	}
}
