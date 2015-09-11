package aQute.lib.collections;

import java.util.ArrayList;
import java.util.Collection;

public class ExtList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	// @SafeVarargs
	public ExtList(T... ts) {
		super(ts.length);
		for (T t : ts) {
			add(t);
		}
	}

	public ExtList(int size) {
		super(size);
	}

	public ExtList(Collection<T> col) {
		super(col);
	}

	public ExtList(Iterable<T> col) {
		for (T t : col)
			add(t);
	}

	public static ExtList<String> from(String s) {
		// TODO make sure no \ before comma
		return from(s, "\\s*,\\s*");
	}

	public static ExtList<String> from(String s, String delimeter) {
		ExtList<String> result = new ExtList<String>();
		String[] parts = s.split(delimeter);
		for (String p : parts)
			result.add(p);
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
				sb.append(t.toString());
		}
		return sb.toString();
	}

}
