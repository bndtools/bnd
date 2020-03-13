package aQute.libg.ints;

public class IntCounter {
	int count;


	public IntCounter() {
		this(0);
	}

	public IntCounter(int n) {
		this.count = n;
	}

	public int inc() {
		int old = count;
		if (count == Integer.MAX_VALUE)
			throw new IllegalArgumentException("Overflow +");
		count++;
		return old;
	}

	public int dec() {
		int old = count;
		if (count == Integer.MIN_VALUE)
			throw new IllegalArgumentException("Overflow -");
		count--;
		return old;
	}

	public int reset(int n) {
		int old = count;
		count = n;
		return old;
	}

	public int reset() {
		return reset(0);
	}

	public int get() {
		return count;
	}
}
