package aQute.libg.generics;

import java.util.*;

public class Create {
    
    public static <K,V>  Map<K, V> map() {
        return new LinkedHashMap<K,V>();
    }

    public static <K,V>  Map<K, V> map(Class<K> key, Class<V> value) {
        return Collections.checkedMap(new LinkedHashMap<K,V>(), key,value);
    }

    public static <T>  List<T> list() {
        return new ArrayList<T>();
    }

    public static <T>  List<T> list(Class<T> c) {
        return Collections.checkedList(new ArrayList<T>(),c) ;
    }

    public static <T>  Set<T> set() {
        return new HashSet<T>();
    }

    public static <T>  Set<T> set(Class<T> c) {
        return Collections.checkedSet(new HashSet<T>(),c);
    }

    public static <T>  List<T> list(T[] source) {
        return new ArrayList<T>(Arrays.asList(source));
    }

    public static <T>  Set<T> set(T[]source) {
        return new HashSet<T>(Arrays.asList(source));
    }

    public static <K,V>  Map<K, V> copy(Map<K,V> source) {
        return new LinkedHashMap<K,V>(source);
    }

    public static <T>  List<T> copy(List<T> source) {
        return new ArrayList<T>(source);
    }

    public static <T>  Set<T> copy(Collection<T> source) {
        if ( source == null )
            return set();
        return new HashSet<T>(source);
    }

    
}
