package aQute.lib.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterators;

import junit.framework.TestCase;

public class CollectionsTest extends TestCase {

	List<String>	a	= Arrays.asList("a");
	List<String>	ab	= Arrays.asList("a", "b");
	List<String>	abc	= Arrays.asList("a", "b", "c");
	List<String>	bc	= Arrays.asList("b", "c");
	List<String>	cd	= Arrays.asList("c", "d");
	List<String>	b	= Arrays.asList("b");

	@SuppressWarnings("unchecked")
	public void testLogicRetain() {
		assertEqualsList(Arrays.asList("a"), Logic.retain(a));
		assertEqualsList(Arrays.asList("a"), Logic.retain(a, a, a, a));
		assertEqualsList(Arrays.asList("a"), Logic.retain(a, abc));
		assertEqualsList(Arrays.asList("b", "c"), Logic.retain(bc, abc));
		assertEqualsList(Arrays.asList("b"), Logic.retain(bc, abc, ab));

	}

	@SuppressWarnings("unchecked")
	public void testHasOverlap() {
		assertFalse(Logic.hasOverlap(a));
		assertTrue(Logic.hasOverlap(a, ab));
		assertFalse(Logic.hasOverlap(a, b));
		assertFalse(Logic.hasOverlap(a, cd, bc));
		assertTrue(Logic.hasOverlap(a, cd, bc, abc));
	}

	private void assertEqualsList(List<String> a, Collection<String> b) {
		assertTrue(compare(a, b));
	}

	private <T> boolean compare(Collection<T> a, Collection<T> b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;

		if (a.equals(b))
			return true;

		List<T> aa = new ArrayList<>(a);
		List<T> bb = new ArrayList<>(b);
		return aa.equals(bb);
	}

	public void testSortedList() throws Exception {
		SortedList<String> sl = new SortedList<>("f", "a", "b", "c", "d", "c", "e");
		assertEquals("[a, b, c, c, d, e, f]", sl.toString());

		SortedSet<String> tail = sl.tailSet("c");
		assertEquals("[c, c, d, e, f]", tail.toString());
		assertEquals("c", tail.first());
		assertEquals("f", tail.last());

		SortedSet<String> head = sl.headSet("c");
		assertEquals("[a, b]", head.toString());

		SortedSet<String> head2 = sl.headSet("c ");
		assertEquals("[a, b, c, c]", head2.toString());
		assertEquals("[c, c]", sl.headSet("c ")
			.tailSet("b ")
			.toString());

		Iterable<String> it = sl.headSet("c ")
			.tailSet("b ");
		ExtList<String> l = new ExtList<>(it);
		assertEquals("[c, c]", l.toString());

		ExtList<String> ll = new ExtList<>(sl.tailSet("c"));
		assertEquals("[c, c, d, e, f]", ll.toString());

		assertEquals(-1, sl.indexOf("g"));
		assertEquals(-1, sl.indexOf("cc"));
		assertEquals(2, sl.indexOf("c"));
		assertEquals(3, sl.lastIndexOf("c"));

		assertTrue(sl.hasDuplicates());

	}

	public void testFailures() {
		SortedList<String> sl = new SortedList<>("f", "a", "b", "c", "d", "c", "e");
		try {
			sl.get(1000);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {

		}

		try {
			sl.get(-1);
			fail();
		} catch (ArrayIndexOutOfBoundsException e) {

		}
	}

	public void testComparator() {
		Comparator<String> reverseOrder = Collections.reverseOrder();
		SortedList<String> sl = new SortedList<>(Arrays.asList("f", "a", "b", "c", "d", "c", "e"), reverseOrder);
		assertEquals("[f, e, d, c, c, b, a]", sl.toString());
	}

	public void testListIterator() throws Exception {
		Comparator<String> reverseOrder = Collections.reverseOrder();
		List<String> sl = new SortedList<>(Arrays.asList("f", "a", "b", "c", "d", "c", "e"), reverseOrder);
		// sl = new ArrayList<String>(sl);
		ListIterator<String> li = sl.listIterator(1);
		assertEquals(1, li.nextIndex());
		assertEquals(0, li.previousIndex());
		assertTrue(li.hasPrevious());
		assertTrue(li.hasNext());
		assertEquals("f", li.previous());
		assertFalse(li.hasPrevious());
		assertTrue(li.hasNext());
		assertEquals("f", li.next());
		assertTrue(li.hasNext());
		assertEquals("e", li.next());
		assertTrue(li.hasNext());
		assertEquals("d", li.next());
		assertTrue(li.hasNext());
		assertEquals("c", li.next());
		assertTrue(li.hasNext());
		assertEquals("c", li.next());
		assertTrue(li.hasNext());
		assertEquals("b", li.next());
		assertTrue(li.hasNext());
		assertEquals("a", li.next());
		assertFalse(li.hasNext());
	}

	public void testEnumerationSpliterator() {
		List<String> test = Arrays.asList("once", "upon", "a", "time");
		Enumeration<String> e = Enumerations.enumeration(test.spliterator(), s -> s.toUpperCase(Locale.ROOT),
			s -> !s.startsWith("A"));

		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertEquals("ONCE", e.nextElement());

		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertEquals("UPON", e.nextElement());

		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertTrue(e.hasMoreElements());
		assertEquals("TIME", e.nextElement());

		assertFalse(e.hasMoreElements());
		assertFalse(e.hasMoreElements());
		assertFalse(e.hasMoreElements());
		try {
			e.nextElement();
		} catch (NoSuchElementException nsee) {
			// expected
		}
		assertFalse(e.hasMoreElements());
	}

	public void testEnumerationEmptySpliterator() {
		Enumeration<String> e = Enumerations.enumeration(Spliterators.emptySpliterator());

		assertFalse(e.hasMoreElements());
		assertFalse(e.hasMoreElements());
		assertFalse(e.hasMoreElements());
		try {
			e.nextElement();
		} catch (NoSuchElementException nsee) {
			// expected
		}
		assertFalse(e.hasMoreElements());
	}
}
