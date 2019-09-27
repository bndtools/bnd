package aQute.lib.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import aQute.lib.strings.Strings;

public class ExtList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	@SafeVarargs
	public ExtList(T... ts) {
		super(ts.length);
		for (T t : ts) {
			add(t);
		}
	}

	ExtList() {
		super();
	}

	public ExtList(int size) {
		super(size);
	}

	public ExtList(Collection<? extends T> col) {
		super(col);
	}

	public ExtList(Iterable<? extends T> col) {
		for (T t : col)
			add(t);
	}

	public static ExtList<String> from(String s) {
		return Strings.splitAsStream(s)
			.collect(collector());
	}

	public static ExtList<String> from(String s, String delimeter) {
		return Pattern.compile(delimeter)
			.splitAsStream(s)
			.collect(collector());
	}

	private static Collector<String, ?, ExtList<String>> collector() {
		return Collector.of(ExtList::new, List::add, (left, right) -> {
			left.addAll(right);
			return left;
		});
	}

	public String join() {
		return Strings.join(this);
	}

	public String join(String del) {
		return Strings.join(del, this);
	}

}
