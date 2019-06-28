package aQute.lib.collections;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple facade for enumerators so they can be used in for loops.
 *
 * @param <T>
 */
public class EnumerationIterator<T> implements Iterable<T>, Iterator<T> {

	public static <T> EnumerationIterator<T> iterator(Enumeration<? extends T> e) {
		return new EnumerationIterator<>(e);
	}

	private final Enumeration<? extends T>	enumerator;
	private final AtomicBoolean				done	= new AtomicBoolean();

	public EnumerationIterator(Enumeration<? extends T> e) {
		enumerator = e;
	}

	@Override
	public Iterator<T> iterator() {
		if (!done.compareAndSet(false, true))
			throw new IllegalStateException("Can only be used once");
		return this;

	}

	@Override
	public boolean hasNext() {
		return enumerator.hasMoreElements();
	}

	@Override
	public T next() {
		return enumerator.nextElement();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Does not support removes");
	}
}
