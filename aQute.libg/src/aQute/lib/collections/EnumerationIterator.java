package aQute.lib.collections;

import java.util.*;

/**
 * Simple facade for enumerators so they can be used in for loops.
 * 
 * @param <T>
 */
public class EnumerationIterator<T> implements Iterable<T>, Iterator<T> {

	public static <T> EnumerationIterator<T> iterator(Enumeration<T> e) {
		return new EnumerationIterator<T>(e);
	}

	final Enumeration<T>	enumerator;
	volatile boolean		done	= false;

	public EnumerationIterator(Enumeration<T> e) {
		enumerator = e;
	}

	public synchronized Iterator<T> iterator() {
		if (done)
			throw new IllegalStateException("Can only be used once");
		done = true;
		return this;

	}

	public boolean hasNext() {
		return enumerator.hasMoreElements();
	}

	public T next() {
		return enumerator.nextElement();
	}

	public void remove() {
		throw new UnsupportedOperationException("Does not support removes");
	}
}
