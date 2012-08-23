
package test;

import static org.mockito.Mockito.*;

import java.util.*;

import junit.framework.*;
import aQute.bnd.differ.*;
import aQute.bnd.service.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;

public class RepoTreeTest extends TestCase {
	public static void testSimple() throws Exception {
		RepositoryPlugin a = mock(RepositoryPlugin.class);
		RepositoryPlugin b = mock(RepositoryPlugin.class);
		
		when(a.getName()).thenReturn("a");
		when(a.list(null)).thenReturn(Arrays.asList("a","b"));
		when(a.versions("a")).thenReturn(new SortedList<Version>(new Version("1"), new Version("2")));
		when(a.versions("b")).thenReturn(new SortedList<Version>(new Version("2"), new Version("3")));

		when(b.getName()).thenReturn("b");
		when(b.list(null)).thenReturn(Arrays.asList("b","c"));
		when(b.versions("b")).thenReturn(new SortedList<Version>(new Version("1"), new Version("2")));
		when(b.versions("c")).thenReturn(new SortedList<Version>(new Version("2"), new Version("3")));
		
		Tree ta = RepositoryElement.getTree(a);
		Tree tb = RepositoryElement.getTree(b);
		
		Diff diff = new DiffImpl(ta, tb);
		print(diff,0);
		
		assertEquals(Delta.MAJOR, diff.getDelta());
		assertEquals(Type.PROGRAM, diff.get("a").getType());
		assertEquals(Type.VERSION, diff.get("a").get("1.0.0").getType());
		assertEquals(Delta.ADDED, diff.get("a").get("1.0.0").getDelta());
		assertEquals(Delta.ADDED, diff.get("a").get("2.0.0").getDelta());
		
		assertEquals(Delta.REMOVED, diff.get("b").get("1.0.0").getDelta());
		assertEquals(Delta.UNCHANGED, diff.get("b").get("2.0.0").getDelta());
		assertEquals(Delta.ADDED, diff.get("b").get("3.0.0").getDelta());
		
		assertEquals(Delta.REMOVED, diff.get("c").getDelta());
		assertEquals(Delta.REMOVED, diff.get("c").get("2.0.0").getDelta());
		assertEquals(Delta.REMOVED, diff.get("c").get("3.0.0").getDelta());
		
	}
	
	static void print(Diff diff , int n) {
		System.out.println("                            ".substring(0,n) + diff.getName() + " " + diff.getType() + "  " + diff.getDelta());
		for ( Diff c : diff.getChildren()) {
			print(c, n+1);
		}
		
	}
}
