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

}
