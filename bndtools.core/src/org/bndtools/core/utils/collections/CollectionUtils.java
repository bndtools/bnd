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
package org.bndtools.core.utils.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import aQute.lib.collections.MultiMap;

public class CollectionUtils {
    /**
     * Move the selected items down one position in the list.
     *
     * @param list
     *            The list of items, which will be altered in-place.
     * @param selectionIndexes
     *            The indexes of the items to be moved.
     * @return Whether any items have been moved. For example, would return
     *         false if the selected items were already at the bottom of the list.
     */
    public static <T> boolean moveDown(List<T> list, int[] selectionIndexes) {
        boolean moved = false;
        int resultPos = 0;

        int size = list.size();
        for (int i = 0; i < size; i++) {
            T item = list.get(i);
            if (arrayContains(selectionIndexes, i)) {
                // Find next unselected item
                int nextUnselected = -1;
                int j = i + 1;
                while (j < size) {
                    if (!arrayContains(selectionIndexes, j)) {
                        nextUnselected = j;
                        break;
                    }
                    j++;
                }
                // Swap with it
                if (nextUnselected != -1) {
                    list.set(i, list.get(nextUnselected));
                    list.set(nextUnselected, item);
                    moved = true;
                }
                resultPos++;
            }
        }
        return moved;
    }

    /**
     * Move the selected items up one position in the list.
     *
     * @param list
     *            The list of items, which will be altered in-place.
     * @param selectionIndexes
     *            The indexes of the items to be moved.
     * @return Whether any items have been moved. For example, would return
     *         false if the selected items were already at the top of the list.
     */
    public static <T> boolean moveUp(List<T> list, int[] selectionIndexes) {
        boolean moved = false;
        int resultPos = 0;

        int size = list.size();
        for (int i = size - 1; i >= 0; i--) {
            T item = list.get(i);
            if (arrayContains(selectionIndexes, i)) {
                // Find next unselected item
                int nextUnselected = -1;
                int j = i - 1;
                while (j >= 0) {
                    if (!arrayContains(selectionIndexes, j)) {
                        nextUnselected = j;
                        break;
                    }
                    j--;
                }
                // Swap with it
                if (nextUnselected != -1) {
                    list.set(i, list.get(nextUnselected));
                    list.set(nextUnselected, item);
                    moved = true;
                }
                resultPos++;
            }
        }
        return moved;
    }
	private static boolean arrayContains(int[] array, int item) {
		for (int i : array) {
			if(i == item)
				return true;
		}
		return false;
	}
	
    public static <K, V> MultiMap<V, K> invertMultiMap(MultiMap<K, V> input) {
        MultiMap<V, K> result = new MultiMap<V, K>();
        
        for (Entry<K, List<V>> inputEntry : input.entrySet()) {
            K inputKey = inputEntry.getKey();
            List<V> inputList = inputEntry.getValue();
            for (V inputVal : inputList) {
                result.add(inputVal, inputKey);
            }
        }
        
        return result;
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

    public static <T> List<T> asList(Enumeration<T> enumeration) {
        List<T> result = new ArrayList<T>();
        while (enumeration.hasMoreElements())
            result.add(enumeration.nextElement());
        return result;
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
