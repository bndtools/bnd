package aQute.lib.comparators;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntSupplier;

/**
 * Some utilities to make comparisons easier to do, especially with null
 * handling
 */
public interface Comparators {

	/**
	 * When you compare, the nulls can be quite annoying to handle. For this
	 * interface, we use a magic value to mark outside the normal 1,0,-1 values
	 * for compare to indicate that both values are not null and should be
	 * compared with traditional means.
	 */
	int COMPARISON_REQUIRED = Integer.MAX_VALUE;

	/**
	 * Protect a comparator from being called with null
	 * <ul>
	 * <li>if they are both null, 0
	 * <li>if a and !b -> 1
	 * <li>if !a and b -> -1
	 * <li>Otherwise result of the nonNulComparator.compare(a,b)
	 * </ul>
	 *
	 * @param <T> the shared type
	 * @param a
	 * @param b
	 * @param nonNullComparator
	 * @return the result
	 */

	static <T> int compare(T a, T b, Comparator<T> nonNullComparator) {

		if (a != null) {
			if (b != null)
				return nonNullComparator.compare(a, b);
			else
				return 1;
		} else if (b != null)
			return -1;
		else
			return 0;
	}

	/**
	 * Shield the nonNullComparator from null objects. This is a convenience
	 * method so you do not have to invent new parameter names but can use the
	 * direct parameters from the invoked method.
	 * <ul>
	 * <li>if they are both null, 0
	 * <li>if a and !b -> 1
	 * <li>if !a and b -> -1
	 * <li>Otherwise nonNullComparator.getAsInt()
	 * </ul>
	 *
	 * @param <T> the shared type
	 * @param a
	 * @param b
	 * @return the result
	 */

	static <T> int compare(T a, T b, IntSupplier nonNullComparator) {
		return compare(a, b, (aa, bb) -> nonNullComparator.getAsInt());
	}

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

	static <T> int comparePresent(T a, T b) {
		return compare(a, b, () -> COMPARISON_REQUIRED);
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

	static <T> int comparePresentEquals(T a, T b) {
		return compare(a, b, (aa, bb) -> aa.equals(bb) ? 0 : COMPARISON_REQUIRED);
	}

	/**
	 * Check if compare value is final, that it decides the comparison and can
	 * be used as return value in a compare operation.
	 */

	static boolean isFinal(int compare) {
		return compare != COMPARISON_REQUIRED;
	}

	/**
	 * Compare two objects.
	 * <ul>
	 * <li>Test for null as in {@link #comparePresent(Object, Object)}
	 * <li>Test for same class, if not class names are compared
	 * <li>Test if they are comparable, use the object comparison
	 * <li>If array, compare the fields of the array with this method in order,
	 * if all match, the longer is higher.
	 * <li>If Collection, compare the members of the Collection with this method
	 * in order, longer is higher
	 * <li>returns 0 in a desperate attempt. This can make objects return 0 that
	 * are not equals.
	 * </ul>
	 * The method compares to a depth of 4
	 *
	 * @param <T> the shared type of the objects
	 * @param a
	 * @param b
	 * @return 0, 1 a>b, -1 a<b
	 */
	static <T> int compare(T a, T b) {
		return compare(a, b, 4);
	}

	/**
	 * Compare two objects.
	 * <ul>
	 * <li>Test for null as in {@link #comparePresent(Object, Object)}
	 * <li>Test for same class, if not class names are compared
	 * <li>Test if they are comparable, use the object comparison
	 * <li>If array, compare the fields of the array with this method in order,
	 * if all match, the longer is higher.
	 * <li>If Collection, compare the members of the Collection with this method
	 * in order, longer is higher
	 * <li>returns 0 in a desperate attempt. This can make objects return 0 that
	 * are not equals.
	 * </ul>
	 * The method compares to a depth of 4
	 *
	 * @param <T> the shared type of the objects
	 * @param a
	 * @param b
	 * @param maxDepth max number of recursions
	 * @return 0, 1 a>b, -1 a<b
	 */
	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	static <T> int compare(T a, T b, int maxDepth) {
		int compare = comparePresent(a, b);
		if (isFinal(compare))
			return compare;

		assert a != null && b != null;

		if (maxDepth <= 0)
			return 0;

		Class<? extends Object> ca = a.getClass();
		Class<? extends Object> cb = b.getClass();

		if (a.getClass()
			.isArray()
			&& b.getClass()
				.isArray()) {

			int na = Array.getLength(a);
			int nb = Array.getLength(b);
			int l = Math.min(na, nb);

			for (int n = 0; n < l; n++) {
				Object va = Array.get(a, n);
				Object vb = Array.get(b, n);
				compare = compare(va, vb, maxDepth - 1);
				if (compare != 0)
					return compare;
			}

			return Integer.compare(na, nb);
		}

		if (a instanceof Collection la && b instanceof Collection lb) {

			int na = la.size();
			int nb = lb.size();
			int l = Math.min(na, nb);

			Iterator ia = la.iterator();
			Iterator ib = lb.iterator();

			for (int i = 0; i < l; i++) {
				assert ia.hasNext() && ib.hasNext();
				Object oa = ia.next();
				Object ob = ib.next();

				compare = compare(oa, ob, maxDepth - 1);
				if (compare != 0)
					return compare;
			}

			return Integer.compare(na, nb);
		}

		if (ca != cb) {
			assert ca != null && cb != null;
			return Integer.signum(ca.getName()
				.compareTo(cb.getName()));
		}

		assert ca == cb;

		if (Comparable.class.isAssignableFrom(ca)) {
			Comparable<T> cmpa = (Comparable<T>) a;
			return Integer.signum(cmpa.compareTo(b));
		}

		return 0;
	}

	/**
	 * Compare sequential fields in a Map with {@link #compare(Object, Object)}.
	 * The given fields are retrieved from the map and then compared. The first
	 * non-0 value defines the result. If no keys are given, all keys in both
	 * maps in sorted order are used instead.
	 *
	 * @param a the a value or null
	 * @param b the b value or null
	 * @param keys the array of keys to use
	 * @return 0, 1 a>b, -1 a<b
	 */
	static int compareMapsByKeys(Map<String, Object> a, Map<String, Object> b, String... keys) {
		int compare = comparePresent(a, b);
		if (isFinal(compare))
			return compare;

		if (keys.length == 0) {
			Set<String> keySet = new TreeSet<>(a.keySet());
			keySet.addAll(b.keySet());
			keys = keySet.toArray(String[]::new);
		}

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
