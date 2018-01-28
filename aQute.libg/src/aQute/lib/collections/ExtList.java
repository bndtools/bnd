package aQute.lib.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExtList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	@SafeVarargs
	public ExtList(T... ts) {
		super(ts.length);
		for (T t : ts) {
			add(t);
		}
	}

	public ExtList(int size) {
		super(size);
	}

	public ExtList(Collection< ? extends T> col) {
		super(col);
	}

	public ExtList(Iterable< ? extends T> col) {
		for (T t : col)
			add(t);
	}

	public static ExtList<String> from(String s) {
		// TODO make sure no \ before comma
		return from(s, "\\s*,\\s*");
	}

	public static ExtList<String> from(String s, String delimeter) {
		ExtList<String> result = new ExtList<>();
		String[] parts = s.split(delimeter);
		Collections.addAll(result, parts);
		return result;
	}

	public String join() {
		return join(",");
	}

	public String join(String del) {
		StringBuilder sb = new StringBuilder();
		String d = "";
		for (T t : this) {
			sb.append(d);
			d = del;
			if (t != null)
				sb.append(t);
		}
		return sb.toString();
	}

}
