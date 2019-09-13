package aQute.lib.stringrover;

import static java.util.Objects.requireNonNull;

public class StringRover implements CharSequence {
	private final String	string;
	private int				offset;

	public StringRover(String string) {
		this.string = requireNonNull(string);
		offset = 0;
	}

	private StringRover(String string, int offset) {
		this.string = string;
		this.offset = offset;
	}

	@Override
	public int length() {
		return string.length() - offset;
	}

	public boolean isEmpty() {
		return string.length() <= offset;
	}

	@Override
	public char charAt(int index) {
		return string.charAt(offset + index);
	}

	public StringRover increment() {
		return increment(1);
	}

	public StringRover increment(int increment) {
		int new_offset = offset + increment;
		if (new_offset <= 0) {
			offset = 0;
		} else {
			int len = string.length();
			offset = (new_offset >= len) ? len : new_offset;
		}
		return this;
	}

	public StringRover reset() {
		offset = 0;
		return this;
	}

	public StringRover duplicate() {
		return new StringRover(string, offset);
	}

	public int indexOf(int ch, int from) {
		int index = string.indexOf(ch, offset + from) - offset;
		return (index < 0) ? -1 : index;
	}

	public int indexOf(int ch) {
		return indexOf(ch, 0);
	}

	public int indexOf(CharSequence str, int from) {
		final int length = length();
		final int size = str.length();
		if (from >= length) {
			return (size == 0) ? length : -1;
		}
		if (from < 0) {
			from = 0;
		}
		if (size == 0) {
			return from;
		}
		final char first = str.charAt(0);
		outer: for (int limit = offset + (length - size), i = offset + from; i <= limit; i++) {
			if (string.charAt(i) == first) {
				final int end = i + size;
				for (int j = i + 1, k = 1; j < end; j++, k++) {
					if (string.charAt(j) != str.charAt(k)) {
						continue outer;
					}
				}
				return i - offset;
			}
		}
		return -1;
	}

	public int indexOf(CharSequence str) {
		return indexOf(str, 0);
	}

	public int lastIndexOf(int ch, int from) {
		int index = string.lastIndexOf(ch, offset + from) - offset;
		return (index < 0) ? -1 : index;
	}

	public int lastIndexOf(int ch) {
		return lastIndexOf(ch, length() - 1);
	}

	public int lastIndexOf(CharSequence str, int from) {
		final int length = length();
		final int size = str.length();
		if (from < 0) {
			return -1;
		}
		final int right = length - size;
		if (from > right) {
			from = right;
		}
		if (size == 0) {
			return from;
		}
		final int end = size - 1;
		final char last = str.charAt(end);
		outer: for (int limit = offset + end, i = limit + from; i >= limit; i--) {
			if (string.charAt(i) == last) {
				final int start = i - end;
				for (int j = start, k = 0; j < i; j++, k++) {
					if (string.charAt(j) != str.charAt(k)) {
						continue outer;
					}
				}
				return start - offset;
			}
		}
		return -1;
	}

	public int lastIndexOf(CharSequence str) {
		return lastIndexOf(str, length());
	}

	public String substring(int start) {
		return string.substring(offset + start);
	}

	public String substring(int start, int end) {
		return string.substring(offset + start, offset + end);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return substring(start, end);
	}

	@Override
	public String toString() {
		return string.substring(offset);
	}

	public boolean startsWith(CharSequence prefix, int from) {
		int size = prefix.length();
		if ((from < 0) || (from > (length() - size))) {
			return false;
		}
		for (int source = offset + from, target = 0; size > 0; size--, source++, target++) {
			if (string.charAt(source) != prefix.charAt(target)) {
				return false;
			}
		}
		return true;
	}

	public boolean startsWith(CharSequence prefix) {
		return startsWith(prefix, 0);
	}

	public boolean endsWith(CharSequence suffix) {
		return startsWith(suffix, length() - suffix.length());
	}
}
