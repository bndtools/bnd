package name.neilbartlett.eclipse.bndtools.utils;

import java.util.List;

public class CollectionUtils {
	public static <T> int[] moveDown(List<T> list, int[] selectionIndexes) {
		int[] result = new int[selectionIndexes.length];
		int resultPos = 0;
		
		int size = list.size();
		for(int i=0; i<size; i++) {
			T item = list.get(i);
			if(arrayContains(selectionIndexes, i)) {
				// Find next unselected item
				int nextUnselected = -1;
				int j = i + 1;
				while(j < size) {
					if(!arrayContains(selectionIndexes, j)) {
						nextUnselected = j;
						break;
					}
					j++;
				}
				
				// Swap with it
				if(nextUnselected != -1) {
					list.set(i, list.get(nextUnselected));
					list.set(nextUnselected, item);
					result[resultPos] = i + 1;
				} else {
					result[resultPos] = i;
				}
				resultPos++;
			}
		}
		return result;
	}
	public static <T> int[] moveUp(List<T> list, int[] selectionIndexes) {
		int[] result = new int[selectionIndexes.length];
		int resultPos = 0;
		
		int size = list.size();
		for(int i = size - 1; i >= 0; i--) {
			T item = list.get(i);
			if(arrayContains(selectionIndexes, i)) {
				// Find next unselected item
				int nextUnselected = -1;
				int j = i - 1;
				while(j >= 0) {
					if(!arrayContains(selectionIndexes, j)) {
						nextUnselected = j;
						break;
					}
					j--;
				}
				
				// Swap with it
				if(nextUnselected != -1) {
					list.set(i, list.get(nextUnselected));
					list.set(nextUnselected, item);
					result[resultPos] = i - 1;
				} else {
					result[resultPos] = i;
				}
				resultPos++;
			}
		}
		return result;
	}
	private static boolean arrayContains(int[] array, int item) {
		for (int i : array) {
			if(i == item)
				return true;
		}
		return false;
	}
}
