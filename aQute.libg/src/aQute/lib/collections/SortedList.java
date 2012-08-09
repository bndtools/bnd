package aQute.lib.collections;

import java.util.*;

/**
 * An immutbale list that sorts objects by their natural order or through a
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
	static SortedList< ? >		empty		= new SortedList<Object>();

	final T[]					list;
	final int					start;
	final int					end;
	final Comparator<T>			cmp;
	Class< ? >					type;
	static Comparator<Object>	comparator	= //

											new Comparator<Object>() {
												public int compare(Object o1, Object o2) {

													if (o1 == o2)
														return 0;

													if (o1.equals(o2))
														return 0;

													return ((Comparable<Object>) o1).compareTo(o2);
												}
											};

	class It implements ListIterator<T> {
		int	n;

		It(int n) {
			this.n = n;
		}

		public boolean hasNext() {
			return n < end;
		}

		public T next() throws NoSuchElementException {
			if (!hasNext()) {
				throw new NoSuchElementException("");
			}
			return list[n++];
		}

		public boolean hasPrevious() {
			return n > start;
		}

		public T previous() {
			return get(n - 1);
		}

		public int nextIndex() {
			return (n + 1 - start);
		}

		public int previousIndex() {
			return (n - 1) - start;
		}

		@Deprecated
		public void remove() {
			throw new UnsupportedOperationException("Immutable");
		}

		@Deprecated
		public void set(T e) {
			throw new UnsupportedOperationException("Immutable");
		}

		@Deprecated
		public void add(T e) {
			throw new UnsupportedOperationException("Immutable");
		}
	}

	public SortedList(Collection< ? extends Comparable< ? >> x) {
		this((Collection<T>) x, 0, x.size(), (Comparator<T>) comparator);
	}

	public SortedList(Collection<T> x, Comparator<T> cmp) {
		this(x, 0, x.size(), cmp);
	}

	@SuppressWarnings("cast")
	public SortedList(T... x) {
		this((T[]) x.clone(), 0, x.length, (Comparator<T>) comparator);
	}

	@SuppressWarnings("cast")
	public SortedList(Comparator<T> cmp, T... x) {
		this((T[]) x.clone(), 0, x.length, cmp);
	}

	private SortedList(SortedList<T> other, int start, int end) {
		this.list = other.list;
		this.cmp = other.cmp;
		this.start = start;
		this.end = end;
	}

	public SortedList(T[] x, int start, int end, Comparator<T> comparator2) {
		if (start > end) {
			int tmp = start;
			start = end;
			end = tmp;
		}
		if (start < 0 || start >= x.length)
			throw new IllegalArgumentException("Start is not in list");

		if (end < 0 || end > x.length)
			throw new IllegalArgumentException("End is not in list");

		this.list = x.clone();
		Arrays.sort(this.list, start, end, comparator2);
		this.start = start;
		this.end = end;
		this.cmp = comparator2;
	}

	public SortedList(Collection< ? extends T> x, int start, int end, Comparator<T> cmp) {
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
		this.cmp = cmp;
	}

	private SortedList() {
		list = null;
		start = 0;
		end = 0;
		cmp = null;
	}

	public int size() {
		return end - start;
	}

	public boolean isEmpty() {
		return start == end;
	}

	@SuppressWarnings("cast")
	public boolean contains(Object o) {
		assert type != null & type.isInstance(o);
		return indexOf((T) o) >= 0;
	}

	public Iterator<T> iterator() {
		return new It(start);
	}

	public Object[] toArray() {
		return list.clone();
	}

	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] a) {
		if (a == null || a.length < list.length) {
			return (T[]) list.clone();
		}
		System.arraycopy(list, 0, a, 0, list.length);
		return a;
	}

	public boolean add(T e) {
		throw new UnsupportedOperationException("Immutable");
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Immutable");
	}

	public boolean containsAll(Collection< ? > c) {
		if (c.isEmpty())
			return true;

		if (isEmpty())
			return false;

		// TODO take advantage of sorted nature for this

		for (Object el : c) {
			if (!contains(el))
				return false;
		}
		return false;
	}

	public boolean addAll(Collection< ? extends T> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	public boolean retainAll(Collection< ? > c) {
		throw new UnsupportedOperationException("Immutable");
	}

	public boolean removeAll(Collection< ? > c) {
		throw new UnsupportedOperationException("Immutable");
	}

	public void clear() {
		throw new UnsupportedOperationException("Immutable");
	}

	public Comparator< ? super T> comparator() {
		return cmp;
	}

	public boolean isSubSet() {
		return start > 0 && end < list.length;
	}

	public SortedList<T> subSet(T fromElement, T toElement) {
		int start = indexOf(fromElement);
		int end = indexOf(toElement);
		if (isSubSet() && (start < 0 || end < 0))
			throw new IllegalArgumentException("This list is a subset");
		if (start < 0)
			start = 0;
		if (end < 0)
			end = list.length;

		return subList(start, end);
	}

	public int indexOf(Object o) {
		assert type != null && type.isInstance(o);

		int n = Arrays.binarySearch(list, (T) o, cmp);
		if (n >= start && n < end)
			return n - start;

		return -1;
	}

	public SortedList<T> headSet(T toElement) {
		int i = indexOf(toElement);
		if (i < 0) {
			if (isSubSet())
				throw new IllegalArgumentException("This list is a subset");
			i = end;
		}

		if (i == end)
			return this;

		return subList(0, i);
	}

	public SortedSet<T> tailSet(T fromElement) {
		int i = indexOf(fromElement);
		if (i < 0) {
			if (isSubSet())
				throw new IllegalArgumentException("This list is a subset");
			i = start;
		}

		return subList(i, end);
	}

	public T first() {
		if (isEmpty())
			throw new NoSuchElementException("first");
		return get(0);
	}

	public T last() {
		if (isEmpty())
			throw new NoSuchElementException("last");
		return get(end - 1);
	}

	@Deprecated
	public boolean addAll(int index, Collection< ? extends T> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	public T get(int index) {
		return list[index + start];
	}

	@Deprecated
	public T set(int index, T element) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Deprecated
	public void add(int index, T element) {
		throw new UnsupportedOperationException("Immutable");
	}

	@Deprecated
	public T remove(int index) {
		throw new UnsupportedOperationException("Immutable");
	}

	public int lastIndexOf(Object o) {
		int n = indexOf(o);
		if (n < 0)
			return -1;

		while (cmp.compare(list[n], (T) o) == 0)
			n++;

		return n;
	}

	public ListIterator<T> listIterator() {
		return new It(start);
	}

	public ListIterator<T> listIterator(int index) {
		return new It(index + start);
	}

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

		return new SortedList<T>(this, fromIndex, toIndex);
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
			if (comparator.compare(this.list[as], this.list[bs]) != 0)
				return false;
		}
		return true;
	}

	public Class< ? > getType() {
		return type;
	}

	public void setType(Class< ? > type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String del = "";
		for (T s : list) {
			sb.append(del);
			sb.append(s);
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
			if (prev.equals(list[i]))
				return true;
		}
		return false;
	}

	public static <T extends Comparable< ? >> SortedList<T> fromIterator(Iterator<T> it) {
		IteratorList<T> l = new IteratorList<T>(it);
		return new SortedList<T>(l);
	}

	public static <T> SortedList<T> fromIterator(Iterator<T> it, Comparator<T> cmp) {
		IteratorList<T> l = new IteratorList<T>(it);
		return new SortedList<T>(l, cmp);
	}

	public static <T> SortedSet<T> empty() {
		return (SortedSet<T>) empty;
	}
}
