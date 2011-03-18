package aQute.lib.collections;

import java.util.*;

public class MultiMap<K,V> extends HashMap<K,Set<V>> {
	private static final long	serialVersionUID	= 1L;
	final Set<V> EMPTY = Collections.emptySet();
	
	public boolean add( K key, V value ) {
		Set<V> set = get(key);
		if ( set == null) {
			set=new HashSet<V>();
			put(key,set);
		}
		return set.add(value);
	}
	
	public boolean addAll( K key, Collection<V> value ) {
		Set<V> set = get(key);
		if ( set == null) {
			set=new HashSet<V>();
			put(key,set);
		}
		return set.addAll(value);
	}
	
	public boolean remove( K key, V value ) {
		Set<V> set = get(key);
		if ( set == null) {
			return false;
		}
		boolean result = set.remove(value);
		if ( set.isEmpty())
			remove(key);
		return result;
	}
	
	public boolean removeAll( K key, Collection<V> value ) {
		Set<V> set = get(key);
		if ( set == null) {
			return false;
		}
		boolean result = set.removeAll(value);
		if ( set.isEmpty())
			remove(key);
		return result;
	}
	
	public Iterator<V> iterate(K key) {
		Set<V> set = get(key);
		if ( set == null)
			return EMPTY.iterator();
		else
			return set.iterator();
	}
	
	public Iterator<V> all() {
		return new Iterator<V>() {
			Iterator<Set<V>> master = values().iterator();
			Iterator<V> current = null;
			
			public boolean hasNext() {
				if ( current == null || !current.hasNext()) {
					if ( master.hasNext()) {
						current = master.next().iterator();
						return current.hasNext();
					}
					return false;
				}
				return true;
			}

			public V next() {
				return current.next();
			}

			public void remove() {
				current.remove();
			}
			
		};
	}
}
