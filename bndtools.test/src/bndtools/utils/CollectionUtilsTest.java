package bndtools.utils;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

import bndtools.utils.CollectionUtils;

public class CollectionUtilsTest {
	
	@Test
	public void testMoveDown() {
		String[] array = new String[] { "A","B","C","D","E","F" };
		int[] selections = new int[] { 1, 2, 4 }; // B,C,E
	
		String[] expected = new String[] { "A","D","B","C","F","E" };
		int[] expectedNewSelections = new int[] { 2, 3, 5 };
		
		int[] newSelections = CollectionUtils.moveDown(Arrays.asList(array), selections);
		assertArrayEquals(expected, array);
		assertArrayEquals(expectedNewSelections, newSelections);
	}

	@Test
	public void testMoveDownHitsEnd() {
		String[] array = new String[] { "A","B","C","D","E","F" };
		int[] selections = new int[] { 2, 3, 5 }; // C,D,F
		
		String[] expected = new String[] { "A","B","E","C","D","F" };
		int[] expectedNewSelections = new int[] { 3, 4, 5 };
		
		int[] newSelections = CollectionUtils.moveDown(Arrays.asList(array), selections);
		assertArrayEquals(expected, array);
		assertArrayEquals(expectedNewSelections, newSelections);
	}

	@Test
	public void testMoveUp() {
		String[] array = new String[] { "A","B","C","D","E","F" };
		int[] selections = new int[] { 1, 2, 4 }; // B,C,E
	
		String[] expected = new String[] { "B","C","A","E","D","F" };
		int[] expectedNewSelections = new int[] { 3, 1, 0 };
		
		int[] newSelections = CollectionUtils.moveUp(Arrays.asList(array), selections);
		assertArrayEquals(expected, array);
		assertArrayEquals(expectedNewSelections, newSelections);
	}

	@Test
	public void testMoveUpHitsEnd() {
		String[] array = new String[] { "A","B","C","D","E","F" };
		int[] selections = new int[] { 0, 1, 3 }; // A,B,D
		
		String[] expected = new String[] { "A","B","D","C","E","F" };
		int[] expectedNewSelections = new int[] { 2, 1, 0 };
		
		int[] newSelections = CollectionUtils.moveUp(Arrays.asList(array), selections);
		assertArrayEquals(expected, array);
		assertArrayEquals(expectedNewSelections, newSelections);
	}

}
