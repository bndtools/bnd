package aQute.lib.collections;

import java.util.*;

public class Parameters extends LinkedHashMap<String,Map<String,String>> {
	private static final long	serialVersionUID	= 1L;

	@Override public Map<String,String> get(Object key) {
		Map<String,String> map = super.get(key);
		if ( map == null) {
			map = new LinkedHashMap<String, String>();
			super.put((String)key, map);
		}
		return map;
	}
}
