package aQute.lib.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterator;

/**
 * An immutable list that sorts objects by their natural order or through a
 * comparator. It has convenient methods/constructors to create it from
 * collections and iterators. Why not maintain the lists in their sorted form?
 * Well, TreeMaps are quite expensive ... I once profiled bnd and was shocked
 * how much memory the Jar class took due to the TreeMaps. I could not easily
 * change it unfortunately. The other reason is that Parameters uses a
 * LinkedHashMap because the preferred order should be the declaration order.
 * However, sometimes you need to sort the keys by name. Last, and most
 * important reason, is that sometimes you do not know what collection you have
 * or it is not available in a sort ordering (MultiMap for example) ... I found
 * myself sorting these things over and over again and decided to just make an
 * immutable SortedList that is easy to slice and dice
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class SortedList<T> implements SortedSet<T>, List<T> {
	private static final SortedList<?>	EMPTY	= new SortedList<>();

	private final T[]					list;
	private final int					start;
	private final int					end;
	private final Comparator<? super T>	comparator;
	private Class<?>					type;

	private class It implements ListIterator<T> {
		private int n;

		It(int n) {
			this.n = n;
		}

		@Override
		public boolean hasNext() {
			return n < end;
		}

		@Override
		public T next() throws NoSuchElementException {
			if (!hasNext()) {
				throw new NoSuchElementException("");
			}
			return list[n++];
		}

		@Override
		public boolean hasPrevious() {
			return n > start;
		}

		@Override
		public T previous() {
			assert n > start;
			return list[--n];
		}

		@Override
		public int nextIndex() {
			return (n - start);
		}

		@Override
		public int previousIndex() {
			return (n - 1) - start;
		}

		@Override
		@Deprecated
		public void remove() {
			throw new UnsupportedOperationException("Immutable");
		}

		@Override
		@Deprecated
		public void set(T e) {
			throw new UnsupportedOperationException("Immutable");
		}

		@Override
		@Deprecated
		public void add(T e) {
			throw new UnsupportedOperationException("Immutable");
		}
	}

	public SortedList(Collection<? extends Comparable<? super T>> x) {
		this((Collection<? extends T>) x, 0, x.size(), null);
	}

	public SortedList(Collection<? extends T> x, Comparator<? super T> cmp) {
		this(x, 0, x.size(), cmp);
	}

	@SafeVarargs
	public <C extends Comparable<? super T>> SortedList(C... x) {
		this((T[]) x, 0, x.length, null);
	}

	@SafeVarargs
	public SortedList(Comparator<? super T> cmp, T... x) {
		this(x, 0, x.length, cmp);
	}

	private SortedList(SortedList<T> other, int start, int end) {
		this.list = other.list;
		this.comparator = other.comparator;
		this.start = start;
		this.end = end;
	}

	public SortedList(T[] x, int start, int end, Comparator<? super T> cmp) {
		if (start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}
		if (start < 0 || start >= x.length)
			throw new IllegalArgumentException("Start is not in list");

		if (end < 0 || end > x.length)
			throw new IllegalArgumentException("End is not in list");

		this.list = (T[]) Arrays.copyOf(x, x.length, Object[].class);
		Arrays.sort(this.list, start, end, cmp);
		this.start = start;
		this.end = end;
		this.comparator = cmp;
	}

	public SortedList(Collection<? extends T> x, int start, int end, Comparator<? super T> cmp) {
		if (start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}
		if (start < 0 || start > x.size())
			throw new IllegalArgumentException("Start is not in list");

		if (end < 0 || end > x.size())
			throw new IllegalArgumentException("End is not in list");

		this.list = (T[]) x.toArray();
		Arrays.sort(this.list, start, end, cmp);
		this.start = start;
		this.end = end;
		this.comparator = cmp;
	}

	private SortedList() {
		list = null;
		start = 0;
		end = 0;
		comparator = null;
	}

	@Override
	public int size() {
		return end - start;
	}

	@Override
	public boolean isEmpty() {
		return start == end;
	}

	@Override
	public boolean contains(Object o) {
		assert type == null || type.isInstance(o);
		for (int i = start; i < end; i++) {
			if (compare((T) o, list[i]) == 0)
				return true;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return new It(start);
	}

	@Override
	public Object[] toArray() {
		if (list == null) {
			return new Object[0];
		}

		if (start == 0 && end == list.length)
			return list.clone();

		if (type != null)
			return toArray((Object[]) Array.newInstance(type, 0));

		return toArray(new Object[0]);
	}

	@Override
	public <X> X[] toArray(X[] a) {
		int size = size();

		if (a.length < size)
			a = (X[]) Array.newInstance(a.getClass()
				.getComponentType(), size);

		System.arraycopy(list, start, a, 0, size);
		if (a.length > size)
			a[size] = null;
		return a;
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c.isEmpty())
			return true;

		if (isEmpty())
			return false;

		// TODO take advantage of sorted nature for this

		for (Object el : c) {
			if (!contains(el))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public Comparator<? super T> comparator() {
		return comparator;
	}

	private int compare(T o1, T o2) {
		if (comparator == null) {
			return ((Comparable<? super T>) o1).compareTo(o2);
		}
		return comparator.compare(o1, o2);
	}

	public boolean isSubSet() {
		return start > 0 && end < list.length;
	}

	@Override
	public SortedList<T> subSet(T fromElement, T toElement) {
		int start = find(fromElement);
		int end = find(toElement);
		if (isSubSet() && (start < 0 || end < 0))
			throw new IllegalArgumentException("This list is a subset");
		if (start < 0)
			start = 0;
		if (end < 0)
			end = list.length;

		return subList(start, end);
	}

	@Override
	public int indexOf(Object o) {
		assert type == null || type.isInstance(o);

		int n = find(o);
		if (n < end && compare((T) o, list[n]) == 0)
			return n;

		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		assert type == null || type.isInstance(o);

		int n = find(o);
		if (n >= end || compare((T) o, list[n]) != 0)
			return -1;

		while (n < end - 1 && compare((T) o, list[n + 1]) == 0)
			n++;

		return n;
	}

	/**
	 * Find the first element that is equal or bigger than the given element
	 *
	 * @param toElement
	 * @return absolute index (not relative!), returns end if not found
	 */
	private int find(Object toElement) {
		int i = start;
		for (; i < end; i++) {
			if (compare((T) toElement, list[i]) <= 0)
				break;
		}

		return i;
	}

	@Override
	public SortedSet<T> tailSet(T fromElement) {
		int i = find(fromElement);
		return subList(i - start, end - start);
	}

	@Override
	public SortedList<T> headSet(T toElement) {
		int i = find(toElement);
		return subList(start, i - start);
	}

	@Override
	public T first() {
		if (isEmpty())
			throw new NoSuchElementException("first");
		return get(0);
	}

	@Override
	public T last() {
		if (isEmpty())
			throw new NoSuchElementException("last");
		return get(size() - 1);
	}

	@Override
	@Deprecated
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public T get(int index) {
		index += start;
		if (index >= end)
			throw new ArrayIndexOutOfBoundsException();

		return list[index];
	}

	@Override
	@Deprecated
	public T set(int index, T element) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	@Deprecated
	public void add(int index, T element) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	@Deprecated
	public T remove(int index) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Override
	public ListIterator<T> listIterator() {
		return new It(start);
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return new It(index + start);
	}

	@Override
	public SortedList<T> subList(int fromIndex, int toIndex) {
		fromIndex += start;
		toIndex += start;

		if (toIndex < fromIndex) {
			int tmp = toIndex;
			toIndex = fromIndex;
			fromIndex = tmp;
		}

		toIndex = Math.max(0, toIndex);
		toIndex = Math.min(toIndex, end);
		fromIndex = Math.max(0, fromIndex);
		fromIndex = Math.min(fromIndex, end);
		if (fromIndex == start && toIndex == end)
			return this;

		if (toIndex == fromIndex)
			return (SortedList<T>) EMPTY;

		return new SortedList<>(this, fromIndex, toIndex);
	}

	@Override
	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isEqual(SortedList<T> list) {
		if (size() != list.size())
			return false;

		for (int as = start, bs = list.start, al = size(); as < al && bs < al; as++, bs++) {
			if (compare(this.list[as], this.list[bs]) != 0)
				return false;
		}
		return true;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String del = "";
		for (T t : this) {
			sb.append(del);
			sb.append(t);
			del = ", ";
		}

		sb.append("]");
		return sb.toString();
	}

	public boolean hasDuplicates() {
		if (list.length < 2)
			return false;

		T prev = list[0];
		for (int i = 1; i < list.length; i++) {
			if (compare(prev, list[i]) == 0)
				return true;

			prev = list[i];
		}
		return false;
	}

	public static <T extends Comparable<? super T>> SortedList<T> fromIterator(Iterator<? extends T> it) {
		IteratorList<T> l = new IteratorList<>(it);
		return new SortedList<>(l);
	}

	public static <T> SortedList<T> fromIterator(Iterator<? extends T> it, Comparator<? super T> cmp) {
		IteratorList<T> l = new IteratorList<>(it);
		return new SortedList<>(l, cmp);
	}

	public static <T> SortedSet<T> empty() {
		return (SortedSet<T>) EMPTY;
	}

	@Override
	public Spliterator<T> spliterator() {
		return SortedSet.super.spliterator();
	}
}
