package aQute.lib.comparators;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Some utilities to make comparisons easier to do, especially with null
 * handling
 */
public interface Comparators {

	/**
	 * Compare two objects for null.
	 * <ul>
	 * <li>if they are both null, 0
	 * <li>if a and !b -> 1
	 * <li>if !a and b -> -1
	 * <li>Otherwise Integer.MAXVALUE
	 * </ul>
	 * <p>
	 * To use this, compare the result with <=1 and return if so.
	 *
	 * @param <T> the shared type
	 * @param a
	 * @param b
	 * @return the result
	 */

	static <T> int compareNull(T a, T b) {

		if (a != null) {
			if (b != null)
				return Integer.MAX_VALUE;
			else
				return 1;
		} else if (b != null)
			return -1;
		else
			return 0;
	}

	/**
	 * Compare two objects for null and equality.
	 * <ul>
	 * <li>if they are both null, 0
	 * <li>if a and !b -> 1
	 * <li>if !a and b -> -1
	 * <li>if a.equals(b) -> 0
	 * <li>Otherwise Integer.MAXVALUE
	 * </ul>
	 * <p>
	 * To use this, compare the result with <=1 and return if so.
	 *
	 * @param <T> the shared type
	 * @param a
	 * @param b
	 * @return the result
	 */

	static <T> int compareNullEquals(T a, T b) {
		int n = compareNull(a, b);
		if (isDecided(n))
			return n;
		else
			return a.equals(b) ? 0 : Integer.MAX_VALUE;
	}

	/**
	 * Check if the result of compareNull is decided, i.e. -1 >= compare <= 1
	 */

	static boolean isDecided(int compare) {
		return compare != Integer.MAX_VALUE;
	}

	/**
	 * Compare two objects.
	 * <ul>
	 * <li>Test for null as in {@link #compareNull(Object, Object)}
	 * <li>Test for same class, if not class names are compared
	 * <li>Test if they are comparable, use the object comparison
	 * <li>If array, compare the fields of the array with this method in order,
	 * if all match, the longer is higher
	 * <li>If Iterable, compare the members of the Iterable with this method in
	 * order, longer is higher
	 * <li>returns 0 in a desperate attempt. This can make objects return 0 that
	 * are not equals.
	 *
	 * @param <T> the shared type of the objects
	 * @param a
	 * @param b
	 * @return 0, 1 a>b, -1 a<b
	 */
	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	static <T> int compare(T a, T b) {
		int compare = compareNull(a, b);
		if (isDecided(compare))
			return compare;

		assert a != null && b != null;

		Class<? extends Object> ca = a.getClass();
		Class<? extends Object> cb = b.getClass();
		if (ca != cb) {
			assert ca != null && cb != null;
			return ca.getName()
				.compareTo(cb.getName());
		}

		assert ca == cb;

		if (Comparable.class.isAssignableFrom(ca)) {
			Comparable<T> cmpa = (Comparable<T>) a;
			return cmpa.compareTo(b);
		}

		if (ca.isArray()) {
			int na = Array.getLength(a);
			int nb = Array.getLength(b);
			int l = Math.min(na, nb);
			for (int n = 0; n < l; n++) {
				Object va = Array.get(a, n);
				Object vb = Array.get(b, n);
				compare = compare(va, vb);
				if (compare != 0)
					return compare;
			}

			return Integer.compare(na, nb);
		}

		if (a instanceof Collection) {
			Collection la = (Collection) a;
			Collection lb = (Collection) b;

			int na = la.size();
			int nb = lb.size();
			int l = Math.min(na, nb);

			Iterator ia = la.iterator();
			Iterator ib = lb.iterator();

			for (int i = 0; i < l; i++) {
				assert ia.hasNext() && ib.hasNext();
				Object oa = ia.next();
				Object ob = ib.next();
				compare = compare(oa, ob);
				if (compare != 0)
					return compare;
			}

			return Integer.compare(na, nb);
		}

		return 0;
	}

	/**
	 * Compare sequential fields in a Map with {@link #compare(Object, Object)}.
	 * The given fields are retrieved from the map and then compared. The first
	 * non-0 value defines the result.
	 *
	 * @param a the a value or null
	 * @param b the b value or null
	 * @param keys the array of keys to use
	 * @return 0, 1 a>b, -1 a<b
	 */
	static int compare(Map<String, Object> a, Map<String, Object> b, String... keys) {
		int compare = compareNull(a, b);
		if (isDecided(compare))
			return compare;

		for (String key : keys) {
			Object va = a.get(key);
			Object vb = b.get(key);
			compare = compare(va, vb);
			if (compare != 0)
				return compare;
		}
		return 0;
	}

}
