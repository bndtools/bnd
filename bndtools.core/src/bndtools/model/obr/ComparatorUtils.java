package bndtools.model.obr;

import java.util.Arrays;
import java.util.Comparator;

public class ComparatorUtils {
    
    public static <T extends Comparable<T>> int safeCompare(T o1, T o2) {
        if (o1 == null) {
            if (o2 == null)
                return 0;
            return -1;
        }
        if (o2 == null)
            return 1;
        
        return o1.compareTo(o2);
    }

    public static <T> int safeCompare(T o1, T o2, Comparator<? super T> comparator) {
        if (o1 == null) {
            if (o2 == null)
                return 0;
            return -1;
        }
        if (o2 == null)
            return 1;
        
        return comparator.compare(o1, o2);
    }
    

    public static <T> int arrayCompare(T[] a1, T[] a2, Comparator<? super T> comparator) {
        if (a1 == null) {
            if (a2 == null)
                return 0;
            return -1;
        }
        if (a2 == null)
            return 1;
        
        Arrays.sort(a1, comparator);
        Arrays.sort(a2, comparator);
        
        int index = 0;
        while (index < a1.length || index < a2.length) {
            if (index >= a1.length) {
                // a2 array is longer
                return -1;
            } else if (index >= a2.length) {
                // a1 array is longer
                return 1;
            } else {
                int diff = comparator.compare(a1[index], a2[index]);
                if (diff != 0)
                    return diff;
            }
            index++;
        }
        return 0;
    }
}
