package aQute.libg.map;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Easy way to build a map: Map<String,Integer> s = MAP.$("a",2).$("b",3);
 */
public class MAP {

	static public class MAPX<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;

		public MAPX<K, V> $(K key, V value) {
			put(key, value);
			return this;
		}

		public MAPX<K, V> $(Map<K, V> all) {
			putAll(all);
			return this;
		}

		public Hashtable<K, V> asHashtable() {
			return new Hashtable<>(this);
		}
	}

	public static <Kx, Vx> MAPX<Kx, Vx> $(Kx key, Vx value) {
		MAPX<Kx, Vx> map = new MAPX<>();
		map.put(key, value);
		return map;
	}

	public <K, V> Map<K, V> dictionary(Dictionary<K, V> dict) {
		Map<K, V> map = new LinkedHashMap<>();
		for (Enumeration<K> e = dict.keys(); e.hasMoreElements();) {
			K k = e.nextElement();
			V v = dict.get(k);
			map.put(k, v);
		}
		return map;
	}
}
