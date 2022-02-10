package aQute.bnd.unmodifiable;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

final class ImmutableMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable {
	final static ImmutableMap<?, ?>	EMPTY	= new ImmutableMap<>();
	final Object[]					entries;
	final transient short[]			hash_bucket;

	ImmutableMap(Object... entries) {
		this.entries = entries;
		this.hash_bucket = hash(entries);
	}

	private static short[] hash(Object[] entries) {
		if ((entries.length & 1) != 0) {
			throw new IllegalArgumentException("entries is not even length");
		}
		int length = entries.length >>> 1;
		if (length == 0) {
			return new short[1];
		}
		if (length >= (1 << Short.SIZE)) {
			throw new IllegalArgumentException("map too large: " + length);
		}
		short[] hash_bucket = new short[length * 2];
		for (int slot = 0, index = 0; slot < length;) {
			Object key = entries[index++];
			int hash = -1 - linear_probe(entries, hash_bucket, key);
			if (hash < 0) {
				throw new IllegalArgumentException("duplicate key: " + key);
			}
			hash_bucket[hash] = (short) ++slot;
			requireNonNull(entries[index++]);
		}
		return hash_bucket;
	}

	// https://en.wikipedia.org/wiki/Linear_probing
	private static int linear_probe(Object[] entries, short[] hash_bucket, Object key) {
		int length = hash_bucket.length;
		for (int hash = (key.hashCode() & 0x7FFF_FFFF) % length;; hash = (hash + 1) % length) {
			int index = (Short.toUnsignedInt(hash_bucket[hash]) - 1) << 1;
			if (index < 0) { // empty
				return -1 - hash;
			}
			if (entries[index].equals(key)) { // found
				return index;
			}
		}
	}

	private int linear_probe(Object key) {
		return linear_probe(entries, hash_bucket, key);
	}

	@Override
	public int size() {
		return entries.length >>> 1;
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
			for (int i = 1; i < entries.length; i += 2) {
				if (value.equals(entries[i])) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		if (key != null) {
			int index = linear_probe(key);
			if (index >= 0) {
				return (V) entries[index + 1];
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
		if (size() != other.size()) {
			return false;
		}
		try {
			for (int index = 0, length = entries.length; index < length; index += 2) {
				if (!entries[index + 1].equals(other.get(entries[index]))) {
					return false;
				}
			}
		} catch (ClassCastException checked) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (int index = 0, length = entries.length; index < length; index += 2) {
			hashCode += entries[index].hashCode() ^ entries[index + 1].hashCode();
		}
		return hashCode;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<V> values() {
		return new ValueCollection();
	}

	abstract class ElementCollection<E> extends AbstractCollection<E> implements Collection<E> {
		abstract E element(int index);

		@Override
		abstract public boolean contains(Object o);

		final class ElementIterator implements Iterator<E> {
			private int			index;
			private final int	end;

			ElementIterator(int index, int end) {
				this.index = index;
				this.end = end;
			}

			@Override
			public boolean hasNext() {
				return index < end;
			}

			@Override
			public E next() {
				if (hasNext()) {
					E element = element(index);
					index += 2;
					return element;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public Iterator<E> iterator() {
			return new ElementIterator(0, entries.length);
		}

		final class ElementSpliterator implements Spliterator<E> {
			private int			index;
			private final int	end;

			ElementSpliterator(int index, int end) {
				this.index = index;
				this.end = end;
			}

			@Override
			public boolean tryAdvance(Consumer<? super E> action) {
				requireNonNull(action);
				if (index < end) {
					E element = element(index);
					index += 2;
					action.accept(element);
					return true;
				}
				return false;
			}

			@Override
			public void forEachRemaining(Consumer<? super E> action) {
				requireNonNull(action);
				while (index < end) {
					E element = element(index);
					index += 2;
					action.accept(element);
				}
			}

			@Override
			public Spliterator<E> trySplit() {
				int split = ((index + end) >>> 2) << 1;
				if (index < split) {
					ElementSpliterator spliterator = new ElementSpliterator(index, split);
					index = split;
					return spliterator;
				}
				return null; // no split
			}

			@Override
			public long estimateSize() {
				return getExactSizeIfKnown();
			}

			@Override
			public int characteristics() {
				int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL
					| Spliterator.SIZED | Spliterator.SUBSIZED;
				if (ElementCollection.this instanceof Set) {
					characteristics |= Spliterator.DISTINCT;
				}
				return characteristics;
			}

			@Override
			public long getExactSizeIfKnown() {
				return (end - index) >>> 1;
			}
		}

		@Override
		public Spliterator<E> spliterator() {
			return new ElementSpliterator(0, entries.length);
		}

		@Override
		public void forEach(Consumer<? super E> action) {
			requireNonNull(action);
			for (int index = 0, end = entries.length; index < end;) {
				E element = element(index);
				index += 2;
				action.accept(element);
			}
		}

		@Override
		public int size() {
			return ImmutableMap.this.size();
		}
	}

	abstract class ElementSet<E> extends ElementCollection<E> implements Set<E> {
		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof Set)) {
				return false;
			}
			Set<?> other = (Set<?>) o;
			if (size() != other.size()) {
				return false;
			}
			try {
				return containsAll(other);
			} catch (ClassCastException checked) {
				return false;
			}
		}

		@Override
		abstract public int hashCode();
	}

	final class EntrySet extends ElementSet<Entry<K, V>> {
		@Override
		@SuppressWarnings("unchecked")
		Entry<K, V> element(int index) {
			return new ImmutableEntry<>((K) entries[index], (V) entries[index + 1]);
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry<?, ?> other = (Entry<?, ?>) o;
			V v = ImmutableMap.this.get(other.getKey());
			if (v == null) {
				return false;
			}
			return v.equals(other.getValue());
		}

		@Override
		public int hashCode() {
			return ImmutableMap.this.hashCode();
		}
	}

	final class KeySet extends ElementSet<K> {
		@Override
		@SuppressWarnings("unchecked")
		K element(int index) {
			return (K) entries[index];
		}

		@Override
		public boolean contains(Object o) {
			return ImmutableMap.this.containsKey(o);
		}

		@Override
		public int hashCode() {
			int hashCode = 0;
			for (int i = 0; i < entries.length; i += 2) {
				hashCode += entries[i].hashCode();
			}
			return hashCode;
		}
	}

	final class ValueCollection extends ElementCollection<V> {
		@Override
		@SuppressWarnings("unchecked")
		V element(int index) {
			return (V) entries[index + 1];
		}

		@Override
		public boolean contains(Object o) {
			return ImmutableMap.this.containsValue(o);
		}
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
			data = map.entries;
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
			if ((length & 1) != 0) {
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
				return new ImmutableMap<>(local);
			} catch (RuntimeException e) {
				InvalidObjectException ioe = new InvalidObjectException("invalid");
				ioe.initCause(e);
				throw ioe;
			}
		}
	}
}
