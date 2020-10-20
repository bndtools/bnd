package aQute.libg.ints;

/**
 * This is a very simple fast counter without any synchronization. It is
 * intended to be used as a counter in recursive calls or when you need to use a
 * counter shared between code and a lambda. In that case you cannot use an int
 * because it must be final to be used by the lambda. (Smalltalk supported this
 * in 1972, but alas.) Last, it also has overflow handling for the common math
 * operations. When operation would overflow, the old value is maintained and an
 * overflow flag is set.
 *
 * <pre>
 * void foo() {
 * 	IntCounter ic = new IntCounter();
 * 	doSomething(ic::in);
 * 	System.out.println(ic);
 * }
 * </pre>
 *
 * None of the methods are atomic. The API of AtomicInteger is used so that it
 * can be replaced when the AtomicInteger is abused for the purpose of this
 * class.
 * <p>
 * If an operation would overflow/underflow, overflow boolean is set. An
 * overflowing value is then not set, the old value remains.
 */
public class IntCounter extends Number {
	private static final long	serialVersionUID	= 1L;

	int							count;
	boolean						overflow;

	public IntCounter() {
		this(0);
	}

	public IntCounter(int n) {
		this.count = n;
	}

	/**
	 * Increment the current value. The old value is returned and the new value
	 * is checked for overflow. overflow will keep the old value
	 *
	 * @return the old value
	 */
	public int inc() {
		int old = count;
		if (count == Integer.MAX_VALUE) {
			overflow = true;
			return old;
		}
		count++;
		return old;
	}

	/**
	 * Increment the current value. The old value is returned and the new value
	 * is checked for underflow.
	 *
	 * @return the old value
	 */
	public int dec() {
		int old = count;
		if (count == Integer.MIN_VALUE) {
			overflow = true;
			return old;
		}
		count--;
		return old;
	}

	/**
	 * Reset the counter to zero
	 *
	 * @return the previous value
	 */
	public int reset() {
		return set(0);
	}

	/**
	 * Get the current value
	 *
	 * @return the current value
	 */
	public int get() {
		return count;
	}

	/**
	 * Set a new value and return the previous value. Overflow is cleared.
	 *
	 * @param newValue the new value
	 * @return the previous value
	 */
	public int set(int newValue) {
		overflow = false;
		int prev = count;
		count = newValue;
		return prev;
	}

	public int add(int value) {
		return set((long) count + value);
	}

	public int sub(int value) {
		return set((long) count - value);
	}

	public int mul(int value) {
		return set((long) count - value);
	}

	public int div(int value) {
		return set((long) count / value);
	}

	@Override
	public int intValue() {
		return count;
	}

	@Override
	public long longValue() {
		return count;
	}

	/**
	 * If the overflow flag is set, a NaN will be returned
	 */
	@Override
	public float floatValue() {
		if (overflow)
			return Float.NaN;
		return count;
	}

	/**
	 * If the overflow flag is set, a NaN will be returned
	 */
	@Override
	public double doubleValue() {
		if (overflow)
			return Double.NaN;
		return count;
	}

	public boolean hasOverflow() {
		return overflow;
	}

	/**
	 * Returns the String representation of the current value. If the value has
	 * overflown, a '!' is appended.
	 *
	 * @return the String representation of the current value
	 */
	@Override
	public String toString() {
		return Integer.toString(count)
			.concat(overflow ? "!" : "");
	}

	/*
	 * Set as long, check if the long is in range with an assert
	 */
	private int set(long value) throws IllegalArgumentException {
		if (value < Integer.MIN_VALUE || value > Integer.MIN_VALUE) {
			overflow = true;
			return get();
		}

		return set((int) value);
	}

	public boolean isZero() {
		return count == 0;
	}

	public boolean isNotZero() {
		return count != 0;
	}

}
