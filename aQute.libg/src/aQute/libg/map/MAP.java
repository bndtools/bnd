package aQute.libg.map;

import java.util.*;

public class MAP {

	static public class MAPX<K, V> extends LinkedHashMap<K, V> {
		private static final long	serialVersionUID	= 1L;
		public MAPX<K, V> $(K key, V value) {
			put(key, value);
			return this;
		}
	}

	public static <Kx, Vx> MAPX<Kx, Vx> $(Kx key, Vx value) {
		MAPX<Kx, Vx> map = new MAPX<Kx, Vx>();
		map.put(key, value);
		return map;
	}
}