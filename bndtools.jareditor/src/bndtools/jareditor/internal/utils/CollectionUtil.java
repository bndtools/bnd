package bndtools.jareditor.internal.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CollectionUtil {
    public static <K, V> Map<V,Set<K>> invertMapOfCollection(Map<K, ? extends Collection<V>> mapOfCollection) {
        Map<V,Set<K>> result = new TreeMap<V,Set<K>>();

        for (Entry<K, ? extends Collection<V>> inputEntry : mapOfCollection.entrySet()) {
            K inputKey = inputEntry.getKey();
            Collection<V> inputCollection = inputEntry.getValue();

            for (V inputValue : inputCollection) {
                Set<K> resultSet = result.get(inputValue);
                if (resultSet == null) {
                    resultSet = new TreeSet<K>();
                    result.put(inputValue, resultSet);
                }
                resultSet.add(inputKey);
            }
        }

        return result;
    }

}
