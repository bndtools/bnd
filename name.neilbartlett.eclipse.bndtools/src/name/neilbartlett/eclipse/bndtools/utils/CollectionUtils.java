/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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
	public static <K,V> Map<V, Set<K>> invertMapOfCollection(Map<K, ? extends Collection<V>> mapOfCollection) {
		Map<V, Set<K>> result = new TreeMap<V, Set<K>>();
		
		for (Entry<K, ? extends Collection<V>> inputEntry : mapOfCollection.entrySet()) {
			K inputKey = inputEntry.getKey();
			Collection<V> inputCollection = inputEntry.getValue();
			
			for (V inputValue : inputCollection) {
				Set<K> resultSet = result.get(inputValue);
				if(resultSet == null) {
					resultSet = new TreeSet<K>();
					result.put(inputValue, resultSet);
				}
				resultSet.add(inputKey);
			}
		}
		
		return result;
	}
	public static <T> List<T> asList(Object[] array) {
		@SuppressWarnings("unchecked") List<T> list = (List<T>) Arrays.asList(array);
		return list;
	}
	public static <T> List<T> newArrayList(Object[] array) {
		List<T> result = new ArrayList<T>(array.length);
		for (Object obj : array) {
			@SuppressWarnings("unchecked")
			T item = (T) obj;
			result.add(item);
		}
		return result;
	}
}
