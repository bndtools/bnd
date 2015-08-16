package org.bndtools.utils.collections;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

public class IdentityHashSet<E> implements Set<E> {

    private static final String SENTINEL = "";

    private final IdentityHashMap<E,String> map = new IdentityHashMap<E,String>();

    public IdentityHashSet() {}

    public IdentityHashSet(Collection< ? extends E> collection) {
        for (E e : collection) {
            map.put(e, SENTINEL);
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, SENTINEL) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection< ? > c) {
        for (Object o : c) {
            if (!map.containsKey(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection< ? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            if (map.put(e, SENTINEL) != null)
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection< ? > c) {
        boolean changed = false;
        IdentityHashSet<Object> inverse = new IdentityHashSet<Object>(c);
        for (Iterator<E> iter = map.keySet().iterator(); iter.hasNext();) {
            E entry = iter.next();
            if (!inverse.contains(entry)) {
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection< ? > c) {
        boolean changed = false;
        for (Object o : c) {
            if (map.remove(o) != null)
                changed = true;
        }
        return changed;
    }

    @Override
    public void clear() {
        map.clear();
    }

}
