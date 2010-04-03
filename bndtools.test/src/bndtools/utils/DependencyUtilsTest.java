package bndtools.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.junit.Test;

import bndtools.Plugin;
import bndtools.utils.CircularDependencyException;
import bndtools.utils.DependencyUtils;
import bndtools.utils.DependencyUtils.Processor;
import static org.junit.Assert.*;



public class DependencyUtilsTest {
	
	private static Set<String> toSet(String[] strings) {
		Set<String> result = new HashSet<String>();
		for (String string : strings) {
			result.add(string);
		}
		return result;
	}
	
	private static <T> int indexOf(T x, T[] array) {
		for (int i = 0; i < array.length; i++) {
			if(array[i].equals(x)) {
				return i;
			}
		}
		return -1;
	}
	
	private static <T> void assertBefore(T x, T y, T[] array) {
		int xindex = indexOf(x, array);
		int yindex = indexOf(y, array);
		
		assertTrue("Item not found in array", xindex != -1);
		assertTrue("Item not found in array", yindex != -1);
		
		assertTrue("Items not in correct order", xindex < yindex);
	}
	
	@Test
	public void testSimple() throws CoreException, CircularDependencyException {
		List<String> input = Arrays.asList(new String[] {"A","B","C","D","E"});
		
		Map<String,Set<String>> deps = new HashMap<String, Set<String>>();
		deps.put("A", toSet(new String[] {"B","C","D"}));
		deps.put("B", toSet(new String[] {"C","E"}));
		deps.put("C", toSet(new String[] {"D"}));

		final List<String> result = new LinkedList<String>();
		DependencyUtils.processDependencyMap(input, deps, new Processor<String>() {
			public void process(String obj, IProgressMonitor monitor) throws CoreException {
				result.add(obj);
			}
		}, new NullProgressMonitor());
		
		String[] array = result.toArray(new String[0]);
		// E must precede A or B
		assertBefore("E", "B", array);
		assertBefore("E", "A", array);
		
		// D must precede A, B or C
		assertBefore("D", "A", array);
		assertBefore("D", "B", array);
		assertBefore("D", "C", array);
		
		// C must precede A and B
		assertBefore("C", "A", array);
		assertBefore("C", "B", array);
		
		// B must precede A
		assertBefore("B", "A", array);
	}
	
	@Test
	public void testCircular() throws CoreException {
		List<String> input = Arrays.asList(new String[] {"A","B","C","D","E"});
		
		Map<String,Set<String>> deps = new HashMap<String, Set<String>>();
		deps.put("A", toSet(new String[] {"B","C"}));
		deps.put("B", toSet(new String[] {"C","E"}));
		deps.put("C", toSet(new String[] {"D"}));
		// Next line creates the cycle
		deps.put("D", toSet(new String[] {"A"}));

		try {
			DependencyUtils.processDependencyMap(input, deps, new Processor<String>() {
				public void process(String obj, IProgressMonitor monitor) throws CoreException {
				}
			}, new NullProgressMonitor());
			fail("Should have thrown CircularDependencyException");
		} catch (CircularDependencyException e) {
			// Expected
		}

	}
}
