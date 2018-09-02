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
		offset++;
		return this;
	}

	public StringRover increment(int increment) {
		offset += increment;
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
		return string.indexOf(ch, offset + from) - offset;
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
}
