package org.bndtools.utils.collections;

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
	 * @param list The list of items, which will be altered in-place.
	 * @param selectionIndexes The indexes of the items to be moved.
	 * @return Whether any items have been moved. For example, would return
	 *         false if the selected items were already at the bottom of the
	 *         list.
	 */
	public static <T> boolean moveDown(List<T> list, int[] selectionIndexes) {
		boolean moved = false;

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
			}
		}
		return moved;
	}

	/**
	 * Move the selected items up one position in the list.
	 *
	 * @param list The list of items, which will be altered in-place.
	 * @param selectionIndexes The indexes of the items to be moved.
	 * @return Whether any items have been moved. For example, would return
	 *         false if the selected items were already at the top of the list.
	 */
	public static <T> boolean moveUp(List<T> list, int[] selectionIndexes) {
		boolean moved = false;

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
			}
		}
		return moved;
	}

	private static boolean arrayContains(int[] array, int item) {
		for (int i : array) {
			if (i == item)
				return true;
		}
		return false;
	}

	public static <K, V> MultiMap<V, K> invertMultiMap(MultiMap<K, V> input) {
		MultiMap<V, K> result = new MultiMap<>();

		for (Entry<K, List<V>> inputEntry : input.entrySet()) {
			K inputKey = inputEntry.getKey();
			List<V> inputList = inputEntry.getValue();
			for (V inputVal : inputList) {
				result.add(inputVal, inputKey);
			}
		}

		return result;
	}

	public static <K, V> Map<V, Set<K>> invertMapOfCollection(Map<K, ? extends Collection<V>> mapOfCollection) {
		Map<V, Set<K>> result = new TreeMap<>();

		for (Entry<K, ? extends Collection<V>> inputEntry : mapOfCollection.entrySet()) {
			K inputKey = inputEntry.getKey();
			Collection<V> inputCollection = inputEntry.getValue();

			for (V inputValue : inputCollection) {
				Set<K> resultSet = result.get(inputValue);
				if (resultSet == null) {
					resultSet = new TreeSet<>();
					result.put(inputValue, resultSet);
				}
				resultSet.add(inputKey);
			}
		}

		return result;
	}

	public static <T> List<T> asList(Object[] array) {
		@SuppressWarnings("unchecked")
		List<T> list = (List<T>) Arrays.asList(array);
		return list;
	}

	public static <T> List<T> asList(Enumeration<T> enumeration) {
		List<T> result = new ArrayList<>();
		while (enumeration.hasMoreElements())
			result.add(enumeration.nextElement());
		return result;
	}

	public static <T> List<T> newArrayList(Object[] array) {
		List<T> result = new ArrayList<>(array.length);
		for (Object obj : array) {
			@SuppressWarnings("unchecked")
			T item = (T) obj;
			result.add(item);
		}
		return result;
	}

	public static <T> List<T> flatten(Collection<? extends Collection<T>> listList) {
		int size = 0;
		for (Collection<T> list : listList)
			size += list.size();
		List<T> result = new ArrayList<>(size);
		for (Collection<T> list : listList)
			result.addAll(list);
		return result;
	}

	public static <T> T[] flatten(List<T[]> arrayList) {
		int size = 0;
		for (T[] array : arrayList)
			size += array.length;
		@SuppressWarnings("unchecked")
		T[] result = (T[]) new Object[size];
		int copied = 0;
		for (T[] array : arrayList) {
			System.arraycopy(array, 0, result, copied, array.length);
			copied += array.length;
		}
		return result;
	}

	public static <T> List<T> append(Collection<T> first, Collection<T> second) {
		List<T> result = new ArrayList<>(first.size() + second.size());
		result.addAll(first);
		result.addAll(second);
		return result;
	}

}
