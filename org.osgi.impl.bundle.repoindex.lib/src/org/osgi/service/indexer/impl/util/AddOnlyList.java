package org.osgi.service.indexer.impl.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>
 * A List implementation in which destructive removal or replacement of elements
 * is forbidden. Any attempt to either <b>remove</b> elements (e.g. through
 * {@link #remove(int)}, {@link #clear()}, {@link Iterator#remove()}) or
 * <b>replace</b> elements (e.g. through {@link #set(int, Object)} or
 * {@link ListIterator#set(Object)}) will throw an
 * {@link UnsupportedOperationException}.
 * </p>
 * 
 * <p>
 * Note that this is a wrapper class only. It must be initialised with an actual
 * {@link List} implementation for its underlying data structure.
 * </p>
 * 
 * @author Neil Bartlett
 * 
 * @param <T>
 */
public class AddOnlyList<T> implements List<T> {

	static final String ERROR_REMOVE = "Removal of items is not permitted.";
	static final String ERROR_REPLACE = "Replacement of items is not permitted.";

	private final List<T> delegate;

	/**
	 * Create a new add-only list based on the specified underlying list.
	 * 
	 * @param list
	 *            The list providing the underlying data structure.
	 */
	public AddOnlyList(List<T> list) {
		this.delegate = list;
	}

	// FORBIDDEN METHODS: remove, removeAll, retailAll, clear, set

	public boolean remove(Object o) {
		throw new UnsupportedOperationException(ERROR_REMOVE);
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(ERROR_REMOVE);
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException(ERROR_REMOVE);
	}

	public void clear() {
		throw new UnsupportedOperationException(ERROR_REMOVE);
	}

	public T set(int index, T element) {
		throw new UnsupportedOperationException(ERROR_REPLACE);
	}

	public T remove(int index) {
		throw new UnsupportedOperationException(ERROR_REMOVE);
	}

	// WRAPPING METHODS: create restricted iterators and sublists

	public Iterator<T> iterator() {
		return new AddOnlyIterator<T>(delegate.iterator());
	}

	public ListIterator<T> listIterator() {
		return new AddOnlyListIterator<T>(delegate.listIterator());
	}

	public ListIterator<T> listIterator(int index) {
		return new AddOnlyListIterator<T>(delegate.listIterator(index));
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return new AddOnlyList<T>(delegate.subList(fromIndex, toIndex));
	}

	// STRAIGHT-THROUGH DELEGATED METHODS

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <T1> T1[] toArray(T1[] a) {
		return delegate.toArray(a);
	}

	public boolean add(T e) {
		return delegate.add(e);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		return delegate.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		return delegate.addAll(index, c);
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public T get(int index) {
		return delegate.get(index);
	}

	public void add(int index, T element) {
		delegate.add(index, element);
	}

	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

}

class AddOnlyIterator<T> implements Iterator<T> {

	private final Iterator<T> iter;

	AddOnlyIterator(Iterator<T> iter) {
		this.iter = iter;
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	public T next() {
		return iter.next();
	}

	public void remove() {
		throw new UnsupportedOperationException(AddOnlyList.ERROR_REMOVE);
	}
}

class AddOnlyListIterator<T> implements ListIterator<T> {

	private final ListIterator<T> iter;

	AddOnlyListIterator(ListIterator<T> iter) {
		this.iter = iter;
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	public T next() {
		return iter.next();
	}

	public boolean hasPrevious() {
		return iter.hasPrevious();
	}

	public T previous() {
		return iter.previous();
	}

	public int nextIndex() {
		return iter.nextIndex();
	}

	public int previousIndex() {
		return iter.previousIndex();
	}

	public void remove() {
		throw new UnsupportedOperationException(AddOnlyList.ERROR_REMOVE);
	}

	public void set(T e) {
		throw new UnsupportedOperationException(AddOnlyList.ERROR_REPLACE);
	}

	public void add(T e) {
		iter.add(e);
	}
}
