package aQute.lib.collections;

import java.util.*;

public class ExtList<T> extends ArrayList<T> {
	private static final long	serialVersionUID	= 1L;

	public ExtList(T ... ts) {
		super(ts.length);
		for (T t : ts){
			add(t);
		}
	}
	
	public String join() {
		return join(",");
	}

	public String join(String del) {
		StringBuilder sb = new StringBuilder();
		String d= "";
		for ( T t : this) {
			sb.append(d);
			d=del;
			if ( t != null)
				sb.append(t.toString());
		}
		return sb.toString();
	}

}
