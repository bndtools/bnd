package test;

import java.util.*;

import junit.framework.*;
import junit.runner.*;
import aQute.lib.collections.*;

public class CollectionsTest extends TestCase {
	
	public void testSortedList() throws Exception {
		SortedList<String> sl = new SortedList<String>("f", "a", "b", "c", "d", "c", "e");
		assertEquals("[a, b, c, c, d, e, f]", sl.toString());
		
		SortedSet<String> tail = sl.tailSet("c");
		assertEquals("[c, c, d, e, f]", tail.toString());
		assertEquals( "c", tail.first() );
		assertEquals( "f", tail.last() );
		
		SortedSet<String> head = sl.headSet("c");
		assertEquals("[a, b]", head.toString());
		
		SortedSet<String> head2 = sl.headSet("c ");
		assertEquals("[a, b, c, c]", head2.toString());
		assertEquals("[c, c]", sl.headSet("c ").tailSet("b ").toString());
		
		Iterable<String> it = sl.headSet("c ").tailSet("b ");
		ExtList<String> l = new ExtList<String>(it);
		assertEquals("[c, c]", l.toString());

		ExtList<String> ll = new ExtList<String>(sl.tailSet("c"));
		assertEquals("[c, c, d, e, f]", ll.toString());

		assertEquals(-1, sl.indexOf("g"));
		assertEquals(-1, sl.indexOf("cc"));
		assertEquals(2, sl.indexOf("c"));
		assertEquals(3, sl.lastIndexOf("c"));
		
		assertTrue( sl.hasDuplicates());
		
	}
	
	public void testFailures() {
		SortedList<String> sl = new SortedList<String>("f", "a", "b", "c", "d", "c", "e");
		try {
			sl.get(1000);
			fail();
		} catch( ArrayIndexOutOfBoundsException e) {
			
		}
		
		try {
			sl.get(-1);
			fail();
		} catch( ArrayIndexOutOfBoundsException e) {
			
		}
	}
	
	public void testComparator() {
		Comparator<String> reverseOrder = Collections.reverseOrder();
		SortedList<String> sl = new SortedList<String>(Arrays.asList("f", "a", "b", "c", "d", "c", "e"), reverseOrder);
		assertEquals("[f, e, d, c, c, b, a]", sl.toString());
	}
	

	public void testListIterator() throws Exception {
		Comparator<String> reverseOrder = Collections.reverseOrder();
		SortedList<String> sl = new SortedList<String>(Arrays.asList("f", "a", "b", "c", "d", "c", "e"), reverseOrder);
		
		ListIterator<String> li = sl.listIterator(1);
		assertEquals( 2, li.nextIndex());
		assertEquals( 0, li.previousIndex());
		assertTrue(li.hasPrevious());
		assertTrue(li.hasNext());
		assertEquals("f", li.previous());
		assertFalse(li.hasPrevious());
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
	
	
}
