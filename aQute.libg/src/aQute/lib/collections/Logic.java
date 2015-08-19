package aQute.lib.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Logic {

	// @SafeVarargs
	public static <T> Collection<T> retain(Collection<T> first, Collection<T>... sets) {
		Set<T> result = new HashSet<T>(first);
		for (Collection<T> set : sets) {
			result.retainAll(set);
		}
		return result;
	}

	// @SafeVarargs
	public static <T> Collection<T> remove(Collection<T> first, Collection<T>... sets) {
		Set<T> result = new HashSet<T>(first);
		for (Collection<T> set : sets) {
			result.removeAll(set);
		}
		return result;
	}

	public static <T> boolean hasOverlap(Collection<T> source, Collection<T>... toBeChecked) {
		for (T t : source) {
			for (Collection<T> l : toBeChecked) {
				for (T r : l) {
					if (t.equals(r))
						return true;
				}
			}
		}
		return false;
	}
}
