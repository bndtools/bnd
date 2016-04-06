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
	static Comparator<Object>	comparator	=															//

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
		int n;

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
			assert n > start;
			return list[--n];
		}

		public int nextIndex() {
			return (n - start);
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
		this(x.clone(), 0, x.length, (Comparator<T>) comparator);
	}

	@SuppressWarnings("cast")
	public SortedList(Comparator<T> cmp, T... x) {
		this(x.clone(), 0, x.length, cmp);
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
		assert type == null || type.isInstance(o);
		return find((T) o) >= 0;
	}

	public Iterator<T> iterator() {
		return new It(start);
	}

	public Object[] toArray() {
		if (list == null) {
			return new Object[0];
		}

		if (start == 0 && end == list.length)
			return list.clone();

		if (type != null)
			return (Object[]) Array.newInstance(type, size());

		return toArray(new Object[0]);
	}

	@SuppressWarnings("hiding")
	public <X> X[] toArray(X[] a) {
		int size = size();

		if (a.length < size)
			a = (X[]) Array.newInstance(a.getClass().getComponentType(), size);

		System.arraycopy(list, start, a, 0, size);
		if (a.length > size)
			a[size] = null;
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

	public int indexOf(Object o) {
		assert type == null || type.isInstance(o);

		int n = find((T) o);
		if (n < end && comparator.compare(o, list[n]) == 0)
			return n;

		return -1;
	}

	public int lastIndexOf(Object o) {
		assert type == null || type.isInstance(o);

		int n = find((T) o);
		if (n >= end || comparator.compare(o, list[n]) != 0)
			return -1;

		while (n < end - 1 && comparator.compare(o, list[n + 1]) == 0)
			n++;

		return n;
	}

	/**
	 * Find the first element that is equal or bigger than the given element
	 * 
	 * @param toElement
	 * @return absolute index (not relative!), returns end if not found
	 */
	private int find(T toElement) {
		int i = start;
		while (i < end) {
			if (comparator.compare(toElement, list[i]) <= 0)
				break;
			else
				i++;
		}

		return i;
	}

	public SortedSet<T> tailSet(T fromElement) {
		int i = find(fromElement);
		return subList(i - start, end - start);
	}

	public SortedList<T> headSet(T toElement) {
		int i = find(toElement);
		return subList(0, i - start);
	}

	public T first() {
		if (isEmpty())
			throw new NoSuchElementException("first");
		return get(0);
	}

	public T last() {
		if (isEmpty())
			throw new NoSuchElementException("last");
		return get(size() - 1);
	}

	@Deprecated
	public boolean addAll(int index, Collection< ? extends T> c) {
		throw new UnsupportedOperationException("Immutable");
	}

	public T get(int index) {
		index += start;
		if (index >= end)
			throw new ArrayIndexOutOfBoundsException();

		return list[index];
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

		if (toIndex == fromIndex)
			return (SortedList<T>) empty();

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
			if (comparator.compare(prev, list[i]) == 0)
				return true;

			prev = list[i];
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
