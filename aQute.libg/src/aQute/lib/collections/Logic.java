package aQute.lib.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Logic {
	private Logic() {}

	@SafeVarargs
	public static <T> Collection<T> retain(Collection<? extends T> first, Collection<? extends T>... sets) {
		Set<T> result = new HashSet<>(first);
		for (Collection<? extends T> set : sets) {
			result.retainAll(set);
		}
		return result;
	}

	@SafeVarargs
	public static <T> Collection<T> remove(Collection<? extends T> first, Collection<? extends T>... sets) {
		Set<T> result = new HashSet<>(first);
		for (Collection<? extends T> set : sets) {
			result.removeAll(set);
		}
		return result;
	}

	@SafeVarargs
	public static <T> boolean hasOverlap(Collection<? extends T> source, Collection<? extends T>... toBeChecked) {
		for (T t : source) {
			for (Collection<? extends T> l : toBeChecked) {
				for (T r : l) {
					if (t.equals(r))
						return true;
				}
			}
		}
		return false;
	}
}
