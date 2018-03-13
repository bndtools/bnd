package aQute.bnd.deployer.repository;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class MapToDictionaryAdapter extends Dictionary<String, Object> {

	private final Map<String, Object> map;

	public MapToDictionaryAdapter(Map<String, Object> map) {
		this.map = map;
	}

	@Override
	public Enumeration<Object> elements() {
		final Iterator<Object> iter = map.values()
			.iterator();
		return new Enumeration<Object>() {
			@Override
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			@Override
			public Object nextElement() {
				return iter.next();
			}
		};
	}

	@Override
	public Object get(Object key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		final Iterator<String> iter = map.keySet()
			.iterator();
		return new Enumeration<String>() {
			@Override
			public boolean hasMoreElements() {
				return iter.hasNext();
			}

			@Override
			public String nextElement() {
				return iter.next();
			}
		};
	}

	@Override
	public Object put(String key, Object value) {
		return map.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

}
